package elsu.web.service.filemanager.services;

import java.io.*;
import java.math.*;
import java.sql.*;
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

	private String dbJDBC = "mysql";
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbDatabase = "filemanagersvc";
	private String dbOptionalParams = "noAccessToProcedureBodies=true";
	private String dbHost = "localhost";
	private String dbPort = "3306";
	private String dbUser = "filemanageruser";
	private String dbPassword = "fmcu";
	private String fileStoreType = "relative";
	private String fileStorePath = "/WEB-INF/resources/examples";
	private Integer bufferSize = 8;

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
		String realPath = "";
		if (fileStoreType.equals("relative")) {
			realPath = context.getRealPath(fileStorePath);
		} else {
			realPath = fileStorePath;
		}
		File exDir = new File(realPath);

		File resultDir = new File(exDir.getPath() + File.separator + userName);
		if (!resultDir.exists()) {
			System.out.println(
					"working directory " + resultDir.getAbsolutePath() + " doesnt exist, attempt to create it... ");
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

		try {
			this.initializeService();
			
			fileStoreType = config.getProperty("application.service.fileStore.type").toString();
			fileStorePath = config.getProperty("application.service.fileStore.path").toString();
			dbJDBC = config.getProperty("application.service.database.jdbc").toString();
			dbDriver = config.getProperty("application.service.database.driver").toString();
			dbHost = config.getProperty("application.service.database.host").toString();
			dbPort = config.getProperty("application.service.database.port").toString();
			dbDatabase = config.getProperty("application.service.database.database").toString();
			dbOptionalParams = config.getProperty("application.service.database.optionalParameters").toString();
			dbUser = config.getProperty("application.service.database.user").toString();
			dbPassword = config.getProperty("application.service.database.password").toString();
			bufferSize = Integer.valueOf(config.getProperty("application.service.fileUpload.bufferSize").toString());
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
					.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST")
					.build();
		}
		
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
						System.out.println("File field " + fieldName + " with file name " + itemName + " detected.");
						File dir = getWorkingDir(userName);

						final File targetFile = new File(dir.getPath() + File.separator + itemName.toLowerCase());
						OutputStream outStream = new FileOutputStream(targetFile);

						byte[] buffer = new byte[bufferSize * 1024];
						int bytesRead;
						while ((bytesRead = stream.read(buffer)) != -1) {
							outStream.write(buffer, 0, bytesRead);
						}
						IOUtils.closeQuietly(outStream);

						storeUserData(userName, itemName, targetFile);
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

		String json = "result: " + uploaded;

		return Response.status(Response.Status.OK).entity(json).header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "POST").build();
	}

	void storeUserData(String user, String file, File inputFile) {
		java.nio.file.Path path = inputFile.toPath();
		String mimeType = "";
		InputStream inStream = null;
		
		try {
			Class.forName(dbDriver).newInstance();
		} catch (Exception e) {
			System.out.println(e.getMessage());

		}
		
		String mysqlURL = "jdbc:" + dbJDBC + "://"+dbHost+":"+dbPort+"/" + dbDatabase + "?" + dbOptionalParams;
		Properties properties = new java.util.Properties();
		properties.setProperty("user", dbUser);
		properties.setProperty("password", dbPassword);
		
		java.sql.Connection conn = null;
		try {
			conn = java.sql.DriverManager.getConnection(mysqlURL, properties);
			
			long oUserId, oFileId, oDataId;
			
			mimeType = java.nio.file.Files.probeContentType(path);
			inStream = new FileInputStream(inputFile);
			
			String sql = "call updateFile(?, ?, ?, ?, ?, ?, ?, ?)";
			CallableStatement statement = conn.prepareCall(sql);
			System.out.println(user);
			statement.setString(1, user);
			statement.setString(2, file);
			statement.setString(3, "N");
			statement.setString(4, mimeType);
			statement.setBinaryStream(5, inStream);
			statement.registerOutParameter(6, Types.BIGINT);
			statement.registerOutParameter(7, Types.BIGINT);
			statement.registerOutParameter(8, Types.BIGINT);
			statement.execute();
			
			// get the  output params
			oUserId = statement.getLong(6);
			oFileId = statement.getLong(7);
			oDataId = statement.getLong(8);
			
			System.out.println(oUserId + ".." + oFileId + ".." + oDataId);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (inStream != null) {
				IOUtils.closeQuietly(inStream);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception exi) { }
			}
		}
	}
}
