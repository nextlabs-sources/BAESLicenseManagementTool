package com.nextlabs.bae.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LicenseDBHelper implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(LicenseDBHelper.class);

	/**
	 * Get all licenses from database
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of license
	 */
	public static List<License> getAllLicense() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();
		try {
			connection = DBHelper.getDatabaseConnection();
			String queryString = "SELECT * FROM License";

			preparedStatement = connection.prepareStatement(queryString);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				String name = resultSet.getString("Name");
				String parties = resultSet.getString("Parties");
				String category = resultSet.getString("Category");
				String label = resultSet.getString("Label");
				Timestamp effectiveDate = resultSet
						.getTimestamp("EffectiveDate");
				Timestamp expiration = resultSet.getTimestamp("Expiration");
				int deactivated = resultSet.getInt("Deactivated");
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
			LOG.error("LicenseDBHelper getAllLicense(): " + ex.getMessage(), ex);
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
						"LicenseDBHelper getAllLicense(): " + ex.getMessage(),
						ex);
			}
		}
		return resultList;
	}

	/**
	 * Get all authorized-to-manage licenses
	 * 
	 * @param controlledAttributes
	 *            Authorized-to-manage attributes
	 * 
	 * @param deactivated
	 *            Deactivated status
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of licenses
	 */
	public static List<License> getAllLicensesByRights(
			List<String> controlledAttributes, int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();
		if (controlledAttributes == null || controlledAttributes.isEmpty()) {
			return null;
		}
		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT * FROM License WHERE Deactivated = " + deactivated
							+ " AND ");

			for (int i = 0; i < controlledAttributes.size() - 1; i++) {
				queryString.append("Category = ? OR ");
			}

			queryString.append("Category = ?");

			preparedStatement = connection.prepareStatement(queryString
					.toString());

			for (int i = 0; i < controlledAttributes.size() - 1; i++) {
				preparedStatement.setString(i + 1, controlledAttributes.get(i));
			}

			preparedStatement.setString(controlledAttributes.size(),
					controlledAttributes.get(controlledAttributes.size() - 1));
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
			LOG.error(
					"LicenseDBHelper getAllLicensesByRights(): "
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
						"LicenseDBHelper getAllLicensesByRights(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Get top n licenses by category (data set) that start with some characters
	 * 
	 * @param category
	 *            License category
	 * 
	 * @param partialName
	 *            Characters for querying
	 * 
	 * @param limit
	 *            Maximum size of returned list
	 * 
	 * @param deactivated
	 *            Deactivated status
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of licenses
	 */
	public static List<License> getLicensesByCategoryWithPartialNameAndLimit(
			String category, String partialName, int limit, int deactivated) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<License> resultList = new ArrayList<License>();

		try {
			connection = DBHelper.getDatabaseConnection();
			preparedStatement = connection
					.prepareStatement("SELECT * FROM License WHERE Deactivated = "
							+ deactivated
							+ " AND Category = ? AND UPPER(Name) LIKE UPPER(?) AND ROWNUM <="
							+ limit);
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
					"LicenseDBHelper getLicensesByCategoryWithPartialNameAndLimit(): "
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
						"LicenseDBHelper getLicensesByCategoryWithPartialNameAndLimit(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Return licenses used for lazy loading
	 * 
	 * @param start
	 *            Start index of the page
	 * 
	 * @param size
	 *            Page size
	 * 
	 * @param sortField
	 *            Field used to sort
	 * 
	 * @param sortOrder
	 *            Sort order
	 * 
	 * @param filters
	 *            Filter to be used for querying
	 * 
	 * @param deactivated
	 *            Deactivated status
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of licenses
	 */
	public static List<License> getLicensesListLazy(int start, int size,
			String sortField, String sortOrder, Map<String, Object> filters,
			int deactivated) {
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
			LOG.error(
					"LicenseDBHelper getLicenseListLazy(): " + ex.getMessage(),
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
						"LicenseDBHelper getLicenseListLazy(): "
								+ ex.getMessage(), ex);
			}
		}
		return resultList;
	}

	/**
	 * Count licenses based on filters - used for lazy loading
	 * 
	 * @param filters
	 *            Map of filters
	 * 
	 * @param deactivated
	 *            Deactivated status
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return Number of filtered licenses
	 */
	public static int countAllLicense(Map<String, Object> filters,
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
				queryString.append(" WHERE Deactivated = " + deactivated
						+ " AND " + whereClause);
			} else {
				queryString.append(" WHERE Deactivated = " + deactivated + " ");
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
			LOG.error("LicenseDBHelper countAllLicense(): " + ex.getMessage(),
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
						"LicenseDBHelper countAllLicense(): " + ex.getMessage(),
						ex);
			}
		}
		return resultCount;
	}

	/**
	 * Get a license based on license name
	 * 
	 * @param name
	 *            License name
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return The license
	 */
	public static License getLicense(String name) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		License license = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("SELECT * FROM License WHERE Name = ?");
			statement.setString(1, name);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				String parties = resultSet.getString("Parties");
				String category = resultSet.getString("Category");
				String label = resultSet.getString("Label");
				Timestamp effectiveDate = resultSet
						.getTimestamp("EffectiveDate");
				Timestamp expiration = resultSet.getTimestamp("Expiration");
				int deactivated = resultSet.getInt("Deactivated");
				int groupEnabled = resultSet.getInt("GroupEnabled");
				String groupName = resultSet.getString("GroupName");
				license = new License(name.trim(), (parties == null) ? ""
						: parties.trim(), (category == null) ? ""
						: category.trim(), (label == null) ? "" : label.trim(),
						effectiveDate, expiration, deactivated, groupEnabled,
						(groupName == null) ? "" : groupName.trim());
			}
		} catch (Exception ex) {
			LOG.error("LicenseDBHelper getLicense(): " + ex.getMessage(), ex);
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
				LOG.error("LicenseDBHelper getLicense(): " + ex.getMessage(),
						ex);
			}
		}
		return license;
	}

	/**
	 * Delete a license
	 * 
	 * @param license
	 *            License name
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteLicense(String license) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("DELETE FROM License WHERE Name = ?");
			statement.setString(1, license);
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseDBHelper deleteLicense(): " + ex.getMessage());
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
						"LicenseDBHelper deleteLicense(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;

	}

	/**
	 * Create a new license
	 * 
	 * @param name
	 *            License name
	 * @param parties
	 *            License parties
	 * @param category
	 *            License category
	 * @param label
	 *            Category label
	 * @param effectiveDate
	 *            License effective date
	 * @param expiration
	 *            License expiring date
	 * @param groupEnabled
	 *            Enable license group or not
	 * @param groupName
	 *            Name of the license group
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean createLicense(String name, String parties,
			String category, String label, Timestamp effectiveDate,
			Timestamp expiration, int groupEnabled, String groupName) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("INSERT INTO License (Name, Parties, Category, Label, EffectiveDate, Expiration, GroupEnabled, GroupName)"
							+ " VALUES (?, ?, ?, ?, ? ,?, ?, ?)");
			statement.setString(1, name);
			statement.setString(2, parties);
			statement.setString(3, category);
			statement.setString(4, label);
			statement.setTimestamp(5, effectiveDate);
			statement.setTimestamp(6, expiration);
			statement.setInt(7, groupEnabled);
			statement.setString(8, groupName);
			// LOG.info(queryString);
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseDBHelper createLicense(): " + ex.getMessage(), ex);
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
						"LicenseDBHelper createLicense(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update a license in database - select by license name
	 * 
	 * @param name
	 *            License name
	 * @param parties
	 *            License parties
	 * @param effectiveDate
	 *            License effective date
	 * @param expiration
	 *            License expiring date
	 * @param groupEnabled
	 *            Enable license group or not
	 * @param groupName
	 *            name of the license group
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean updateLicense(String name, String parties,
			Timestamp effectiveDate, Timestamp expiration, int groupEnabled,
			String groupName) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("UPDATE License SET Parties = ?, EffectiveDate = ?, Expiration = ?, GroupEnabled = ?, GroupName = ? "
							+ "WHERE Name = ?");

			statement.setString(1, parties);
			statement.setTimestamp(2, effectiveDate);
			statement.setTimestamp(3, expiration);
			statement.setInt(4, groupEnabled);
			statement.setString(5, groupName);
			statement.setString(6, name);

			// LOG.info(queryString);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LicenseDBHelper updateLicense(): " + ex.getMessage(), ex);
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
						"LicenseDBHelper updateLicense(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update a license activation
	 * 
	 * @param name
	 *            License name
	 * @param deactivated
	 *            Deactivated status
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean toggleActivationLicense(String name, int deactivated) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("UPDATE License SET Deactivated = "
							+ deactivated + " WHERE Name = ?");

			statement.setString(1, name);

			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error(
					"LicenseDBHelper toggleActivationLicense(): "
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
						"LicenseDBHelper toggleActivationLicense(): "
								+ ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Get all license table's columns
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return A list containing column names
	 */
	public static List<String> getColumnsName() {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		List<String> resultList = new ArrayList<String>();

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection.createStatement();
			resultSet = statement
					.executeQuery("SELECT * FROM License WHERE ROWNUM <= 1");
			ResultSetMetaData rsmd = resultSet.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				resultList.add(rsmd.getColumnName(i));
			}
			resultList.remove("Name");

		} catch (Exception ex) {
			LOG.error("LicenseDBHelper getColumnsName(): " + ex.getMessage(),
					ex);
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
						"LicenseDBHelper getColumnsName(): " + ex.getMessage(),
						ex);
			}
		}
		return resultList;
	}

	/**
	 * Export licenses in database into an InputStream (not used currently)
	 * 
	 * @param columns
	 *            Select-to-export columns
	 * @param fileType
	 *            csv or sql
	 * @exception Exception
	 *                Any exception
	 * @return The stream
	 */
	public static InputStream exportLicense(List<String> columns,
			String fileType) {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		InputStream in = null;
		columns.add("Name");

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT * FROM License");

			// Exporting a list of upsert queries
			StringBuilder exportStatement = new StringBuilder();

			while (resultSet.next()) {
				if (fileType.equals("sql")) {
					StringBuilder insertStatement = new StringBuilder();
					StringBuilder updateStatement = new StringBuilder();
					insertStatement.append("INSERT INTO License (");
					updateStatement.append("UPDATE License SET ");

					for (String col : columns) {
						insertStatement.append("\"" + col + "\",");
						// build update statement values
						if (!col.equals("Name")) {
							String value = resultSet.getString(col);
							updateStatement.append(""
									+ col
									+ ((value == null) ? " = null," : " = '"
											+ value.trim() + "',"));
						}
					}
					insertStatement = new StringBuilder(
							insertStatement.substring(0,
									insertStatement.length() - 1));
					updateStatement = new StringBuilder(
							updateStatement.substring(0,
									updateStatement.length() - 1));

					insertStatement.append(") SELECT ");
					for (String col : columns) {
						String value = resultSet.getString(col);
						insertStatement.append(((value == null) ? "null," : "'"
								+ value.trim() + "',"));
					}
					insertStatement.append(insertStatement.substring(0,
							insertStatement.length() - 1));

					updateStatement.append(" WHERE Name = '"
							+ resultSet.getString("Name").trim() + "' ");

					// a complete one-row upsert statement
					exportStatement.append("WITH upsert AS (" + updateStatement
							+ " RETURNING *) " + insertStatement
							+ " WHERE NOT EXISTS (SELECT * FROM upsert);\n");
				} else {
					for (String col : columns) {
						String value = resultSet.getString(col);
						exportStatement.append(""
								+ ((value == null) ? "" : resultSet.getString(
										col).trim()) + ",");
					}
					exportStatement.append(exportStatement.substring(0,
							exportStatement.length() - 1));
					exportStatement.append("\n");
				}
			}

			if (fileType.equals("csv")) {
				StringBuilder header = new StringBuilder();
				for (String col : columns) {
					header.append(col + ",");
				}
				header = new StringBuilder(header.substring(0,
						header.length() - 1));
				header.append("\n");
				exportStatement = header.append(exportStatement);
			} else {
				// lock table to prevent race condition
				exportStatement = new StringBuilder(
						"BEGIN;\nLOCK TABLE License IN SHARE ROW EXCLUSIVE MODE;\n"
								+ exportStatement + "COMMIT;");
			}
			LOG.info("LicenseDBHelper exportLicense(): exportStatement: "
					+ exportStatement);

			in = new ByteArrayInputStream(exportStatement.toString().getBytes());

		} catch (Exception ex) {
			LOG.error("LicenseDBHelper exportLicense(): " + ex.getMessage(), ex);
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
						"LicenseDBHelper exportLicense(): " + ex.getMessage(),
						ex);
			}
		}
		return in;
	}

	/**
	 * Import license from a string - not currently used
	 * 
	 * @param fileType
	 *            csv or sql
	 * @param content
	 *            String content
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean importLicense(String fileType, String content) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			String[] rows = content.split("\n");
			int keyPosition = 0;
			StringBuilder importStatement = new StringBuilder();
			if (fileType.equals("csv")) {
				String[] headers = rows[0].split(",");
				for (int i = 0; i < headers.length; i++) {
					if (headers[i].trim().equals("Name")) {
						keyPosition = i;
					}
				}

				for (int rowIndex = 1; rowIndex < rows.length; rowIndex++) {
					String[] rowValues = rows[rowIndex].split(",");
					StringBuilder insertStatement = new StringBuilder();
					StringBuilder updateStatement = new StringBuilder();
					insertStatement.append("INSERT INTO License (");
					updateStatement.append("UPDATE License SET ");

					for (int i = 0; i < headers.length; i++) {
						String col = headers[i];
						if (!rowValues[i].trim().equals("")) {
							insertStatement.append("" + col.trim() + ",");
							// build update statement values
							if (!col.trim().equals("Name")) {
								String value = rowValues[i];
								updateStatement.append("" + col.trim() + " = '"
										+ ((value == null) ? "" : value.trim())
										+ "',");
							}
						}
					}
					insertStatement = new StringBuilder(
							insertStatement.substring(0,
									insertStatement.length() - 1));
					updateStatement = new StringBuilder(
							updateStatement.substring(0,
									updateStatement.length() - 1));

					insertStatement.append(") SELECT ");
					for (int i = 0; i < headers.length; i++) {
						String value = rowValues[i];
						if (!value.trim().equals("")) {
							insertStatement.append("'"
									+ ((value == null) ? "" : value.trim())
									+ "',");
						}
					}
					insertStatement.append(insertStatement.substring(0,
							insertStatement.length() - 1));
					updateStatement.append(" WHERE Name = '"
							+ rowValues[keyPosition].trim() + "' ");

					// a complete one-row upsert statement
					importStatement.append("WITH upsert AS (" + updateStatement
							+ " RETURNING *) " + insertStatement
							+ " WHERE NOT EXISTS (SELECT * FROM upsert);\n");
				}

				importStatement = new StringBuilder(
						"BEGIN;\nLOCK TABLE License IN SHARE ROW EXCLUSIVE MODE;\n"
								+ importStatement + "COMMIT;");
			} else {
				importStatement = new StringBuilder(content);
			}
			statement = connection.prepareStatement(importStatement.toString());

			LOG.info("LicenseDBHelper importLicense(): importStatement: "
					+ importStatement);
			statement.execute();
		} catch (Exception ex) {
			LOG.error("LicenseDBHelper importLicense(): " + ex.getMessage(), ex);
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
						"LicenseDBHelper importLicense(): " + ex.getMessage(),
						ex);
				return false;
			}
		}
		return true;
	}

}
