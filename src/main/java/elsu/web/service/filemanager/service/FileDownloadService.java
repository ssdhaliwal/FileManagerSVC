package elsu.web.service.filemanager.service;

import java.io.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/download")
public class FileDownloadService {
	@GET
	@Path("/status")
	@Produces("text/plain")
	public String getStatus() {
		return "File Downloader";
	}
}
