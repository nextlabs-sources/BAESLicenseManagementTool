package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserLicenseDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(UserLicenseDBHelper.class);

	/**
	 * Get all users who have a license
	 * 
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return List of users
	 */
	public static List<User> getUserByLicense(String license) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<User> resultList = new ArrayList<User>();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT * FROM user_license WHERE license = ?";
			preparedStatement = connection.prepareStatement(queryString);
			preparedStatement.setString(1, license);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String aduser = resultSet.getString("aduser");
				String displayName = resultSet.getString("displayName");
				String email = resultSet.getString("email");
				resultList.add(new User(aduser, displayName, email));
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getUserByLicense(): "
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
						"UserLicenseDBHelper getUserByLicense():"
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get a concatenated string representing all licenses of a user
	 * 
	 * @param aduser
	 *            sAMAccountName
	 * @param category
	 *            License category. Can be null
	 * @return
	 */
	public static String getLicensesByUserAsString(String aduser,
			String category) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		StringBuilder result = new StringBuilder();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString;
			if (category == null) {
				queryString = "SELECT license FROM user_license WHERE aduser = ? ORDER BY license ASC";
			} else {
				queryString = "SELECT ul.license FROM user_license ul, license l WHERE ul.aduser = ? AND ul.license = l.name AND l.category = ? ORDER BY ul.license ASC";
			}
			preparedStatement = connection.prepareStatement(queryString);
			if (category == null) {
				preparedStatement.setString(1, aduser);
			} else {
				preparedStatement.setString(1, aduser);
				preparedStatement.setString(2, category);
			}
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String license = resultSet.getString("license");
				result.append(license + ", ");
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getUserByLicenseAsString(): "
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
						"UserLicenseDBHelper getUsgetUserByLicenseAsStringerByLicense():"
								+ ex.getMessage(), ex);
			}
		}
		if (result.length() > 0) {
			return result.substring(0, result.length() - 2);
		} else {
			return result.toString();
		}
	}

	/**
	 * Get all users who have a license
	 * 
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return List of user names
	 */
	public static List<String> getUserNameByLicense(String license) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<String> resultList = new ArrayList<String>();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT aduser FROM user_license WHERE license = ?";
			preparedStatement = connection.prepareStatement(queryString);
			preparedStatement.setString(1, license);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String aduser = resultSet.getString("aduser");
				resultList.add(aduser);
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getUserByLicense(): "
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
						"UserLicenseDBHelper getUserByLicense():"
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get all users who have a license - used for lazy loading
	 * 
	 * @param start
	 *            Page start
	 * @param size
	 *            Page size
	 * @param sortField
	 *            Field used to sort
	 * @param sortOrder
	 *            Sort order
	 * @param filters
	 *            Filters used for querying
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return List of users
	 */
	public static List<User> getUserListByLicenseLazy(int start, int size,
			String sortField, String sortOrder, Map<String, Object> filters,
			String license) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<User> resultList = new ArrayList<User>();
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT outer.* FROM (SELECT ROWNUM rn, inner.* FROM (SELECT * FROM user_license");
			StringBuilder whereClause = new StringBuilder();

			// build query string
			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause.append("UPPER(").append(key)
							.append(") LIKE UPPER(?) AND ");

					// for setting value of the filter later
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (whereClause.toString().trim().length() != 0) {
				whereClause = new StringBuilder(whereClause.substring(0,
						whereClause.length() - 5));
				queryString.append(" WHERE license = '").append(license)
						.append("' AND ").append(whereClause);
			} else {
				queryString.append(" WHERE license = '").append(license)
						.append("' ");
			}

			// concate sort
			queryString.append(" ORDER BY ")
					.append(Character.toUpperCase(sortField.charAt(0)))
					.append(sortField.substring(1)).append("");
			if (!sortOrder.equals("")) {
				queryString.append(" ").append(sortOrder);
			}

			// lazy loading
			queryString.append(" ) inner) outer WHERE outer.rn >= ")
					.append((start + 1)).append(" AND outer.rn <= ")
					.append((start + size));

			preparedStatement = connection.prepareStatement(queryString
					.toString());

			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					// set value of the filter
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					String value = filter.getValue().toString() + "%";
					preparedStatement.setString(parameterIndex.get(key), value);
				}

			}

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String aduser = resultSet.getString("aduser");
				String displayName = resultSet.getString("displayName");
				String email = resultSet.getString("email");
				resultList.add(new User(aduser, displayName, email));
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getUserListByLicenseLazy():"
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
				LOG.error("UserLicenseDBHelper getUserListByLicenseLazy():"
						+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Count all users who have a license
	 * 
	 * @param filters
	 *            Filters used for querying
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return Number of users
	 */
	public static int countUserByLicense(Map<String, Object> filters,
			String license) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT COUNT(*) FROM user_license");
			StringBuilder whereClause = new StringBuilder();

			// build query string
			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause.append("UPPER(").append(key)
							.append(") LIKE UPPER(?) AND ");

					// for setting value of the filter later
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (whereClause.toString().trim().length() != 0) {
				whereClause = new StringBuilder(whereClause.substring(0,
						whereClause.length() - 5));
				queryString.append(" WHERE license = '").append(license)
						.append("' AND ").append(whereClause);
			} else {
				queryString.append(" WHERE license = '").append(license)
						.append("' ");
			}

			preparedStatement = connection.prepareStatement(queryString
					.toString());

			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					// set value of the filter
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					String value = filter.getValue().toString() + "%";
					preparedStatement.setString(parameterIndex.get(key), value);
				}

			}

			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				resultCount = resultSet.getInt(1);
			}
		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper countUserByLicense():"
					+ ex.getMessage());
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
				LOG.error("UserLicenseDBHelper countUserByLicense():"
						+ ex.getMessage());
			}
		}
		return resultCount;
	}

	/**
	 * Count all user_license relationships
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return Number of relationships
	 */
	public static int countUserLicense() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;

		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT COUNT(*) FROM user_license";

			preparedStatement = connection.prepareStatement(queryString);

			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				resultCount = resultSet.getInt(1);
			}
		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper countUserLicense():"
					+ ex.getMessage());
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
				LOG.error("UserLicenseDBHelper countUserLicense():"
						+ ex.getMessage());
			}
		}
		return resultCount;
	}

	/**
	 * Assign some licenses to some users
	 * 
	 * @param licenses
	 *            List of licenses
	 * @param users
	 *            List of users
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean assignLicensesToUsers(List<License> licenses,
			List<User> users) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (licenses == null) {
				LOG.info("UserLicenseDBHelper assignLicensesToUsers(): There is no license");
				return true;
			}
			if (users == null) {
				LOG.info("UserLicenseDBHelper assignLicensesToUsers(): There is no user");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "INSERT INTO User_License (aduser, displayname, email, license)"
					+ " VALUES (?, ?, ?, ?) ";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				User user = users.get(u);
				for (int i = 0; i < licenses.size(); i++) {
					statement.setString(1, user.getAduser());
					statement.setString(2, user.getDisplayName());
					statement.setString(3, user.getEmail());
					statement.setString(4, licenses.get(i).getName());
					statement.addBatch();
				}
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper assignLicensesToUsers():"
					+ ex.getMessage());
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
				LOG.error("UserLicenseDBHelper assignLicensesToUsers():"
						+ ex.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove some licenses from some users
	 * 
	 * @param licenses
	 *            List of licenses
	 * @param users
	 *            List of users
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeLicensesFromUsers(List<License> licenses,
			List<User> users) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (licenses == null) {
				LOG.info("UserLicenseDBHelper removeLicensesFromUsers(): There is no license");
				return true;
			}
			if (users == null) {
				LOG.info("UserLicenseDBHelper removeLicensesFromUsers(): There is no user");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM User_License"
					+ " WHERE aduser = ? AND license = ?";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				User user = users.get(u);
				for (int i = 0; i < licenses.size(); i++) {
					statement.setString(1, user.getAduser());
					statement.setString(2, licenses.get(i).getName());
					statement.addBatch();
				}
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper removeLicensesFromUsers():"
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
				LOG.error("UserLicenseDBHelper removeLicensesFromUsers():"
						+ ex.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Return list of licenses to be removed from user when that user is removed
	 * from some projects
	 * 
	 * @param user
	 *            User sAMAccountName
	 * @param targetProject
	 *            Project name of the license
	 * @param projectsToBeRemove
	 *            Projects that are going to be removed from users
	 * @exception Exception
	 *                Any exception
	 * @return List of licenses
	 */
	public static List<License> licensesToRemoveOnRemovingUserProject(
			User user, String targetProject, List<Project> projectsToBeRemoved) {
		List<License> resultList = new ArrayList<License>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT * FROM license l, license_project lp, user_license ul "
							+ "WHERE l.name = lp.license AND lp.project = ? "
							+ "AND l.name = ul.license AND ul.aduser = ?"
							+ " AND (select count(lp1.project) FROM license_project lp1, user_project up WHERE lp1.license = l.name");
			for (int i = 0; i < projectsToBeRemoved.size(); i++) {
				queryString.append(" AND lp1.project != ?");
			}
			queryString
					.append(" AND up.aduser = ? AND up.project = lp1.project) = 0");

			statement = connection.prepareStatement(queryString.toString());
			statement.setString(1, targetProject);
			statement.setString(2, user.getAduser());
			for (int i = 3; i < projectsToBeRemoved.size() + 3; i++) {
				statement
						.setString(i, projectsToBeRemoved.get(i - 3).getName());
			}
			statement.setString(3 + projectsToBeRemoved.size(),
					user.getAduser());
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String parties = resultSet.getString("Parties");
				String category = resultSet.getString("Category");
				String label = resultSet.getString("Label");
				Timestamp effectiveDate = resultSet
						.getTimestamp("EffectiveDate");
				Timestamp expiration = resultSet.getTimestamp("Expiration");
				int groupEnabled = resultSet.getInt("GroupEnabled");
				String groupName = resultSet.getString("GroupName");
				License license = new License(name.trim(),
						(parties == null) ? "" : parties.trim(),
						(category == null) ? "" : category.trim(),
						(label == null) ? "" : label.trim(), effectiveDate,
						expiration, 0, groupEnabled, (groupName == null) ? ""
								: groupName.trim());
				resultList.add(license);
			}

		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper licensesToRemoveOnRemovingUserProject():"
							+ ex.getMessage(), ex);
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error("UserLicenseDBHelper licensesToRemoveOnRemovingUserProject():"
						+ ex.getMessage());
			}
		}
		return resultList;
	}

	/**
	 * Return list of licenses to be removed from user when a project of user
	 * got deactivated
	 * 
	 * @param user
	 *            User
	 * @param targetProject
	 *            Project name
	 * @return List of licenses
	 */
	public static List<License> licensesToRemoveOnDeactivatingProject(
			User user, String targetProject) {
		List<License> resultList = new ArrayList<License>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT * FROM license l, license_project lp, user_license ul "
							+ "WHERE l.name = lp.license AND lp.project = ? "
							+ "AND l.name = ul.license AND ul.aduser = ? "
							+ "AND (select count(lp1.project) "
							+ "FROM license_project lp1, user_project up WHERE lp1.license = l.name");
			queryString.append(" AND lp1.project != ?");
			queryString
					.append(" AND up.aduser = ? AND up.project = lp1.project) = 0");
			queryString
					.append(" AND (select count(lp2.project) from license_project lp2 WHERE lp2.license = l.name");
			queryString.append(" AND lp2.project != ?) != 0");

			statement = connection.prepareStatement(queryString.toString());
			statement.setString(1, targetProject);
			statement.setString(2, user.getAduser());
			statement.setString(3, targetProject);
			statement.setString(4, user.getAduser());
			statement.setString(5, targetProject);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String parties = resultSet.getString("Parties");
				String category = resultSet.getString("Category");
				String label = resultSet.getString("Label");
				Timestamp effectiveDate = resultSet
						.getTimestamp("EffectiveDate");
				Timestamp expiration = resultSet.getTimestamp("Expiration");
				int groupEnabled = resultSet.getInt("GroupEnabled");
				String groupName = resultSet.getString("GroupName");
				License license = new License(name.trim(),
						(parties == null) ? "" : parties.trim(),
						(category == null) ? "" : category.trim(),
						(label == null) ? "" : label.trim(), effectiveDate,
						expiration, 0, groupEnabled, (groupName == null) ? ""
								: groupName.trim());
				resultList.add(license);
			}

		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper licensesToRemoveOnRemovingUserProject():"
							+ ex.getMessage(), ex);
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error("UserLicenseDBHelper licensesToRemoveOnRemovingUserProject():"
						+ ex.getMessage());
			}
		}
		return resultList;
	}

	/**
	 * Validate a license of a user
	 * 
	 * @param user
	 *            sAMaccountName of user
	 * @param license
	 *            License name
	 * @param projectToExclude
	 *            License's project
	 * @exception Exception
	 *                Any exception
	 * @return True if the relationship is valid, False otherwise
	 */
	public static boolean validateUserLicense(User user, License license,
			String projectToExclude) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT COUNT(up.project) FROM user_project up, license_project lp "
					+ "WHERE up.aduser = ? AND lp.project = up.project AND lp.license = ?";
			if (projectToExclude != null) {
				queryString += " AND up.project != ?";
			}
			statement = connection.prepareStatement(queryString);
			statement.setString(1, user.getAduser());
			statement.setString(2, license.getName());
			if (projectToExclude != null) {
				statement.setString(3, projectToExclude);
			}
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				int resultCount = resultSet.getInt(1);
				if (resultCount == 0)
					return false;
				else
					return true;
			} else {
				return false;
			}

		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper validateUserLicense():"
					+ ex.getMessage());
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error("UserLicenseDBHelper validateUserLicense():"
						+ ex.getMessage());
			}
		}
		return false;
	}

	/**
	 * Return list of users who a license should be removed from when that
	 * license's project is updated
	 * 
	 * @param license
	 *            The license
	 * @exception Exception
	 *                Any exception
	 * @return List of user sAMAccountName
	 */
	public static List<String> usersToRemoveLicenseOnUpdatingLicenseProject(
			License license) {
		List<String> resultList = new ArrayList<String>();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT ul.aduser FROM user_license ul WHERE ul.license = ?"
					+ " AND (select count(lp1.project) FROM license_project lp1, user_project up WHERE lp1.license = ?"
					+ " AND up.aduser = ul.aduser AND up.project = lp1.project) = 0";

			statement = connection.prepareStatement(queryString);
			statement.setString(1, license.getName());
			statement.setString(2, license.getName());
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String user = resultSet.getString("aduser");
				resultList.add(user);
			}

		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper usersToRemoveLicenseOnUpdatingLicenseProject():"
					+ ex.getMessage());
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException ex) {
				LOG.error("UserLicenseDBHelper usersToRemoveOnUpdatingLicenseProject():"
						+ ex.getMessage());
			}
		}
		return resultList;
	}

	/**
	 * Delete all user_license relationships of a license
	 * 
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteLicenseRelationship(String license) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM User_License" + " WHERE license = ?";

			statement = connection.prepareStatement(query);
			statement.setString(1, license);
			connection.commit();
			return true;
		} catch (Exception ex) {
			LOG.error("UserLicenseDBHelper deleteLicenseRelationship():"
					+ ex.getMessage());
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
				LOG.error("UserLicenseDBHelper deleteLicenseRelationship():"
						+ ex.getMessage());
				return false;
			}
		}
	}
}
