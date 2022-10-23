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

public class LicenseProjectDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(LicenseDBHelper.class);

	/**
	 * Associate projects with a license in database
	 * 
	 * @param name
	 *            License name
	 * @param projects
	 *            List of projects
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean assignProjectsToLicense(String name,
			List<String> projects) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (projects == null) {
				LOG.info("LicenseProjectDBHelper assignProjectsToLicense(): There is no project");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "INSERT INTO License_Project (License, Project)"
					+ " VALUES (?, ?) ";
			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);

			statement.setString(1, name);
			for (int i = 0; i < projects.size(); i++) {
				statement.setString(2, projects.get(i));
				statement.addBatch();
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error(
					"LicenseProjectDBHelper assignProjectsToLicense(): "
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
				LOG.error("LicenseProjectDBHelper assignProjectsToLicense(): "
						+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove associations between a license and multiple projects
	 * 
	 * @param name
	 *            License name
	 * @param projects
	 *            List of projects
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeProjectsFromLicense(String name,
			List<String> projects) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (projects == null) {
				LOG.info("LicenseProjectDBHelper removeProjectsFromLicense(): There is no project");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM License_Project WHERE License = ? AND Project = ? ";
			statement = connection.prepareStatement(query);
			connection.setAutoCommit(false);
			statement.setString(1, name);

			for (int i = 0; i < projects.size(); i++) {
				statement.setString(2, projects.get(i));
				statement.addBatch();
			}

			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseProjectDBHelper removeProjectsFromLicense(): "
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
						"LicenseProjectDBHelper removeProjectsFromLicense(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove a license relationships with projects in database
	 * 
	 * @param name
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeAllLicenseRelationship(String name) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM License_Project WHERE License = ?";
			statement = connection.prepareStatement(query);
			statement.setString(1, name);
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseProjectDBHelper removeAllLicenseRelationship(): "
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
						"LicenseProjectDBHelper removeAllLicenseRelationship(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove a project relationships with licenses in database
	 * 
	 * @param name
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean removeAllProjectRelationship(String name) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			String query = "DELETE FROM License_Project WHERE Project = ?";
			statement = connection.prepareStatement(query);
			statement.setString(1, name);
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseProjectDBHelper removeAllProjectRelationship(): "
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
						"LicenseProjectDBHelper removeAllProjectRelationship(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Get all projects of a license
	 * 
	 * @param licenseName
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return List of projects
	 */
	public static List<Project> getProjectByLicense(String licenseName) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		List<Project> resultList = new ArrayList<Project>();

		try {
			connection = DBHelper.getDatabaseConnection();
			String query = "SELECT p.* FROM Project p, License_Project lp "
					+ "WHERE lp.License = ? AND lp.Project = p.Name";
			statement = connection.prepareStatement(query);
			statement.setString(1, licenseName);
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				String prjName = resultSet.getString("Name");
				String description = resultSet.getString("Description");
				int deactivated = resultSet.getInt("Deactivated");
				resultList.add(new Project(prjName.trim(),
						(description == null) ? "" : description.trim(),
						deactivated));
			}
		} catch (Exception ex) {
			LOG.error(
					"LicenseProjectDBHelper getProjectByLicense(): "
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
				LOG.error(
						"LicenseProjectDBHelper getProjectByLicense(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get all project names of a license
	 * 
	 * @param licenseName
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return List of project names
	 */
	public static List<String> getProjectNameByLicense(String licenseName) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		List<String> resultList = new ArrayList<String>();

		try {
			connection = DBHelper.getDatabaseConnection();
			String query = "SELECT p.name FROM Project p, License_Project lp "
					+ "WHERE lp.License = ? AND lp.Project = p.Name";
			statement = connection.prepareStatement(query);
			statement.setString(1, licenseName);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				String prjName = resultSet.getString("Name");
				resultList.add(prjName);
			}
		} catch (Exception ex) {
			LOG.error(
					"LicenseProjectDBHelper getProjectNameByLicense(): "
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
				LOG.error("LicenseProjectDBHelper getProjectNameByLicense(): "
						+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get all license of a project
	 * 
	 * @param projectQuery
	 *            Project name
	 * @param deactivated
	 *            Deactivated status of licenses
	 * @exception Exception
	 *                Any exception
	 * @return List of licenses
	 */
	public static List<License> getLicenseByProject(String projectQuery,
			int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();

		try {
			connection = DBHelper.getDatabaseConnection();
			preparedStatement = connection
					.prepareStatement("SELECT l.* FROM License l, License_Project lp "
							+ "WHERE lp.Project = ? "
							+ "AND l.Name = lp.License "
							+ "AND l.Deactivated = " + deactivated);
			preparedStatement.setString(1, projectQuery);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String parties = resultSet.getString("Parties");
				String label = resultSet.getString("Label");
				String category = resultSet.getString("Category");
				Timestamp effectiveDate = resultSet
						.getTimestamp("EffectiveDate");
				Timestamp expiration = resultSet.getTimestamp("Expiration");
				int groupEnabled = resultSet.getInt("GroupEnabled");
				String groupName = resultSet.getString("GroupName");
				License license = new License(name.trim(),
						(parties == null) ? "" : parties.trim(),
						(category == null) ? "" : category.trim(),
						(label == null) ? "" : label.trim(), effectiveDate,
						expiration, deactivated, groupEnabled,
						(groupName == null) ? "" : groupName.trim());
				resultList.add(license);
			}
		} catch (Exception ex) {
			LOG.error(
					"LicenseProjectDBHelper getLicenseByProject(): "
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
						"LicenseProjectDBHelper getLicenseByProject(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get all licenses of a project - used for lazy loading
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
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return List of licenses
	 */
	public static List<License> getLicensesListLazyByProject(int start,
			int size, String sortField, String sortOrder,
			Map<String, Object> filters, String project, int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT outer.* FROM (SELECT ROWNUM rn, inner.* FROM (SELECT * FROM License");
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
				queryString
						.append(" WHERE Deactivated = "
								+ deactivated
								+ " AND "
								+ whereClause
								+ " AND Name IN (SELECT License from License_Project WHERE Project = '"
								+ project + "') ");
			} else {
				queryString
						.append(" WHERE Deactivated = "
								+ deactivated
								+ " AND Name IN (SELECT License from License_Project WHERE Project = '"
								+ project + "') ");
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
						expiration, deactivated, groupEnabled,
						(groupName == null) ? "" : groupName.trim());
				resultList.add(license);
			}
		} catch (Exception ex) {
			LOG.error("LicenseProjectDBHelper getLicensesListLazyByProject(): "
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
						"LicenseProjectDBHelper getLicensesListLazyByProject(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Count licenses of a project - used for lazy loading
	 * 
	 * @param filters
	 *            Filters used for querying
	 * @param deactivated
	 *            Deactivated status
	 * @param project
	 *            Project name
	 * @exception Exception
	 *                Any exception
	 * @return Number of licenses
	 */
	public static int countLicenseProject(Map<String, Object> filters,
			int deactivated, String project) {
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
			StringBuilder queryString = new StringBuilder(
					"SELECT COUNT(*) FROM License");
			StringBuilder whereClause = new StringBuilder();

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
				queryString
						.append(" WHERE Deactivated = "
								+ deactivated
								+ " AND "
								+ whereClause
								+ " AND Name IN (SELECT License from License_Project WHERE Project = '"
								+ project + "') ");
			} else {
				queryString
						.append(" WHERE Deactivated = "
								+ deactivated
								+ " AND Name IN (SELECT License from License_Project WHERE Project = '"
								+ project + "') ");
			}

			preparedStatement = connection.prepareStatement(queryString
					.toString());

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
			LOG.error(
					"LicenseProjectDBHelper countLicenseProject(): "
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
						"LicenseProjectDBHelper countLicenseProject(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultCount;
	}

	/**
	 * Get top n licenses of some projects which belong to a category and have
	 * names contain some characters
	 * 
	 * @param category
	 *            License category
	 * @param partialName
	 *            Characters to be contained
	 * @param limit
	 *            Maximum number of licenses to be returned
	 * @param projects
	 *            List of projects
	 * @param deactivated
	 *            Deactivates status
	 * @exception Exception
	 *                Any exception
	 * @return List of licenses
	 */
	public static List<License> getLicensesByCategoryWithPartialNameAndLimitAndProjectConstraints(
			String category, String partialName, int limit,
			List<Project> projects, int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder projectConstraint = new StringBuilder(
					"SELECT License FROM License_Project WHERE ");
			for (int i = 0; i < projects.size(); i++) {
				if (i != projects.size() - 1) {
					projectConstraint.append("Project = ? OR ");
				} else {
					projectConstraint.append("Project = ?");
				}
			}

			String noProjectConstraint = "SELECT l.Name FROM License l WHERE (SELECT COUNT(*) FROM License_Project lp WHERE lp.License = l.Name) = 0";
			StringBuilder query = new StringBuilder(
					"SELECT * FROM License WHERE Deactivated = "
							+ deactivated
							+ " AND Category = ? AND UPPER(Name) LIKE UPPER(?) AND ROWNUM <="
							+ limit + " AND (Name IN (" + noProjectConstraint
							+ ")");
			if (projects.isEmpty()) {
				query.append(")");
			} else {
				query.append(" OR Name IN (" + projectConstraint + "))");
			}
			preparedStatement = connection.prepareStatement(query.toString());

			if (projects.isEmpty()) {
				preparedStatement.setString(1, category);
				preparedStatement.setString(2, partialName + "%");
			} else {
				preparedStatement.setString(1, category);
				preparedStatement.setString(2, partialName);
				for (int i = 1; i <= projects.size(); i++) {
					preparedStatement.setString(i + 2, projects.get(i - 1)
							.getName());
				}

			}

			preparedStatement.setString(1, category);
			preparedStatement.setString(2, partialName + "%");

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String parties = resultSet.getString("Parties");
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
						expiration, deactivated, groupEnabled,
						(groupName == null) ? "" : groupName.trim());
				resultList.add(license);
			}
		} catch (Exception ex) {
			LOG.error(
					"LicenseProjectDBHelper getLicensesByCategoryWithPartialNameAndLimitAndProjectConstraints(): "
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
						"LicenseProjectDBHelper getLicensesByCategoryWithPartialNameAndLimitAndProjectConstraints(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}
}
