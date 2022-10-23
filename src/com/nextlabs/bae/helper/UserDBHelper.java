package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(UserDBHelper.class);

	/**
	 * Get unique user lists from two relationship tables
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return List of users sAMAccountName
	 */
	public static List<String> getUniqueUserFromDB() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<String> resultList = new ArrayList<String>();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT ul.col1, up.col2 FROM (SELECT DISTINCT aduser as col1 FROM User_License) ul "
					+ "FULL OUTER JOIN (SELECT DISTINCT aduser as col2 FROM User_Project) up ON ul.col1 = up.col2";
			preparedStatement = connection.prepareStatement(queryString);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String col1 = resultSet.getString("col1");
				String col2 = resultSet.getString("col2");
				if (col1 == null) {
					resultList.add(col2);
				} else {
					if (col2 == null) {
						resultList.add(col1);
					} else {
						if (col1.equals(col2))
							resultList.add(col1);
					}
				}
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getUniqueUserFromDB(): "
							+ ex.getMessage(), ex);
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (preparedStatement != null) {
					preparedStatement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error(
						"UserLicenseDBHelper getUniqueUserFromDB():"
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Remove user relationships from database
	 * 
	 * @param users
	 *            List of users sAMAccountName
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeUsersFromDatabase(List<String> users) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (users == null) {
				LOG.info("UserLicenseDBHelper removeUsersFromDatabase(): There is no user");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM User_License" + " WHERE aduser = ?";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				String user = users.get(u);
				statement.setString(1, user);
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
			statement.close();

			query = "DELETE FROM User_Project" + " WHERE aduser = ?";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				String user = users.get(u);
				statement.setString(1, user);
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper removeUsersFromDatabase():"
							+ ex.getMessage(), ex);
			return false;
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error("UserLicenseDBHelper removeUsersFromDatabase():"
						+ ex.getMessage());
				return false;
			}
		}
		return true;
	}
}
