package ws.file.svc.application;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

//import object.data.GlobalSet;

@ApplicationPath("/files")
public class FileServiceApplication extends Application {

    @Context
    ServletContext servletContext;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();

//        servletContext.setAttribute("shared.storage", new GlobalSet());
        
        addRestResourceClasses(resources);
        return resources;
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(ws.file.svc.service.FileServiceDownload.class);
        resources.add(ws.file.svc.service.FileServiceUpload.class);
    }

}
