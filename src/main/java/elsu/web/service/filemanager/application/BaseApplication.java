package elsu.web.service.filemanager.application;

import java.util.*;

import javax.servlet.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.server.*;

@ApplicationPath("/rest")
public class BaseApplication extends ResourceConfig {

    @Context
    ServletContext servletContext;

    public BaseApplication() {
        // Define the package which contains the service classes.
    	addRestResourceClasses();
    }

    private void addRestResourceClasses() {
        packages("elsu.web.service.filemanager.service.HelloWorldService.class");
        packages("elsu.web.service.filemanager.service.FileDownloadService.class");
        packages("elsu.web.service.filemanager.service.FileUploadService.class");
    }

}
