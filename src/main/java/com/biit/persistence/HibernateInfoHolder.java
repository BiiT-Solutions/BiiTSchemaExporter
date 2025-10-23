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

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class HibernateInfoHolder {

    private static Metadata metadata;

    private static SessionFactoryImplementor sessionFactory;

    private static SessionFactoryServiceRegistry serviceRegistry;

    public static Metadata getMetadata() {
        return metadata;
    }

    public static void setMetadata(Metadata metadata) {
        HibernateInfoHolder.metadata = metadata;
    }

    public static SessionFactoryImplementor getSessionFactory() {
        return sessionFactory;
    }

    public static void setSessionFactory(SessionFactoryImplementor sessionFactory) {
        HibernateInfoHolder.sessionFactory = sessionFactory;
    }

    public static SessionFactoryServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public static void setServiceRegistry(SessionFactoryServiceRegistry serviceRegistry) {
        HibernateInfoHolder.serviceRegistry = serviceRegistry;
    }
}
