package com.biit.persistence;

import org.testng.annotations.Test;

import com.biit.persistence.configuration.ConfigurationReader;

@Test(groups = "jpaTest")
public class JpaTester {

	@Test
	public void jpaTest() {
		String directory = ConfigurationReader.getInstance().getOutputDirectory();

		String outputFile = ConfigurationReader.getInstance().getOutputFile();

		String[] packetsToScan = ConfigurationReader.getInstance().getPackageToScan();

		String user = ConfigurationReader.getInstance().getDatabaseUser();

		String password = ConfigurationReader.getInstance().getDatabasePassword();

		String host = ConfigurationReader.getInstance().getDatabaseHost();

		String port = ConfigurationReader.getInstance().getDatabasePort();

		String databaseName = ConfigurationReader.getInstance().getDatabaseName();

		// String[] scriptsToAdd = ConfigurationReader.getInstance().getScriptsToAdd();

		String[] classesToIgnoreWhenCreatingDatabase = ConfigurationReader.getInstance()
				.getClassesToIgnoreCreatingDatabase();

		String[] classesToIgnoreWhenUpdatingDatabase = ConfigurationReader.getInstance()
				.getClassesToIgnoreUpdatingDatabase();

		// Launch the JpaSchemaExporter
		JpaSchemaExporter gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenCreatingDatabase);
		gen.createDatabaseScript(HibernateDialect.MYSQL, directory, outputFile, host, port, user, password, databaseName, true);
		gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenUpdatingDatabase);
		gen.updateDatabaseScript(HibernateDialect.MYSQL, directory, host, port, user, password, databaseName);
	}

}
