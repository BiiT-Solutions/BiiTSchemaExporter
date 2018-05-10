package com.biit.persistence;

public enum HibernateDialect {
	ORACLE("org.hibernate.dialect.Oracle10gDialect", ""),

	MYSQL("org.hibernate.dialect.MySQL57Dialect", "com.mysql.jdbc.Driver"),

	HSQL("org.hibernate.dialect.HSQLDialect", ""),

	H2("org.hibernate.dialect.H2Dialect", "");

	private String dialectClass;
	private String driver;

	private HibernateDialect(String dialectClass, String driver) {
		this.dialectClass = dialectClass;
		this.driver = driver;
	}

	public String getDialectClass() {
		return dialectClass;
	}

	public String getDriver() {
		return driver;
	}
}
