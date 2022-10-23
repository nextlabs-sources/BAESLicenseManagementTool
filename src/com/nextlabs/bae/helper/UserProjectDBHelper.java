package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserProjectDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(UserProjectDBHelper.class);

	/**
	 * Get all members of a project
	 * 
	 * @param project
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return List of users
	 */
	public static List<User> getUserListByProject(String project) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<User> resultList = new ArrayList<User>();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT * FROM user_project WHERE project = ?";

			preparedStatement = connection.prepareStatement(queryString);

			preparedStatement.setString(1, project);

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String aduser = resultSet.getString("aduser");
				String displayName = resultSet.getString("displayName");
				String email = resultSet.getString("email");
				resultList.add(new User(aduser, displayName, email));
			}
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper getUserListByProject():"
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
				LOG.error("UserProjectDBHelper getUserListByProject(): "
						+ ex.getMessage());
			}
		}
		return resultList;
	}

	/**
	 * Get a concatenated string representing users of a project
	 * 
	 * @param project
	 *            Project name
	 * @return string of users
	 */
	public static String getUsersListByProjectAsString(String project) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		StringBuilder result = new StringBuilder();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT aduser FROM user_project WHERE project = ?";

			preparedStatement = connection.prepareStatement(queryString);
			preparedStatement.setString(1, project);

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String aduser = resultSet.getString("aduser");
				result.append(aduser + ", ");
			}
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper getUserListByProject():"
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
				LOG.error("UserProjectDBHelper getUserListByProject(): "
						+ ex.getMessage());
			}
		}
		if (result.length() > 0) {
			return result.substring(0, result.length() - 2);
		} else {
			return result.toString();
		}
	}

	/**
	 * Get a concatenated string representing all projects of a user
	 * 
	 * @param aduser
	 *            sAMAccountName
	 * @return
	 */
	public static String getProjectsByUserAsString(String aduser) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		StringBuilder result = new StringBuilder();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT project FROM user_project WHERE aduser = ? ORDER BY project ASC";
			preparedStatement = connection.prepareStatement(queryString);
			preparedStatement.setString(1, aduser);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String project = resultSet.getString("project");
				result.append(project + ", ");
			}
		} catch (Exception ex) {
			LOG.error(
					"UserLicenseDBHelper getProjectsByUserAsString(): "
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
				LOG.error("UserProjectDBHelper getProjectsByUserAsString():"
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
	 * Get all members of a project - used by lazy loading
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
	 * @param project
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return List of users
	 */
	public static List<User> getUserListByProjectLazy(int start, int size,
			String sortField, String sortOrder, Map<String, Object> filters,
			String project) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<User> resultList = new ArrayList<User>();
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT outer.* FROM (SELECT ROWNUM rn, inner.* FROM (SELECT * FROM user_project");
			StringBuilder whereClause = new StringBuilder();

			// build query string
			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause.append("UPPER(" + key + ") LIKE UPPER(?) AND ");

					// for setting value of the filter later
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (whereClause.toString().trim().length() != 0) {
				whereClause = new StringBuilder(whereClause.substring(0,
						whereClause.length() - 5));
				queryString.append(" WHERE project = '" + project + "' AND "
						+ whereClause);
			} else {
				queryString.append(" WHERE project = '" + project + "' ");
			}

			// concate sort
			queryString.append(" ORDER BY "
					+ Character.toUpperCase(sortField.charAt(0))
					+ sortField.substring(1) + "");
			if (!sortOrder.equals("")) {
				queryString.append(" " + sortOrder);
			}

			// lazy loading
			queryString.append(" ) inner) outer WHERE outer.rn >= "
					+ (start + 1) + " AND outer.rn <= " + (start + size));

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
			LOG.error("UserProjectDBHelper getUserListByProjectLazy(): "
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
				LOG.error("UserProjectDBHelper getUserListBYProjectLazy(): "
						+ ex.getMessage());
			}
		}
		return resultList;
	}

	/**
	 * Count all members of a project - used for lazy loading
	 * 
	 * @param filters
	 *            Filters used for querying
	 * @param project
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return Number of members
	 */
	public static int countUserByProject(Map<String, Object> filters,
			String project) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT COUNT(*) FROM user_project");
			StringBuilder whereClause = new StringBuilder();

			// build query string
			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause.append("UPPER(" + key + ") LIKE UPPER(?) AND ");

					// for setting value of the filter later
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (whereClause.toString().trim().length() != 0) {
				whereClause = new StringBuilder(whereClause.substring(0,
						whereClause.length() - 5));
				queryString.append(" WHERE project = '" + project + "' AND "
						+ whereClause);
			} else {
				queryString.append(" WHERE project = '" + project + "' ");
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
			LOG.error("UserProjectDBHelper countUserByProject(): "
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
				LOG.error("UserProjectDBHelper countUserByProject(): "
						+ ex.getMessage());
			}
		}
		return resultCount;
	}

	/**
	 * Count all user_project relationships in database
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return Number of relationships
	 */
	public static int countUserProject() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;

		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT COUNT(*) FROM user_project";

			preparedStatement = connection.prepareStatement(queryString);

			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				resultCount = resultSet.getInt(1);
			}
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper countUserProject(): "
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
				LOG.error("UserProjectDBHelper countUserProject(): "
						+ ex.getMessage());
			}
		}
		return resultCount;
	}

	/**
	 * Assign some projects to some users
	 * 
	 * @param projects
	 *            List of projects
	 * @param users
	 *            List of users
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean assignProjectsToUsers(List<Project> projects,
			List<User> users) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (projects == null) {
				LOG.info("UserProjectDBHelper assignProjectsToUsers(): There is no license");
				return true;
			}
			if (users == null) {
				LOG.info("UserProjectDBHelper assignProjectsToUsers(): There is no user");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "INSERT INTO User_Project (aduser, displayname, email, project)"
					+ " VALUES (?, ?, ?, ?) ";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				User user = users.get(u);
				for (int i = 0; i < projects.size(); i++) {
					statement.setString(1, user.getAduser());
					statement.setString(2, user.getDisplayName());
					statement.setString(3, user.getEmail());
					statement.setString(4, projects.get(i).getName());
					statement.addBatch();
				}
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper assignProjectsToUsers(): "
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
				LOG.error("UserProjectDBHelper assignProjectsToUsers(): "
						+ ex.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove some projects frome some users
	 * 
	 * @param projects
	 *            List of projects
	 * @param users
	 *            List of users
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeProjectsFromUsers(List<Project> projects,
			List<User> users) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (projects == null) {
				LOG.info("UserProjectDBHelper removeProjectsFromUsers(): There is no license");
				return true;
			}
			if (users == null) {
				LOG.info("UserPRojectDBHelper removeProjectsFromUsers(): There is no user");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM User_Project"
					+ " WHERE aduser = ? AND project = ? ";

			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			for (int u = 0; u < users.size(); u++) {
				User user = users.get(u);
				for (int i = 0; i < projects.size(); i++) {
					statement.setString(1, user.getAduser());
					statement.setString(2, projects.get(i).getName());
					statement.addBatch();
				}
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper removeProjectsFromUsers(): "
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
				LOG.error("UserProjectDBHelper removeProjectsFromUsers(): "
						+ ex.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Delete all relationships of a project in database
	 * 
	 * @param project
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteProjectRelationship(String project) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM User_Project WHERE project = ?";

			statement = connection.prepareStatement(query);
			statement.setString(1, project);
			statement.execute();
			connection.commit();
			return true;
		} catch (Exception ex) {
			LOG.error("UserProjectDBHelper deleteProjectRelationship(): "
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
				LOG.error("UserProjectDBHelper deleteProjectRelationship(): "
						+ ex.getMessage());
				return false;
			}
		}
	}

}
