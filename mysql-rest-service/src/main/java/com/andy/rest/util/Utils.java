package com.andy.rest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.andy.rest.application.MyApplication;
import com.andy.rest.beans.ForeignKeyColumnDetail;
import com.andy.rest.beans.TreeTraversalListener;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static Properties BUNDLE = new Properties();

    static {
	try {
	    BUNDLE.load(MyApplication.class.getResourceAsStream("/config.properties"));
	} catch (Exception e) {
	    LOGGER.error("Error initializing configuration");
	}
    }

    public void traverse(Connection connection, String entity, final Set<String> dependencySet, Set<String> entitySet,
	    TreeTraversalListener listener) throws SQLException {

	LOGGER.debug("Creating Tree for " + entity);

	List<ForeignKeyColumnDetail> foreignKeyColumns = this.fetchForeignKeyColumns(connection, entity);

	for (ForeignKeyColumnDetail fkcd : foreignKeyColumns) {

	    if (!entitySet.contains(fkcd.getForeignKeyTable())) {
		// Do not visit the entity for which we do not have any data to
		// be inserted
		continue;
	    }

	    listener.childFound(entity, fkcd.getForeignKeyTable(), fkcd.getName());

	    if (dependencySet.contains(fkcd.getForeignKeyTable())) {
		// child foreignKey table has already been visited so skip
		continue;
	    }

	    dependencySet.add(fkcd.getForeignKeyTable());

	    traverse(connection, fkcd.getForeignKeyTable(), dependencySet, entitySet, listener);
	}

	listener.childernTraversed(entity, foreignKeyColumns.size());
    }

    public List<ForeignKeyColumnDetail> fetchForeignKeyColumns(Connection con, String entity) throws SQLException {

	List<ForeignKeyColumnDetail> foreignKeyColumns = new ArrayList<ForeignKeyColumnDetail>();
	DatabaseMetaData meta = con.getMetaData();
	ResultSet rs = meta.getImportedKeys(con.getCatalog(), null, entity);

	while (rs.next()) {

	    ForeignKeyColumnDetail cd = new ForeignKeyColumnDetail();
	    cd.setName(rs.getString("FKCOLUMN_NAME"));
	    cd.setForeignKeyTable(rs.getString("PKTABLE_NAME"));
	    cd.setForeignKeyColumn(rs.getString("PKCOLUMN_NAME"));
	    foreignKeyColumns.add(cd);
	    LOGGER.debug(cd);
	}

	rs.close();

	return foreignKeyColumns;
    }

    public void addColumnTypes(Connection con, String entity, Map<String, Integer> columnTypes) throws SQLException {

	DatabaseMetaData meta = con.getMetaData();
	ResultSet rss = meta.getColumns(con.getCatalog(), null, entity, "%");

	while (rss.next()) {
	    columnTypes.put(entity + '-' + rss.getString("COLUMN_NAME"), rss.getInt("DATA_TYPE"));
	}

	rss.close();
    }

    public int updateParameters(String entity, Map<String, Object> object, Map<String, String> foreignKeyMap,
	    Map<String, Integer> generatedIds, PreparedStatement statement, Set<String> columns,
	    Map<String, Integer> columnDataTypes) throws SQLException, ParseException {
	int i = 1;
	for (String column : columns) {
	    Object value = object.get(column);
	    String referenceEntity = foreignKeyMap.get(entity + "-" + column);
	    if (referenceEntity != null) {
		Integer refId = (Integer) value;
		if (refId < 0) {
		    // The value is a foreign key but negative value means
		    // its referring to an entity in current transaction
		    // hence we need to find the new added ids
		    int entityIndex = -1 - refId;
		    value = generatedIds.get(referenceEntity + "-" + entityIndex);
		}
	    }
	    Integer type = columnDataTypes.get(entity + '-' + column);
	    LOGGER.debug("Type is " + type);
	    if (type != null && columnDataTypes.get(entity + '-' + column).intValue() == Types.TIMESTAMP) {
		statement.setTimestamp(i++, new Timestamp((Long) value));
	    } else if (type != null && columnDataTypes.get(entity + '-' + column).intValue() == Types.DATE) {
		statement.setDate(i++, new Date(DATE_FORMAT.parse(value.toString()).getTime()));
	    } else if (type != null && columnDataTypes.get(entity + '-' + column).intValue() == Types.LONGVARBINARY) {
		// The image like data must be passed as a Base64 encoded string
		// which we would decode and set in db
		byte[] data = Base64.decodeBase64(value.toString());
		statement.setBinaryStream(5, new ByteArrayInputStream(data), data.length);
	    } else {
		statement.setObject(i++, value);
	    }
	}
	return i;
    }

    public String createInsertQuery(String entity, Map<String, Object> object, Set<String> columns) {
	StringBuilder sb = new StringBuilder();
	sb.append("insert into " + entity);
	sb.append(columns.toString().replace('[', '(').replace(']', ')'));

	sb.append(" values (");
	for (int i = 0; i < object.size(); i++) {
	    sb.append('?');
	    if (i < object.size() - 1) {
		sb.append(',');
	    }
	}
	sb.append(')');

	String sql = sb.toString();
	return sql;
    }

    public int fetchGeneratedId(ResultSet keys) throws SQLException {
	int id = -1;
	if ((keys != null) && keys.next()) {
	    id = keys.getInt(1);
	    keys.close();
	}
	return id;
    }

    public String createUpdateQuery(String entity, Map<String, Object> object, Set<String> columns) {
	StringBuilder sb = new StringBuilder();
	sb.append("Update " + entity + " set ");

	Iterator<String> itr = columns.iterator();

	while (itr.hasNext()) {
	    sb.append(itr.next());
	    sb.append('=');
	    sb.append('?');
	    if (itr.hasNext()) {
		sb.append(',');
	    }
	}

	sb.append(" where id = ?");

	String sql = sb.toString();
	return sql;
    }

    public byte[] fetchBlob(InputStream blobData) {

	if (blobData == null) {
	    return null;
	}

	try {
	    int size = 0;
	    byte[] bytearray = new byte[8192];

	    ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 8);

	    while ((size = blobData.read(bytearray)) != -1) {
		bos.write(bytearray, 0, size);
	    }
	    return bos.toByteArray();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

}
