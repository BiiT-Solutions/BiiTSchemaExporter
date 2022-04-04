package com.biit.persistence;

import org.testng.annotations.Test;

import com.biit.persistence.configuration.JpaSchemaExporterConfigurationReader;

@Test(groups = "jpaTest")
public class JpaTester {

	@Test
	public void jpaTest() {
		String directory = JpaSchemaExporterConfigurationReader.getInstance().getOutputDirectory();

		String outputFile = JpaSchemaExporterConfigurationReader.getInstance().getOutputFile();

		String[] packetsToScan = JpaSchemaExporterConfigurationReader.getInstance().getPackageToScan();

		String user = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseUser();

		String password = JpaSchemaExporterConfigurationReader.getInstance().getDatabasePassword();

		String host = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseHost();

		String port = JpaSchemaExporterConfigurationReader.getInstance().getDatabasePort();

		String databaseName = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseName();

		// String[] scriptsToAdd = ConfigurationReader.getInstance().getScriptsToAdd();

		String[] classesToIgnoreWhenCreatingDatabase = JpaSchemaExporterConfigurationReader.getInstance()
				.getClassesToIgnoreCreatingDatabase();

		String[] classesToIgnoreWhenUpdatingDatabase = JpaSchemaExporterConfigurationReader.getInstance()
				.getClassesToIgnoreUpdatingDatabase();

		// Launch the JpaSchemaExporter
		JpaSchemaExporter gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenCreatingDatabase);
		gen.createDatabaseScript(HibernateDialect.MYSQL, directory, outputFile, host, port, user, password, databaseName, true);
		gen = new JpaSchemaExporter(packetsToScan, classesToIgnoreWhenUpdatingDatabase);
		gen.updateDatabaseScript(HibernateDialect.MYSQL, directory, host, port, user, password, databaseName);
	}

}
