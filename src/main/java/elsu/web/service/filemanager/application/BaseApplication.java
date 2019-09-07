package elsu.web.service.filemanager.application;

import java.util.*;

import javax.servlet.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.server.*;

import elsu.web.service.filemanager.resources.*;
import elsu.web.service.filemanager.services.*;

@ApplicationPath("/rest")
public class BaseApplication extends Application {

    @Context
    ServletContext servletContext;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();

        servletContext.setAttribute("shared.storage", new GlobalSet());
        
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(HelloWorldService.class);
        resources.add(FileDownloadService.class);
        resources.add(FileUploadService.class);
    }

}
