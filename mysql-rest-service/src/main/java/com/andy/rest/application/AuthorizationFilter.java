package com.andy.rest.application;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import com.andy.security.api.Role;
import com.andy.security.api.Secured;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(AuthorizationFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest webRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

	LOGGER.debug(this.getClass().getName() + " invoked...");

	HttpSession session = webRequest.getSession();

	String dbName = webRequest.getParameter("dbName");

	Map<String, Role> resourceRoleMap = (Map<String, Role>) session.getAttribute("resourceRoleMap");

	LOGGER.debug("Authorization filter " + resourceRoleMap + ", url-" + webRequest.getPathInfo());

	try {

	    LOGGER.debug("Checking for authorization annotations");

	    // Get the resource method which matches with the requested URL
	    // Extract the roles declared by it
	    Method resourceMethod = resourceInfo.getResourceMethod();
	    List<Role> methodRoles = extractRoles(resourceMethod);

	    // Check if the user is allowed to execute the method
	    // The method annotations override the class annotations
	    if (!methodRoles.isEmpty()) {
		LOGGER.debug("Method annotation found for authorization");
		handleSecurity(dbName, resourceRoleMap, methodRoles);
	    }

	    // Get the resource class which matches with the requested URL
	    // Extract the roles declared by it
	    Class<?> resourceClass = resourceInfo.getResourceClass();
	    List<Role> classRoles = extractRoles(resourceClass);

	    if (!classRoles.isEmpty()) {
		LOGGER.debug("Class annotation found for authorization");
		handleSecurity(dbName, resourceRoleMap, classRoles);
	    }
	    
	} catch (Exception e) {
	    LOGGER.error("User not authorized to access the resource " + dbName);
	    requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build());
	}
    }

    protected void handleSecurity(String dbName, Map<String, Role> resourceRoleMap, List<Role> classRoles) throws Exception {
	if(resourceRoleMap == null) {
	    throw new Exception("Unauthorized access to protected resource - Login Required");
	}
	if (!checkPermissions(resourceRoleMap, classRoles, dbName)) {
	    throw new Exception("Unauthorized access to protected resource - Limited Access");
	}
    }

    // Extract the roles from the annotated element
    protected List<Role> extractRoles(AnnotatedElement annotatedElement) {
	if (annotatedElement == null) {
	    return new ArrayList<Role>();
	} else {
	    com.andy.security.api.Secured secured = annotatedElement.getAnnotation(Secured.class);
	    if (secured == null) {
		return new ArrayList<Role>();
	    } else {
		Role[] allowedRoles = secured.value();
		return Arrays.asList(allowedRoles);
	    }
	}
    }

    protected boolean checkPermissions(Map<String, Role> resourceRoleMap, List<Role> allowedRoles, String dbName) throws Exception {
	// Check if the user contains one of the allowed roles
	// Throw an Exception if the user has not permission to execute the
	// method

	Role role = resourceRoleMap.get(dbName);
	if (role == null) {
	    LOGGER.debug("Unauthorized");
	    return false;
	}

	for (Role allowedRole : allowedRoles) {
	    if (role.isCompatible(allowedRole)) {
		return true;
	    }
	}

	LOGGER.debug("User not authorized to access " + dbName);
	return false;
    }
}
