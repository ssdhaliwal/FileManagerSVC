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

import elsu.web.service.filemanager.application.*;
import elsu.web.service.filemanager.resources.*;

@Path("/download")
public class FileDownloadService extends BaseConfigManager {

	@Context
	ServletContext context;

	@Context
	HttpServletRequest request;

	private String mysqlDriver = "com.mysql.jdbc.Driver";
	private String mysqlHost = "localhost";
	private String mysqlPort = "3306";
	private String mysqlUser = "filemanageruser";
	private String mysqlPassword = "fmcu";

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
		return "File Downloader";
	}

	private File getWorkingDir(String userName) {
		ClassLoader classLoader = getClass().getClassLoader();
		String realPath = context.getRealPath("/WEB-INF/resources/examples");
		File exDir = new File(realPath);

		File resultDir = new File(exDir.getParent() + File.separator + "uploads" + File.separator + userName);
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
			Class.forName(mysqlDriver).newInstance();
		} catch (Exception e) {
			System.out.println(e.getMessage());

		}

		String mysqlURL = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort
				+ "/filemanagersvc?noAccessToProcedureBodies=true";
		Properties properties = new java.util.Properties();
		properties.setProperty("user", mysqlUser);
		properties.setProperty("password", mysqlPassword);

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

				byte[] buffer = new byte[8 * 1024];
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

}
