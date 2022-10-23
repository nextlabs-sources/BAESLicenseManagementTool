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

public class ProjectDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(ProjectDBHelper.class);

	/**
	 * Get the top n projects with names start with some characters
	 * 
	 * @param limit
	 *            Number of projects to get
	 * @param partialName
	 *            Characters
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return List of projects
	 */
	public static List<Project> getProjectsWithLimitAndPartialName(int limit,
			String partialName, int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<Project> resultList = new ArrayList<Project>();

		try {
			connection = DBHelper.getDatabaseConnection();
			preparedStatement = connection
					.prepareStatement("SELECT * FROM Project WHERE Deactivated = "
							+ deactivated
							+ " AND UPPER(Name) LIKE UPPER(?) AND ROWNUM <= "
							+ limit);
			preparedStatement.setString(1, partialName + "%");
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String description = resultSet.getString("Description");
				Project project = new Project(name.trim(),
						(description == null) ? "" : description.trim(),
						deactivated);
				resultList.add(project);
			}
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper getProjectsWithLimitAndPartialName(): "
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
						"ProjectDBHelper getProjectsWithLimitAndPartialName(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Return projects used for lazy loading
	 * 
	 * @param start
	 *            Page start
	 * @param size
	 *            Page size
	 * @param sortField
	 *            Field used to sort
	 * @param sortOrder
	 *            Order of sort
	 * @param filters
	 *            Filters used for querying
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return List of projects
	 */
	public static List<Project> getProjectListLazy(int start, int size,
			String sortField, String sortOrder, Map<String, Object> filters,
			int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<Project> resultList = new ArrayList<Project>();
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT outer.* FROM (SELECT ROWNUM rn, inner.* FROM (SELECT * FROM Project");
			StringBuilder whereClause = new StringBuilder();
			// concat all filters

			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause.append("UPPER(" + key + ") LIKE UPPER(?) AND ");
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (whereClause.toString().trim().length() != 0) {
				whereClause = new StringBuilder(whereClause.substring(0,
						whereClause.length() - 5));
				queryString.append(" WHERE Deactivated = " + deactivated
						+ " AND " + whereClause);
			} else {
				queryString.append(" WHERE Deactivated = " + deactivated + " ");
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
					// try as string using like
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					String value = filter.getValue().toString() + "%";
					preparedStatement.setString(parameterIndex.get(key), value);
				}

			}

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String description = resultSet.getString("Description");
				Project project = new Project(name.trim(),
						(description == null) ? "" : description.trim(),
						deactivated);
				resultList.add(project);
			}
		} catch (Exception ex) {
			LOG.error(
					"ProjectDBHelper getProjectListLazy(): " + ex.getMessage(),
					ex);
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
						"ProjectDBHelper getProjectListLazy(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Count all projects for lazy loading
	 * 
	 * @param filters
	 *            Filters used for querying
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return Number of projects
	 */
	public static int countAllProject(Map<String, Object> filters,
			int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		if (filters == null) {
			filters = new HashMap<String, Object>();
		}

		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT COUNT(*) FROM Project";
			String whereClause = "";

			int countFilters = 1;
			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					whereClause += "UPPER(" + key + ") LIKE UPPER(?) AND ";
					parameterIndex.put(key, countFilters);
					countFilters++;
				}

			}

			if (!whereClause.trim().equals("")) {
				whereClause = whereClause
						.substring(0, whereClause.length() - 5);
				queryString += " WHERE Deactivated = " + deactivated + " AND "
						+ whereClause;
			} else {
				queryString += " WHERE Deactivated = " + deactivated + " ";
			}

			preparedStatement = connection.prepareStatement(queryString);

			for (Map.Entry<String, Object> filter : filters.entrySet()) {
				if (!(filter.getValue().equals("") || filter.getValue() == null)) {
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					String value = filter.getValue().toString() + "%";
					preparedStatement.setString(parameterIndex.get(key), value);
				}

			}

			// LOG.info("Lazy query count: " + preparedStatement.toString());

			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				resultCount = resultSet.getInt(1);
			}
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper countAllProject(): " + ex.getMessage(),
					ex);
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
						"ProjectDBHelper countAllProject(): " + ex.getMessage(),
						ex);
			}
		}
		return resultCount;
	}

	/**
	 * Get a project from database
	 * 
	 * @param name
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return The project
	 */
	public static Project getProject(String name) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		Project project = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("SELECT * FROM Project WHERE Name = ?");
			statement.setString(1, name);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				String prjName = resultSet.getString("Name");
				String description = resultSet.getString("Description");
				int deactivated = resultSet.getInt("Deactivated");
				project = new Project(prjName.trim(),
						(description == null) ? "" : description.trim(),
						deactivated);
			}
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper getProject(): " + ex.getMessage(), ex);
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
				LOG.error("ProjectDBHelper getProject(): " + ex.getMessage(),
						ex);
			}
		}
		return project;
	}

	/**
	 * Create a new project in database
	 * 
	 * @param name
	 *            Project name
	 * @param description
	 *            Project description
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean createNewProject(String name, String description) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("INSERT INTO Project (Name, Description) VALUES (?, ?)");
			statement.setString(1, name);
			statement.setString(2, description);

			// LOG.info(queryString);
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper createNewProject(): " + ex.getMessage(),
					ex);
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
				LOG.error(
						"ProjectDBHelper createNewProject(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update project information in database
	 * 
	 * @param name
	 *            Project name
	 * @param description
	 *            Project description
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean updateProject(String name, String description) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("UPDATE Project SET Description = ? WHERE Name = ?");
			statement.setString(1, description);
			statement.setString(2, name);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper updateProject(): " + ex.getMessage(), ex);
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
				LOG.error(
						"ProjectDBHelper updateProject(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update project deactivated status in database
	 * 
	 * @param name
	 *            Project name
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean toggleDeactivationProject(String name, int deactivated) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("UPDATE Project SET Deactivated = "
							+ deactivated + " WHERE Name = ?");
			statement.setString(1, name);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error(
					"ProjectDBHelper toggleDeactivationProject(): "
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
				LOG.error(
						"ProjectDBHelper toggleDeactivationProject(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Delete a project in database
	 * 
	 * @param name
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteProject(String name) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("DELETE FROM Project WHERE Name = ?");
			statement.setString(1, name);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("ProjectDBHelper deleteProject(): " + ex.getMessage(), ex);
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
				LOG.error(
						"ProjectDBHelper deleteProject(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;
	}

}
