package com.andy.rest.resources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.andy.rest.beans.Input;
import com.andy.rest.beans.Response;
import com.andy.rest.beans.TreeTraversalListener;
import com.andy.rest.exception.DependencyException;
import com.andy.rest.exception.EntityException;
import com.andy.rest.util.Utils;
import com.andy.security.api.Role;
import com.andy.security.api.Secured;
import com.andy.security.api.User;

import io.swagger.annotations.Api;

@Api("MySql DML Operations")
@Path("mdml")
@Produces("application/json")
public class MysqlDML {

    private static final Logger LOGGER = Logger.getLogger(MysqlDML.class);

    static {
	try {
	    Class.forName(Utils.BUNDLE.getProperty("mysql.db.driver"));
	} catch (ClassNotFoundException e) {
	    LOGGER.error("Error initializing MySql DB Driver");
	    System.out.println("");
	}
    }

    @Context
    @NotNull
    private ResourceContext resourceContext;

    @Context
    private Request request;

    @Context
    private Application application;

    private Connection connection;

    private Utils utils = new Utils();

    @PostConstruct
    public void init() {
	LOGGER.debug("Initialized " + this.getClass().getName());
    }

    @PreDestroy
    public void destroy() throws SQLException {
	LOGGER.debug("Destroying " + this.getClass().getName());
	if (connection != null && !connection.isClosed())
	    connection.close();
    }

    private Connection getConnection() throws SQLException {
	return getConnection(Utils.BUNDLE.getProperty("mysql.db.dbName"));
    }

    private Connection getConnection(String dbName) throws SQLException {
	// if (connection == null)
	connection = DriverManager.getConnection(Utils.BUNDLE.getProperty("mysql.db.url") + "/" + dbName,
		Utils.BUNDLE.getProperty("mysql.db.user"), Utils.BUNDLE.getProperty("mysql.db.password"));
	return connection;
    }

    private List<String> getColumns(ResultSetMetaData md) throws SQLException {
	List<String> columns = new ArrayList<String>();
	for (int i = 0; i < md.getColumnCount(); i++) {
	    columns.add(md.getColumnLabel(i + 1));
	}
	return columns;
    }

    @POST
    @Path("query")
    @Secured({ Role.User })
    public Response fetch(@FormParam("dbName") String dbName, @FormParam("query") String query) {

	Response response = new Response();
	long t1 = System.currentTimeMillis();
	try {

	    Connection con = getConnection(dbName);
	    Statement st = con.createStatement();

	    ResultSet rs = st.executeQuery(query);

	    ResultSetMetaData md = rs.getMetaData();

	    LOGGER.debug("Column Count: " + md.getColumnCount());
	    LOGGER.debug("Columns: " + getColumns(md));
	    List<Object> list = new ArrayList<Object>();

	    while (rs.next()) {

		Map<String, Object> re = new HashMap<String, Object>(md.getColumnCount());
		for (int i = 0; i < md.getColumnCount(); i++) {
		    if (md.getColumnType(i + 1) == Types.LONGVARBINARY) {
			LOGGER.debug("Reading BLOB");
			re.put(md.getColumnLabel(i + 1), utils.fetchBlob(rs.getBinaryStream(i + 1)));
		    } else {
			Object obj = rs.getObject(i + 1);
			re.put(md.getColumnLabel(i + 1), obj);
		    }
		}
		list.add(re);
	    }
	    response.setObjects(list);

	} catch (Exception e) {
	    response.setStatus(false);
	    response.setMessage("Error: " + e.getMessage());
	} finally {
	    long t2 = System.currentTimeMillis();
	    response.setTime(t2 - t1);
	}
	return response;
    }

