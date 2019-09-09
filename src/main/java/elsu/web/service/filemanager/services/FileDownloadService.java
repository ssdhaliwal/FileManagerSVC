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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.*;

import com.fasterxml.jackson.databind.*;

import elsu.web.service.filemanager.application.*;
import elsu.web.service.filemanager.resources.*;

@Path("/download")
public class FileDownloadService extends BaseConfigManager {

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

	public FileDownloadService() {
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
		
		return "File Downloader";
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

	public static Map<String, String[]> getQueryParameters(HttpServletRequest request) {
		Map<String, String[]> queryParameters = new HashMap<>();
		String queryString = request.getQueryString();

		if (StringUtils.isEmpty(queryString)) {
			return queryParameters;
		}

		String[] parameters = queryString.split("&");

		for (String parameter : parameters) {
			String[] keyValuePair = parameter.split("=");
			String[] values = queryParameters.get(keyValuePair[0]);
			values = ArrayUtils.add(values, keyValuePair.length == 1 ? "" : keyValuePair[1]); // length is one if no
																								// value is available.
			queryParameters.put(keyValuePair[0], values);
		}
		return queryParameters;
	}

	@POST
	@Path("/file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response retreiveFile() {
		List<String> uploaded = new ArrayList<String>();
		FileDownloadType fdt = new FileDownloadType();
		String fileName = "", userName = "", data = "";

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
			bufferSize = Integer.valueOf(config.getProperty("application.service.fileDownload.bufferSize").toString());
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
					.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST")
					.build();
		}

		if (ServletFileUpload.isMultipartContent(request)) {
			System.out.println("multipart form data");

			final FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload fileUpload = new ServletFileUpload(factory);

			try {
				FileItemIterator iter = fileUpload.getItemIterator(request);

				while (iter.hasNext()) {
					final FileItemStream item = iter.next();
					final String fieldName = item.getFieldName();
					// final String fieldValue = item.getString();

					InputStream stream = item.openStream();

					if (item.isFormField()) {
						data = Streams.asString(stream);
						System.out.println("Field Name: " + fieldName + ", Value: " + data);

						if (fieldName.equals("fileName")) {
							fileName = data;
						} else if (fieldName.equals("userName")) {
							userName = data;

							// check if file exists
							File dir = getWorkingDir(userName);
							/*
							 * String fullFileName = dir.getPath() + File.separator +
							 * fileName.toLowerCase(); java.nio.file.Path path = new
							 * File(fullFileName).toPath(); mimeType =
							 * java.nio.file.Files.probeContentType(path);
							 * 
							 * System.out.println("mimeType//" + mimeType);
							 * 
							 * InputStream inStream = new FileInputStream(fullFileName);
							 * ByteArrayOutputStream baos = new ByteArrayOutputStream(); byte[] buffer = new
							 * byte[8 * 1024]; int len;
							 * 
							 * while ((len = inStream.read(buffer)) != -1) { baos.write(buffer, 0, len); }
							 * 
							 * IOUtils.closeQuietly(inStream);
							 * 
							 * System.out.println(baos.size()); imageData = baos.toByteArray();
							 * IOUtils.closeQuietly(baos);
							 */

							fdt = retrieveUserData(userName, fileName);
						}
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
		System.out.println(request.getContentType() + "//" + fdt.mimeType);

		return Response.ok(fdt.imageData).header("Content-Type", fdt.mimeType)
				.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
	}

	FileDownloadType retrieveUserData(String user, String file) {
		FileDownloadType fdt = new FileDownloadType();
		InputStream outStream = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

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

			String sql = "select user_id, username, file_id, filename, ispublic, mimetype, date_updated, data "
					+ "from vwFileData " + "where username = ? and filename = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			System.out.println(user);
			statement.setString(1, user);
			statement.setString(2, file);
			ResultSet rs = statement.executeQuery();

			if (rs.next() == true) {
				fdt.mimeType = rs.getString(6);
				outStream = rs.getBinaryStream(8);

				byte[] buffer = new byte[bufferSize * 1024];
				int len;

				while ((len = outStream.read(buffer)) != -1) {
					baos.write(buffer, 0, len);
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (outStream != null) {
				IOUtils.closeQuietly(outStream);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception exi) {
				}
			}
		}

		fdt.imageData = baos.toByteArray();
		IOUtils.closeQuietly(baos);
		return fdt;
	}

	@POST
	@Path("/list")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response retreiveList() {
		List<String> uploaded = new ArrayList<String>();
		FileUserType fut = new FileUserType();
		String userName = "", data = "", jsonString = "";

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
			bufferSize = Integer.valueOf(config.getProperty("application.service.fileDownload.bufferSize").toString());
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
					.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "POST")
					.build();
		}

		if (ServletFileUpload.isMultipartContent(request)) {
			System.out.println("multipart form data");

			final FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload fileUpload = new ServletFileUpload(factory);

			try {
				FileItemIterator iter = fileUpload.getItemIterator(request);

				while (iter.hasNext()) {
					final FileItemStream item = iter.next();
					final String fieldName = item.getFieldName();
					// final String fieldValue = item.getString();

					InputStream stream = item.openStream();

					if (item.isFormField()) {
						data = Streams.asString(stream);
						System.out.println("Field Name: " + fieldName + ", Value: " + data);

						if (fieldName.equals("userName")) {
							userName = data;

							fut = retrieveUserList(userName);
							
							ObjectMapper mapper = new ObjectMapper();
							jsonString = mapper.writeValueAsString(fut);
						}
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
		System.out.println(request.getContentType() + "//" + jsonString);

		return Response.ok(jsonString)
				.header("Content-Disposition", "").build();
	}

	FileUserType retrieveUserList(String user) {
		FileUserType fut = new FileUserType();
		InputStream outStream = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

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

			String sql = "select user_id, username, file_id, filename, ispublic, mimetype, date_updated "
					+ "from vwFile " + "where username = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			System.out.println(user);
			statement.setString(1, user);
			ResultSet rs = statement.executeQuery();

			fut.setUserName(user);
			while (rs.next()) {
		      fut.setUserId(rs.getLong(1));
		      fut.setUserName(rs.getString(2));
		      
		      FileFileType fft = new FileFileType();
		      fft.setFileId(rs.getLong(3));
		      fft.setFileName(rs.getString(4));
		      fft.setIsPublic(rs.getString(5));
		      fft.setMimeType(rs.getString(6));
		      fft.setDateUpdated(rs.getTimestamp(7));
		      
		      fut.getFileList().add(fft);
	      }
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (outStream != null) {
				IOUtils.closeQuietly(outStream);
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception exi) {
				}
			}
		}

		return fut;
	}

}
