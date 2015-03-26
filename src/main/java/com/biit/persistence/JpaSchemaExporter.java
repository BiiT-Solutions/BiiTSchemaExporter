package com.biit.persistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import com.biit.persistence.configuration.ConfigurationReader;
import com.biit.persistence.logger.ExporterLogger;
import com.biit.utils.string.StringConverter;

@SuppressWarnings("rawtypes")
public class JpaSchemaExporter {
	private static final int ARG_OUTPUT_DIRECTORY = 0;
	private static final int ARG_OUTPUT_FILE = 1;
	private static final int ARG_PACKETS_TO_SCAN = 2;
	private static final int ARG_DATABASE_USER = 3;
	private static final int ARG_DATABASE_PASSWORD = 4;
	private static final int ARG_DATABASE_HOST = 5;
	private static final int ARG_DATABASE_PORT = 6;
	private static final int ARG_DATABASE_NAME = 7;
	private static final int ARG_SCRIPTS_TO_ADD = 8;
	private static final int ARG_CLASSES_TO_IGNORE_CREATE_DATABASE = 9;
	private static final int ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE = 10;

	private Configuration cfg;

	public JpaSchemaExporter(String[] packagesName, String[] classesToIgnore) {
		cfg = new Configuration();
		try {
			for (int i = 0; i < packagesName.length; i++) {
				for (Class clazz : getClasses(packagesName[i], classesToIgnore)) {
					cfg.addAnnotatedClass(clazz);
				}
			}
		} catch (Exception e) {
			ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
		}
	}

	/**
	 * Utility method used to fetch Class list based on a package name.
	 * 
	 * @param packageName
	 *            should be the package containing your annotated beans.
	 */
	private List<Class> getClasses(String packageName, String[] classesToIgnore) throws Exception {
		File directory = null;
		try {
			ClassLoader cld = getClassLoader();
			URL resource = getResource(packageName, cld);
			directory = new File(resource.getFile());
		} catch (NullPointerException ex) {
			throw new ClassNotFoundException(packageName + " (" + directory + ") does not appear to be a valid package");
		}
		return collectClasses(packageName, directory, classesToIgnore);
	}

	private ClassLoader getClassLoader() throws ClassNotFoundException {
		ClassLoader cld = Thread.currentThread().getContextClassLoader();
		if (cld == null) {
			throw new ClassNotFoundException("Can't get class loader.");
		}
		return cld;
	}

	private URL getResource(String packageName, ClassLoader cld) throws ClassNotFoundException {
		String path = packageName.replace('.', '/');
		URL resource = cld.getResource(path);
		if (resource == null) {
			throw new ClassNotFoundException("No resource for " + path);
		}
		return resource;
	}

