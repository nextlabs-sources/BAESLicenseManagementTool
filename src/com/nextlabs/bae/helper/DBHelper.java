package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public class DBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(DBHelper.class);
	private static DataSource datasource;
	private static String url;
	private static String driver;
	private static String user;
	private static String password;

	/**
	 * Get database connection via DriverManager
	 * 
	 * @return database connection
	 */
	public static Connection getDatabaseConnectionSimple() {
		Connection connection = null;
		try {
			Class.forName(PropertyLoader.bAESProperties.getProperty("driver"));
			String url = PropertyLoader.bAESProperties
					.getProperty("connection-string");
			String user = PropertyLoader.bAESProperties.getProperty("sql-user");
			String password = PropertyLoader.bAESProperties
					.getProperty("sql-password");

			connection = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			LOG.error(
					"ActivityDBHelper getDatabaseConnectionSimple(): SQL Exception: "
							+ e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			LOG.error(
					"ActivityDBHelper getDatabaseConnectionSimple(): Class Not Found Exception: "
							+ e.getMessage(), e);
		}

		if (connection == null) {
			return null;
		}

		return new net.sf.log4jdbc.ConnectionSpy(connection);
	}

	public static boolean testDB() {
		Connection connection = DBHelper.getDatabaseConnection();
		if (connection == null) {
			return false;
		} else {
			try {
				connection.close();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			return true;
		}
	}

	private static boolean validatePoolProperties() {
		if (!PropertyLoader.bAESProperties.getProperty("connection-string")
				.equals(url)
				|| !PropertyLoader.bAESProperties.getProperty("driver").equals(
						driver)
				|| !PropertyLoader.bAESProperties.getProperty("sql-user")
						.equals(user)
				|| !PropertyLoader.bAESProperties.getProperty("sql-password")
						.equals(password)) {
			LOG.info("DBHelper validatePoolProperties() Setting up pool properties");
			url = PropertyLoader.bAESProperties
					.getProperty("connection-string");
			driver = PropertyLoader.bAESProperties.getProperty("driver");
			user = PropertyLoader.bAESProperties.getProperty("sql-user");
			password = PropertyLoader.bAESProperties
					.getProperty("sql-password");
			return false;
		}
		return true;
	}

	public static Connection getDatabaseConnection() {
		Connection connection = null;
		long lCurrentTime = System.nanoTime();

		try {
			if (datasource == null || !validatePoolProperties()) {
				LOG.info("DBHelper getDatabaseConnection() Initialize datasource");

				if (url == null || driver == null || user == null
						|| password == null) {
					url = PropertyLoader.bAESProperties
							.getProperty("connection-string");
					driver = PropertyLoader.bAESProperties
							.getProperty("driver");
					user = PropertyLoader.bAESProperties
							.getProperty("sql-user");
					password = PropertyLoader.bAESProperties
							.getProperty("sql-password");
				}

				PoolProperties p = new PoolProperties();
				p.setUrl(url);
				p.setDriverClassName(driver);
				p.setUsername(user);
				p.setPassword(password);
				p.setMaxIdle(10);
				p.setRemoveAbandonedTimeout(300);
				p.setRemoveAbandoned(true);
				p.setDefaultAutoCommit(true);
				p.setMaxWait(100000);
				p.setMaxActive(30);
				p.setValidationQuery("select 1 from dual");
				p.setTestOnConnect(false);
				p.setTestOnBorrow(true);
				p.setTestWhileIdle(true);
				p.setTimeBetweenEvictionRunsMillis(30000);
				datasource = new org.apache.tomcat.jdbc.pool.DataSource(p);
			}

			connection = datasource.getConnection();
		} catch (Exception e) {
			LOG.error("ActivityDBHelper getDatabaseConnection(): Exception: "
					+ e.getMessage(), e);
		}

		if (connection == null) {
			connection = getDatabaseConnectionSimple();
		}

		LOG.debug("DBHelper getDatabaseConnection() completed. Time spent: "
				+ ((System.nanoTime() - lCurrentTime) / 1000000.00) + "ms");
		if (connection == null)
			return null;
		return new net.sf.log4jdbc.ConnectionSpy(connection);

	}
}
