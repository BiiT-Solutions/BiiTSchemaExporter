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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import com.biit.persistence.logger.ExporterLogger;

/**
 * This is the utility class for getting SessionFactory
 */
public class HibernateInitializator {

	private static SessionFactory sessionFactory = buildSessionFactory();
	private static Configuration configuration;

	private static SessionFactory buildSessionFactory() {
		try {
			configuration = new Configuration();
			configuration.configure();
			StandardServiceRegistryBuilder sb = new StandardServiceRegistryBuilder();
			sb.applySettings(configuration.getProperties());
			StandardServiceRegistry standardServiceRegistry = sb.build();
			sessionFactory = configuration.buildSessionFactory(standardServiceRegistry);
			return sessionFactory;
		} catch (HibernateException ex) {
			ExporterLogger.errorMessage(HibernateInitializator.class.getName(), ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

}
