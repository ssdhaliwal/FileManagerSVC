package ws.file.svc.service;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.tomcat.util.http.fileupload.*;
import org.apache.tomcat.util.http.fileupload.disk.*;
import org.apache.tomcat.util.http.fileupload.servlet.*;
import org.apache.tomcat.util.http.fileupload.util.*;

//import com.google.gson.Gson;

@Path("/upload")
public class FileServiceUpload {

	@Context
	ServletContext servletContext;

	@GET
	@Path("/status")
	@Produces("text/plain")
	public String getStatus() {
		return "File Uploader";
	}

	private File getWorkingDir(@Context ServletContext context) {

		ClassLoader classLoader = getClass().getClassLoader();
		String realPath = context.getRealPath("/WEB-INF/resources/examples");
		File exDir = new File(realPath);
		
		//File exDir = new File(classLoader.getResource("resources/examples").getPath());

		File resultDir = new File(exDir.getParent() + File.separator + "results");
		if (!resultDir.exists()) {
			System.out.println("working directory " + resultDir.getAbsolutePath() + " doesnt exist, attempt to create it... ");
			resultDir.mkdirs();
		}

		return resultDir;
	}

	@POST
	@Path("/file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response storeFile(@Context HttpServletRequest request, @Context ServletContext context) {
		// String candidateName = null;
		File dir = getWorkingDir(context);
		List<String> uploaded = new ArrayList<String>();

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

				// if (items != null) {
				// final Iterator iter = items.iterator();
				while (iter.hasNext()) {
					final FileItemStream item = iter.next();
					final String itemName = item.getName();
					final String fieldName = item.getFieldName();
					// final String fieldValue = item.getString();

					InputStream stream = item.openStream();

					if (item.isFormField()) {
						// candidateName = stream.asString(stream);
						System.out.println("Field Name: " + fieldName + ", andidate Name: " + Streams.asString(stream));
					} else {
						System.out.println(
								"File field " + fieldName + " with file name " + item.getName() + " detected.");

						final File targetFile = new File(dir.getPath() + File.separator + itemName.toLowerCase());
						// System.out.println("Saving the file: " + savedFile.getName());
						// item.write(savedFile);

						OutputStream outStream = new FileOutputStream(targetFile);

						byte[] buffer = new byte[8 * 1024];
						int bytesRead;
						while ((bytesRead = stream.read(buffer)) != -1) {
							outStream.write(buffer, 0, bytesRead);
						}
						IOUtils.closeQuietly(stream);
						IOUtils.closeQuietly(outStream);

						uploaded.add(dir.getPath() + File.separator + itemName.toLowerCase());
					}

				}
				// }
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
		String json = "{result: " + uploaded + "}";

		return Response.ok().entity(json).header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "POST").type(MediaType.MULTIPART_FORM_DATA).build();
	}

}