	private List<Class> collectClasses(String packageName, File directory, String[] classesToIgnore)
			throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		if (directory.exists()) {
			String[] files = directory.list();
			for (String fileName : files) {
				File subdirectory = new File(directory.getPath() + File.separator + fileName);
				if (fileName.endsWith(".class")) {
					// removes the .class extension
					classes.add(Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6)));
				} else if (subdirectory.isDirectory()) {
					// Subpacket.
					classes.addAll(collectClasses(packageName + '.' + fileName, subdirectory, classesToIgnore));
				}
			}
		} else {
			throw new ClassNotFoundException(packageName + " is not a valid package");
		}
		// Remove ignored classes.
		for (String classToRemove : classesToIgnore) {
			for (Class<?> classAdded : new ArrayList<>(classes)) {
				if (classAdded.getSimpleName().equals(classToRemove)) {
					classes.remove(classAdded);
				}
			}
		}
		return classes;
	}

	/**
	 * Create a script that can generate a database for the selected dialect
	 * 
	 * @param dialect
	 * @param directory
	 */
	public void createDatabaseScript(HibernateDialect dialect, String directory, String outputFile, boolean onlyCreation) {
		cfg.setProperty("hibernate.hbm2ddl.auto", "create");
		cfg.setProperty("hibernate.dialect", dialect.getDialectClass());
		cfg.setProperty("hibernate.show_sql", "false");
		SchemaExport export = new SchemaExport(cfg);
		export.setDelimiter(";");
		export.setOutputFile(directory + File.separator + outputFile);
		export.setFormat(true);
		export.execute(true, false, false, onlyCreation);
	}

	public void updateDatabaseScript(HibernateDialect dialect, String outputDirectory, String host, String port,
			String username, String password, String databaseName) {
		cfg.setProperty("hibernate.hbm2ddl.auto", "update");
		cfg.setProperty("hibernate.dialect", dialect.getDialectClass());
		cfg.setProperty("hibernate.show_sql", "false");
		cfg.setProperty("hibernate.connection.driver_class", dialect.getDriver());
		cfg.setProperty("hibernate.connection.url", "jdbc:mysql://" + host + ":" + port + "/" + databaseName);
		cfg.setProperty("hibernate.connection.username", username);
		cfg.setProperty("hibernate.connection.password", password);

		SchemaUpdate update = new SchemaUpdate(cfg);
		update.setDelimiter(";");
		update.setOutputFile(outputDirectory + "updates/update_" + dialect.name().toLowerCase() + "_" + getDate()
				+ ".sql");
		update.setFormat(true);
		update.execute(true, true);
	}

	/**
	 * For executing.
	 * 
	 * @param args
	 *            args[{@value #ARG_OUTPUT_DIRECTORY}] -> outputDirectory, args[{@value #ARG_OUTPUT_FILE}] ->
	 *            outputFile, args[{@value #ARG_PACKETS_TO_SCAN}] -> packetsToScan args[{@value #ARG_DATABASE_USER}] ->
	 *            databaseUser, args[{@value #ARG_DATABASE_PASSWORD}] ->databasePassword, args[
	 *            {@value #ARG_DATABASE_HOST}] -> databaseHost, args[ {@value #ARG_DATABASE_PORT}] -> databasePort,
	 *            args[ {@value #ARG_CLASSES_TO_IGNORE_CREATE_DATABASE}] -> ignoreClasses
	 */
	public static void main(String[] args) {
		String directory;
		if (args.length <= ARG_OUTPUT_DIRECTORY) {
			directory = ConfigurationReader.getInstance().getOutputDirectory();
		} else {
			directory = args[ARG_OUTPUT_DIRECTORY] + File.separator;
		}

		String outputDirectory;
		if (args.length <= ARG_OUTPUT_FILE) {
			outputDirectory = ConfigurationReader.getInstance().getOutputDirectory();
		} else {
			outputDirectory = args[ARG_OUTPUT_FILE];
		}

		String[] packetsToScan;
		if (args.length <= ARG_PACKETS_TO_SCAN) {
			packetsToScan = ConfigurationReader.getInstance().getPackageToScan();
		} else {
			packetsToScan = StringConverter.convertToArray(args[ARG_PACKETS_TO_SCAN]);
		}

		String user;
		if (args.length <= ARG_DATABASE_USER) {
			user = ConfigurationReader.getInstance().getDatabaseUser();
		} else {
			user = args[ARG_DATABASE_USER];
		}
		String password;
		if (args.length < ARG_DATABASE_PASSWORD) {
			password = ConfigurationReader.getInstance().getDatabasePassword();
		} else {
			password = args[ARG_DATABASE_PASSWORD];
		}

		String host;
		if (args.length <= ARG_DATABASE_HOST) {
			host = ConfigurationReader.getInstance().getDatabaseHost();
		} else {
			host = args[ARG_DATABASE_HOST];
		}

		String port;
		if (args.length <= ARG_DATABASE_PORT) {
			port = ConfigurationReader.getInstance().getDatabasePort();
		} else {
			port = args[ARG_DATABASE_PORT];
		}

		String databaseName;
		if (args.length <= ARG_DATABASE_NAME) {
			databaseName = ConfigurationReader.getInstance().getDatabaseName();
		} else {
			databaseName = args[ARG_DATABASE_NAME];
		}

		String[] scriptsToAdd;
		if (args.length <= ARG_SCRIPTS_TO_ADD) {
			scriptsToAdd = ConfigurationReader.getInstance().getScriptsToAdd();
		} else {
			scriptsToAdd = StringConverter.convertToArray(args[ARG_SCRIPTS_TO_ADD]);
		}

		String[] classesToIgnoreWhenCreatingDatabase;
		if (args.length <= ARG_CLASSES_TO_IGNORE_CREATE_DATABASE) {
			classesToIgnoreWhenCreatingDatabase = ConfigurationReader.getInstance().getClassesToIgnoreCreatingDatabase();
		} else {
			classesToIgnoreWhenCreatingDatabase = StringConverter.convertToArray(args[ARG_CLASSES_TO_IGNORE_CREATE_DATABASE]);
		}
		
		String[] classesToIgnoreWhenUpdatingDatabase;
		if (args.length <= ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE) {
			classesToIgnoreWhenUpdatingDatabase = ConfigurationReader.getInstance().getPackageToScan();
		} else {
			classesToIgnoreWhenUpdatingDatabase = StringConverter.convertToArray(args[ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE]);
		}

		// Launch the JpaSchemaExporter
		JpaSchemaExporter gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenCreatingDatabase);
		gen.createDatabaseScript(HibernateDialect.MYSQL, directory, outputDirectory, true);
		gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenUpdatingDatabase);
		gen.updateDatabaseScript(HibernateDialect.MYSQL, directory, host, port, user, password, databaseName);

		// Add hibernate sequence table.
		addTextToFile(createHibernateSequenceTable(), directory + File.separator + outputDirectory);
		// Add extra information from a external script.
		addTextToFile(readFile(scriptsToAdd, Charset.forName("UTF-8")), directory + File.separator + outputDirectory);
	}

	private static String getDate() {
		Date ahora = new Date();
		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
		return formateador.format(ahora);
	}

	/**
	 * Hibernate databases needs this table.
	 * 
	 * @return
	 */
	private static String createHibernateSequenceTable() {
		String table = "\n\tCREATE TABLE `hibernate_sequence` (\n";
		table += "\t\t`next_val` bigint(20) DEFAULT NULL\n";
		table += "\t);\n\n";
		table += "\tLOCK TABLES `hibernate_sequence` WRITE;\n";
		table += "\tINSERT INTO `hibernate_sequence` VALUES (1);\n";
		table += "\tUNLOCK TABLES;\n";
		return table;
	}

	private static String readFile(String[] files, Charset charset) {
		StringBuilder result = new StringBuilder("");
		for (String file : files) {
			result.append("\n");

			File fileResource = new File(file);
			if (!fileResource.exists()) {
				// If this class is in a library, in testing the resources file is the home of the project. Add the
				// complete path.
				fileResource = new File("src/test/resources/" + file);
			}
			try (Scanner scanner = new Scanner(fileResource)) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					result.append("\t").append(line).append("\n");
				}
				scanner.close();
			} catch (FileNotFoundException e) {
				ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
			}
		}
		return result.toString();
	}

	private static void addTextToFile(String text, String file) {
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
			out.println(text);
		} catch (IOException e) {
		}
	}

}
