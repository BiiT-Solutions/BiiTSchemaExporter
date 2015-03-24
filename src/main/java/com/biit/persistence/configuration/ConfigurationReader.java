package com.biit.persistence.configuration;

import com.biit.persistence.logger.ExporterLogger;
import com.biit.utils.configuration.PropertiesSourceFile;
import com.biit.utils.configuration.SystemVariablePropertiesSourceFile;
import com.biit.utils.configuration.exception.PropertyNotFoundException;
import com.biit.utils.string.StringConverter;

public class ConfigurationReader extends com.biit.utils.configuration.ConfigurationReader {

	private static final String DATABASE_CONFIG_FILE = "settings.conf";
	private static final String ABCD_SYSTEM_VARIABLE_CONFIG = "USMO_CONFIG";

	// Tags
	private static final String TAG_DATABASE_NAME = "jpaschemaexporter.database.name";
	private static final String TAG_DATABASE_HOST = "jpaschemaexporter.database.host";
	private static final String TAG_DATABASE_PORT = "jpaschemaexporter.database.port";
	private static final String TAG_DATABASE_USER = "jpaschemaexporter.database.user";
	private static final String TAG_DATABASE_PASSWORD = "jpaschemaexporter.database.password";
	private static final String TAG_OUTPUT_DIRECTORY = "jpaschemaexporter.output.directory";
	private static final String TAG_OUTPUT_FILE = "jpaschemaexporter.output.file";
	private static final String TAG_SCAN_PACKAGES = "jpaschemaexporter.scan.packages";
	private static final String TAG_SCRIPTS_TO_ADD = "jpaschemaexporter.add.scripts";
	private static final String TAG_CLASSES_TO_IGNORE = "jpaschemaexporter.ignore.classes";

	// Default
	private static final String DEFAULT_DATABASE_NAME = "database";
	private static final String DEFAULT_DATABASE_HOST = "localhost";
	private static final String DEFAULT_DATABASE_PORT = "3306";
	private static final String DEFAULT_DATABASE_USER = "testuser";
	private static final String DEFAULT_DATABASE_PASSWORD = "asd123";
	private static final String DEFAULT_OUTPUT_DIRECTORY = "./DatabaseSchema";
	private static final String DEFAULT_OUTPUT_FILE = "create_mysql.sql";
	private static final String DEFAULT_SCAN_PACKAGES = "com.biit.persistence";
	private static final String DEFAULT_SCRIPTS_TO_ADD = "";
	private static final String DEFAULT_CLASSES_TO_IGNORE = "";

	private static ConfigurationReader instance;

	private ConfigurationReader() {
		super();

		addProperty(TAG_DATABASE_NAME, DEFAULT_DATABASE_NAME);
		addProperty(TAG_DATABASE_HOST, DEFAULT_DATABASE_HOST);
		addProperty(TAG_DATABASE_PORT, DEFAULT_DATABASE_PORT);
		addProperty(TAG_DATABASE_USER, DEFAULT_DATABASE_USER);
		addProperty(TAG_DATABASE_PASSWORD, DEFAULT_DATABASE_PASSWORD);
		addProperty(TAG_OUTPUT_DIRECTORY, DEFAULT_OUTPUT_DIRECTORY);
		addProperty(TAG_OUTPUT_FILE, DEFAULT_OUTPUT_FILE);
		addProperty(TAG_SCAN_PACKAGES, DEFAULT_SCAN_PACKAGES);
		addProperty(TAG_CLASSES_TO_IGNORE, DEFAULT_CLASSES_TO_IGNORE);
		addProperty(TAG_SCRIPTS_TO_ADD, DEFAULT_SCRIPTS_TO_ADD);

		addPropertiesSource(new PropertiesSourceFile(DATABASE_CONFIG_FILE));
		addPropertiesSource(new SystemVariablePropertiesSourceFile(ABCD_SYSTEM_VARIABLE_CONFIG, DATABASE_CONFIG_FILE));

		readConfigurations();
	}

	public static ConfigurationReader getInstance() {
		if (instance == null) {
			synchronized (ConfigurationReader.class) {
				if (instance == null) {
					instance = new ConfigurationReader();
				}
			}
		}
		return instance;
	}

	@Override
	public String getProperty(String propertyId) {
		try {
			return super.getProperty(propertyId);
		} catch (PropertyNotFoundException e) {
			ExporterLogger.errorMessage(this.getClass().getName(), e);
			return null;
		}
	}

	public String getDatabaseName() {
		return getProperty(TAG_DATABASE_NAME);
	}

	public String getDatabaseHost() {
		return getProperty(TAG_DATABASE_HOST);
	}

	public String getDatabasePort() {
		return getProperty(TAG_DATABASE_PORT);
	}

	public String getDatabaseUser() {
		return getProperty(TAG_DATABASE_USER);
	}

	public String getDatabasePassword() {
		return getProperty(TAG_DATABASE_PASSWORD);
	}

	public String getOutputDirectory() {
		return getProperty(TAG_OUTPUT_DIRECTORY);
	}

	public String getOutputFile() {
		return getProperty(TAG_OUTPUT_FILE);
	}

	public String[] getPackageToScan() {
		return StringConverter.convertToArray(getProperty(TAG_SCAN_PACKAGES));
	}

	public String[] getClassesToIgnore() {
		return StringConverter.convertToArray(getProperty(TAG_CLASSES_TO_IGNORE));
	}

	public String[] getScriptsToAdd() {
		return StringConverter.convertToArray(getProperty(TAG_SCRIPTS_TO_ADD));
	}

}
