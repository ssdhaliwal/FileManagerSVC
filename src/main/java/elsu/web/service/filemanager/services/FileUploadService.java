package elsu.web.service.filemanager.services;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.util.*;
import org.apache.commons.io.*;

import elsu.web.service.filemanager.application.*;

//import com.google.gson.Gson;

@Path("/upload")
public class FileUploadService extends BaseConfigManager {

	@Context
	ServletContext context;
	
	@Context 
	HttpServletRequest request;

	public FileUploadService() {
	}

	@Override
	protected void initializeService() throws Exception {
		super.initializeService();
	}

	@GET
	@Path("/status")
	@Produces("text/plain")
	public String getStatus() {
		try {
			this.initializeService();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		return "File Uploader";
	}

	private File getWorkingDir(String userName) {
		ClassLoader classLoader = getClass().getClassLoader();
		String realPath = context.getRealPath("/WEB-INF/resources/examples");
		File exDir = new File(realPath);
		
		File resultDir = new File(exDir.getParent() + File.separator + "uploads" + File.separator + userName);
		if (!resultDir.exists()) {
			System.out.println("working directory " + resultDir.getAbsolutePath() + " doesnt exist, attempt to create it... ");
			resultDir.mkdirs();
		}

		return resultDir;
	}

	@POST
	@Path("/file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response storeFile() {
		List<String> uploaded = new ArrayList<String>();
		String userName = "", data = "";
		
		System.out.println(request.getContentType());

		// checks whether there is a file upload request or not
		if (ServletFileUpload.isMultipartContent(request)) {
			final FileItemFactory factory = new DiskFileItemFactory();
			final ServletFileUpload fileUpload = new ServletFileUpload(factory);
			try {
				/*
				 * parseRequest returns a list of FileItem but in old (pre-java5) style
				 */
				// final List items = fileUpload.parseRequest(request);
				FileItemIterator iter = fileUpload.getItemIterator(request);

				while (iter.hasNext()) {
					final FileItemStream item = iter.next();
					final String itemName = item.getName();
					final String fieldName = item.getFieldName();

					InputStream stream = item.openStream();

					if (item.isFormField()) {
						data = Streams.asString(stream);
						System.out.println("Field Name: " + fieldName + ", Value: " + data);
						
						if (fieldName.equals("userName")) {
							userName = data;
						}
					} else {
						// if the user name is not specified; then exit with error
						System.out.println(
								"File field " + fieldName + " with file name " + itemName + " detected.");
						File dir = getWorkingDir(userName);

						final File targetFile = new File(dir.getPath() + File.separator + itemName.toLowerCase());
						OutputStream outStream = new FileOutputStream(targetFile);

						byte[] buffer = new byte[8 * 1024];
						int bytesRead;
						while ((bytesRead = stream.read(buffer)) != -1) {
							outStream.write(buffer, 0, bytesRead);
						}
						IOUtils.closeQuietly(outStream);

						uploaded.add(dir.getPath() + File.separator + itemName.toLowerCase());
					}
					IOUtils.closeQuietly(stream);
				}
			} catch (FileUploadException e) {
				System.out.println(e);
				e.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
						.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST")
						.build();
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
						.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST")
						.build();
			}
		}

		//Gson gson = new Gson();
		//String json = gson.toJson(uploaded);
		String json = "result: " + uploaded;

		return Response.status(Response.Status.OK).entity(json).header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "POST").build();
	}

}