    @POST
    @Path("delete")
    @Secured({ Role.Administrator })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@QueryParam("dbName") String dbName, Input<Object> input) {

	Response response = new Response();
	long t1 = System.currentTimeMillis();

	Connection con = null;

	try {

	    con = getConnection(dbName);

	    final Connection connection = con;

	    con.setAutoCommit(false);

	    LOGGER.debug("Transactional: " + input.isTransaction());

	    Set<String> dependencySet = new LinkedHashSet<String>();

	    // Foreign key's for each entity and its foreign column mapped to
	    // target entity
	    // e.g. RoomBooking-idRoom -> Room
	    final Map<String, String> foreignKeyMap = new LinkedHashMap<String, String>();

	    // List of entities to be inserted in exact order
	    final List<String> list = new ArrayList<String>();

	    final Map<String, Integer> columnDataTypes = new LinkedHashMap<String, Integer>();

	    final TreeTraversalListener tr = new TreeTraversalListener() {

		@Override
		public void childernTraversed(String entity, int numberOfChildren) {
		    list.add(entity);
		}

		@Override
		public void childFound(String entity, String foreignEntity, String foreignKeyName) {
		    foreignKeyMap.put(entity + "-" + foreignKeyName, foreignEntity);
		    try {
			utils.addColumnTypes(connection, entity, columnDataTypes);
			LOGGER.debug(entity + "-> ColumnTypes: " + columnDataTypes);
		    } catch (SQLException e) {
			throw new RuntimeException(e);
		    }
		}
	    };

	    for (String entity : input.getEntityGroups().keySet()) {

		if (dependencySet.contains(entity)) {
		    continue;
		}

		dependencySet.add(entity);

		utils.addColumnTypes(connection, entity, columnDataTypes);
		LOGGER.debug(entity + "-> ColumnTypes: " + columnDataTypes);

		LOGGER.debug("Tree for " + entity);

		try {
		    utils.traverse(con, entity, dependencySet, input.getEntityGroups().keySet(), tr);
		} catch (SQLException e) {
		    throw new DependencyException("Error creating dependency list for " + entity + ", Info: " + e.getMessage(), e);
		}
	    }

	    Map<String, Boolean> resultMap = new LinkedHashMap<String, Boolean>();

	    // Delete the entity in reverse order
	    for (int i = list.size() - 1; i >= 0; i--) {
		String entity = list.get(i);
		LOGGER.debug("Entity: " + entity.toUpperCase());
		List<Map<String, Object>> objects = input.getEntityGroups().get(entity);

		int objectIndex = 0;
		for (Map<String, Object> object : objects) {
		    LOGGER.debug("Object: " + object);
		    try {
			int affectedRows = runDelete(entity, objectIndex, object, con, foreignKeyMap);
			resultMap.put(entity + '-' + objectIndex, affectedRows == 1);
		    } catch (SQLException e) {
			throw new EntityException(
				"Failed: Delete " + entity + " at index " + objectIndex + ", Info: " + e.getMessage(), e);
		    }
		    objectIndex++;
		}
	    }
	    con.commit();

	    response.setObjects(resultMap);

	    LOGGER.debug("Transaction Completed Successfully");

	} catch (Exception e) {
	    if (con != null) {
		try {
		    con.rollback();
		} catch (SQLException e1) {
		    e1.printStackTrace();
		}
	    }
	    response.setObjects(null);
	    response.setStatus(false);
	    response.setMessage("Error: " + e.getMessage());
	} finally {
	    long t2 = System.currentTimeMillis();
	    response.setTime(t2 - t1);
	}
	return response;
    }

    protected int runDelete(String entity, int objectIndex, Map<String, Object> object, Connection con,
	    Map<String, String> foreignKeyMap) throws SQLException, ParseException {
	PreparedStatement statement = null;
	try {
	    String sql = utils.createDeleteQuery(entity);
	    statement = con.prepareStatement(sql);
	    statement.setObject(1, object.get("id"));
	    return statement.executeUpdate();
	} finally {
	    if (statement != null) {
		statement.close();
	    }
	}
    }

    @POST
    @Path("insertOrUpdate")
    @Secured({ Role.Administrator })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insertOrUpdate(@QueryParam("dbName") String dbName, Input<Object> input) {

	Response response = new Response();
	long t1 = System.currentTimeMillis();

	Connection con = null;

	try {

	    con = getConnection(dbName);

	    final Connection connection = con;

	    con.setAutoCommit(false);

	    LOGGER.debug("Transactional: " + input.isTransaction());

	    Set<String> dependencySet = new LinkedHashSet<String>();

	    // Foreign key's for each entity and its foreign column mapped to
	    // target entity
	    // e.g. RoomBooking-idRoom -> Room
	    final Map<String, String> foreignKeyMap = new LinkedHashMap<String, String>();

	    // List of entities to be inserted in exact order
	    final List<String> list = new ArrayList<String>();

	    final Map<String, Integer> columnDataTypes = new LinkedHashMap<String, Integer>();

	    final TreeTraversalListener tr = new TreeTraversalListener() {

		@Override
		public void childernTraversed(String entity, int numberOfChildren) {
		    list.add(entity);
		}

		@Override
		public void childFound(String entity, String foreignEntity, String foreignKeyName) {
		    foreignKeyMap.put(entity + "-" + foreignKeyName, foreignEntity);
		    try {
			utils.addColumnTypes(connection, entity, columnDataTypes);
			LOGGER.debug(entity + "-> ColumnTypes: " + columnDataTypes);
		    } catch (SQLException e) {
			throw new RuntimeException(e);
		    }
		}
	    };

	    for (String entity : input.getEntityGroups().keySet()) {

		if (dependencySet.contains(entity)) {
		    continue;
		}

		dependencySet.add(entity);

		utils.addColumnTypes(connection, entity, columnDataTypes);
		LOGGER.debug(entity + "-> ColumnTypes: " + columnDataTypes);

		LOGGER.debug("Tree for " + entity);

		try {
		    utils.traverse(con, entity, dependencySet, input.getEntityGroups().keySet(), tr);
		} catch (SQLException e) {
		    throw new DependencyException("Error creating dependency list for " + entity + ", Info: " + e.getMessage(), e);
		}
	    }

	    Map<String, Integer> generatedIds = new LinkedHashMap<String, Integer>();

	    for (String entity : list) {
		LOGGER.debug("Entity: " + entity.toUpperCase());

		List<Map<String, Object>> objects = input.getEntityGroups().get(entity);

		int objectIndex = 0;
		for (Map<String, Object> object : objects) {
		    LOGGER.debug("Object: " + object);
		    try {
			runStatement(entity, objectIndex, object, con, foreignKeyMap, generatedIds, columnDataTypes);
		    } catch (SQLException e) {
			throw new EntityException(
				"Failed: Insert/Update " + entity + " at index " + objectIndex + ", Info: " + e.getMessage(), e);
		    }
		    objectIndex++;
		}
	    }
	    con.commit();

	    response.setObjects(generatedIds);

	    LOGGER.debug("Transaction Completed Successfully");

	} catch (Exception e) {
	    if (con != null) {
		try {
		    con.rollback();
		} catch (SQLException e1) {
		    e1.printStackTrace();
		}
	    }
	    response.setObjects(null);
	    response.setStatus(false);
	    response.setMessage("Error: " + e.getMessage());
	} finally {
	    long t2 = System.currentTimeMillis();
	    response.setTime(t2 - t1);
	}
	return response;
    }

    private void runStatement(String entity, int objectIndex, Map<String, Object> object, Connection con,
	    Map<String, String> foreignKeyMap, Map<String, Integer> generatedIds, Map<String, Integer> columnDataTypes)
	    throws SQLException, ParseException {

	PreparedStatement statement = null;
	ResultSet keys = null;
	try {

	    Set<String> columns = object.keySet();

	    if (columns.contains("id")) {
		// If id is given for an object then it will be considered an
		// Update Statement
		LOGGER.debug("Update -> " + entity + "[" + objectIndex + "]");
		statement = runUpdate(entity, object, con, foreignKeyMap, generatedIds, columns, columnDataTypes);
	    } else {
		LOGGER.debug("Insert -> " + entity + "[" + objectIndex + "]");
		statement = runInsert(entity, object, con, foreignKeyMap, generatedIds, columns, columnDataTypes);
		keys = statement.getGeneratedKeys();
		int id = utils.fetchGeneratedId(keys);
		if (id != -1) {
		    generatedIds.put(entity + "-" + objectIndex, id);
		}
	    }
	} finally {
	    if (keys != null) {
		keys.close();
	    }
	    if (statement != null) {
		statement.close();
	    }
	}
    }

    private PreparedStatement runInsert(String entity, Map<String, Object> object, Connection con,
	    Map<String, String> foreignKeyMap, Map<String, Integer> generatedIds, Set<String> columns,
	    Map<String, Integer> columnDataTypes) throws SQLException, ParseException {

	PreparedStatement statement;
	String sql = utils.createInsertQuery(entity, object, columns);
	statement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	utils.updateParameters(entity, object, foreignKeyMap, generatedIds, statement, columns, columnDataTypes);
	statement.executeUpdate();
	return statement;
    }

    private PreparedStatement runUpdate(String entity, Map<String, Object> object, Connection con,
	    Map<String, String> foreignKeyMap, Map<String, Integer> generatedIds, Set<String> columns,
	    Map<String, Integer> columnDataTypes) throws SQLException, ParseException {
	PreparedStatement statement;
	String sql = utils.createUpdateQuery(entity, object, columns);
	statement = con.prepareStatement(sql);
	int parametersUpdated = utils.updateParameters(entity, object, foreignKeyMap, generatedIds, statement, columns,
		columnDataTypes);
	statement.setObject(parametersUpdated, object.get("id"));
	statement.executeUpdate();
	return statement;
    }

    @POST
    @Path("login")
    public javax.ws.rs.core.Response login(@FormParam("user") String user, @FormParam("password") String password,
	    @FormParam("dbName") String dbName, @Context HttpServletRequest webRequest) {

	PreparedStatement ps = null;
	ResultSet rs = null;
	try {
	    long t1 = System.currentTimeMillis();

	    LOGGER.debug("Login invoked()");

	    HttpSession session = webRequest.getSession(true);

	    Map<String, Role> resourcesRoleMap = (Map<String, Role>) session.getAttribute("resourceRoleMap");
	    if (resourcesRoleMap != null && resourcesRoleMap.containsKey(dbName)) {
		LOGGER.debug("User already logged in");
		return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE).entity("Already logged in")
			.build();
	    }

	    Connection con = getConnection(dbName);

	    ps = con.prepareStatement("Select u.firstName, u.lastName, r.name from User u, Role r where "
		    + " u.idRole = r.id and u.uid = ? and u.password = ?");
	    ps.setString(1, user);
	    ps.setString(2, password);

	    rs = ps.executeQuery();

	    if (!rs.next()) {
		LOGGER.debug("Login denied");
		return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED)
			.entity("Invalid credentials or Invalid DB").build();
	    }

	    User u = new User();
	    u.setFirstName(rs.getString(1));
	    u.setLastName(rs.getString(2));
	    u.setPassword(password);

	    session.invalidate();
	    session = webRequest.getSession(true);

	    session.setAttribute(dbName + "-user", user);

	    if (resourcesRoleMap == null) {
		resourcesRoleMap = new HashMap<String, Role>();
	    }

	    resourcesRoleMap.put(dbName, Role.fromName(rs.getString(3)));

	    session.setAttribute("resourceRoleMap", resourcesRoleMap);

	    LOGGER.debug("Mapping set in session: " + resourcesRoleMap);

	    long t2 = System.currentTimeMillis();

	    LOGGER.debug("Login completed in " + (t2 - t1) + " ms");

	    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK)
		    .entity("Authentication Successful for DB " + dbName).build();

	} catch (Exception e) {
	    LOGGER.error("Error login: " + e.getLocalizedMessage(), e);
	    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).entity("Invalid credentials")
		    .build();
	} finally {
	    if (rs != null) {
		try {
		    rs.close();
		} catch (SQLException e) {
		    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	    }
	    if (ps != null) {
		try {
		    ps.close();
		} catch (SQLException e) {
		    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	    }
	}
    }

    @POST
    @Path("logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object logout(@Context HttpServletRequest webRequest) {

	try {
	    long t1 = System.currentTimeMillis();

	    LOGGER.debug("Logout invoked()");

	    HttpSession session = webRequest.getSession(true);

	    if (session.getAttribute("resourceRoleMap") == null) {
		LOGGER.debug("Invalid operation");
		return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE).entity("Failed Log out")
			.build();
	    }
	    session.invalidate();
	    long t2 = System.currentTimeMillis();

	    LOGGER.debug("Logout completed in " + (t2 - t1) + " ms");

	    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity("Successfully Logged out").build();

	} catch (Exception e) {
	    LOGGER.error("Error logging out: " + e.getLocalizedMessage(), e);
	    return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE).entity("Failed Log out")
		    .build();
	}
    }
}
