package com.andy.rest.application;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.andy.rest.util.Utils;

import io.swagger.jaxrs.config.BeanConfig;

public class Bootstrap extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.0");
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setBasePath(config.getServletContext().getContextPath()+ "/api");        
        beanConfig.setResourcePackage(Utils.BUNDLE.getProperty("rest.packages"));        
        beanConfig.setScan(true);
    }
}
