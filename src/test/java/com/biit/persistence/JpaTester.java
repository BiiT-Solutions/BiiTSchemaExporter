package com.biit.persistence;

/*-
 * #%L
 * JPA Schema Exporter
 * %%
 * Copyright (C) 2022 - 2025 BiiT Sourcing Solutions S.L.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

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
