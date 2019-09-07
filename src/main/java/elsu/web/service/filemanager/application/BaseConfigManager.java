package elsu.web.service.filemanager.application;

import java.sql.*;

import elsu.support.*;
import elsu.web.service.filemanager.resources.*;

import java.util.*;

import javax.servlet.*;
import javax.ws.rs.core.*;

public abstract class BaseConfigManager {

	@Context
	protected UriInfo context;
	@Context
	protected ServletContext servlet;

	protected static volatile boolean inuse = false;
	protected GlobalSet resources = null;
	protected ConfigLoader config = null;

	@Override
	public void finalize() {
	}

	protected void initializeService() throws Exception {
		loadConfig();
	}

	// support methods for loading resource configuration from web.inf
	// load config if not already loaded, and initialize variables
	private void loadConfig() throws Exception {
		resources = (GlobalSet) servlet.getAttribute("shared.storage");

		// load config; if already created, then pull that from application
		config = (ConfigLoader) resources.get("shared.config");
		if (config == null) {
			if (!BaseConfigManager.inuse) {
				BaseConfigManager.inuse = true;

				String appPath = servlet.getRealPath("/WEB-INF/resources");
				String appConfig = "app.config";
				
				Boolean configDump = false;
				try {
					configDump = Boolean.valueOf(servlet.getInitParameter("config.dump").toString());
				} catch (Exception ex) {
					throw new Exception("config.dump not defined in web.xml context-param");
				}

				resources.add("app.config", appConfig);
				ConfigLoader.setLocalPath(appPath);
				//ConfigLoader.setLogPath(appPath);

				try {
					config = new ConfigLoader(appConfig, null);

					config.addProperty("application.framework.attributes.key.config.dump", configDump);
					
					if (configDump) {
						config.logInfo(config.toString());
					}
				} catch (Exception ex) {
					System.out.println(ex.getStackTrace());
				}

				// store for reuse by others
				resources.add("shared.config", config);
				resources.add("count", 0);

				// release use
				BaseConfigManager.inuse = false;
			} else {
				int i = 10;
				while ((i >= 1) && (config == null)) {
					// wait for resource to be free and reload
					idle(5000);

					// reload config from global storage
					config = (ConfigLoader) resources.get("shared.config");

					// loop update
					i--;
				}

				// if config is null, then exit
				if (config == null) {
					throw new Exception("config could not be initialized");
				}
			}
		}

		// example of global use of resources
		try {
			resources.lock();
			Object val = resources.get("count");
			resources.add("count", (Integer) val + 1);
		} finally {
			resources.unlock();
		}
	}

	protected Object getResource(String key, Object defaultValue) {
		Object result = null;

		try {
			resources.lock();

			if (!resources.containsKey(key)) {
				result = defaultValue;
			} else {
				result = resources.get(key);
			}
		} finally {
			resources.unlock();
		}

		return result;
	}

	protected void setResource(String key, Object value) {
		try {
			resources.lock();
			resources.add(key, value);
		} finally {
			resources.unlock();
		}
	}

	protected void clearResource(String key) {
		try {
			resources.lock();
			resources.clear(key);
		} finally {
			resources.unlock();
		}
	}

	protected void idle(long delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception exi) {
		}
	}
}
