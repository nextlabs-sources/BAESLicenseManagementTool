package com.nextlabs.bae.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class LogDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(LogDBHelper.class);

	/**
	 * Get all logs from db
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return List of logs
	 */
	public static List<BAELog> getAllLog() {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		List<BAELog> returnLogs = new ArrayList<BAELog>();
		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT * FROM Log;");
			while (resultSet.next()) {
				Timestamp time = resultSet.getTimestamp("Time");
				String admin = resultSet.getString("Admin");
				String targetUser = resultSet.getString("TargetUser");
				String action = resultSet.getString("Action");
				String attribute = resultSet.getString("Attribute");
				String oldValue = resultSet.getString("OldValue");
				String newValue = resultSet.getString("Value");
				String trigger = resultSet.getString("Trig");
				BAELog newLog = new BAELog(time, admin, targetUser, action,
						attribute, oldValue, newValue, trigger);
				returnLogs.add(newLog);
			}
		} catch (Exception ex) {
			LOG.error("LogDBHelper getAllLog(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper getAllLog()" + ex.getMessage(), ex);
			}
		}
		return returnLogs;
	}

	/**
	 * Create a log entry in db
	 * 
	 * @param logItem
	 *            The entry
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean createLog(BAELog logItem) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("INSERT INTO Log (Time, Admin, TargetUser, Action, Attribute, OldValue, Value, Trig)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setTimestamp(1, logItem.getTime());
			statement.setString(2, logItem.getAdmin());
			statement.setString(3, logItem.getTargetUser());
			statement.setString(4, logItem.getAction());
			statement.setString(5, logItem.getAttribute());
			statement.setString(6, logItem.getOldValue());
			statement.setString(7, logItem.getNewValue());
			statement.setString(8, logItem.getTrigger());
			statement.execute();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LogDBHelper createLog(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper createLog(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Create multiple log entries
	 * 
	 * @param logItems
	 *            log items
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean createLogs(List<BAELog> logItems) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			if (logItems == null) {
				LOG.info("LogDBHelper createLogs(): There is no log");
				return true;
			}

			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("INSERT INTO Log (Id, Time, Admin, TargetUser, Action, Attribute, OldValue, Value, Trig)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

			connection.setAutoCommit(false);

			for (int i = 0; i < logItems.size(); i++) {
				BAELog logItem = logItems.get(i);
				statement.setString(1, UUID.randomUUID().toString());
				statement.setTimestamp(2, logItem.getTime());
				statement.setString(3, logItem.getAdmin());
				statement.setString(4, logItem.getTargetUser());
				statement.setString(5, logItem.getAction());
				statement.setString(6, logItem.getAttribute());
				statement.setString(7, logItem.getOldValue());
				statement.setString(8, logItem.getNewValue());
				statement.setString(9, logItem.getTrigger());
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LogDBHelper createLogs(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper createLogs(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Clear all log in db
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean clearLog() {
		Connection connection = null;
		Statement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection.createStatement();
			statement.executeQuery("DELETE from Log");
			connection.commit();
		} catch (Exception ex) {
			LOG.error("LogDBHelper clearLog(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper clearLog(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Get all logs for lazy loading
	 * 
	 * @param start
	 *            Page start
	 * @param size
	 *            Page size
	 * @param sortField
	 *            Field used to sort
	 * @param sortOrder
	 *            Order of sorting
	 * @param filters
	 *            Filters for querying
	 * @exception Exception
	 *                Any exception
	 * @return List of log
	 */
	public static List<BAELog> getLogListLazy(int start, int size,
			String sortField, String sortOrder, Map<String, Object> filters) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<BAELog> resultList = new ArrayList<BAELog>();
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT outer.* FROM (SELECT ROWNUM rn, inner.* FROM (SELECT * FROM Log");
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
				queryString.append(" WHERE " + whereClause);
			} else {
				queryString.append(" ");
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
					String key = Character.toUpperCase(filter.getKey()
							.charAt(0)) + filter.getKey().substring(1);
					String value = filter.getValue().toString() + "%";
					preparedStatement.setString(parameterIndex.get(key), value);
				}

			}

			// LOG.info("Lazy query: " + preparedStatement.toString());

			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				Timestamp time = resultSet.getTimestamp("Time");
				String admin = resultSet.getString("Admin");
				String targetUser = resultSet.getString("TargetUser");
				String action = resultSet.getString("Action");
				String attribute = resultSet.getString("Attribute");
				String oldValue = resultSet.getString("OldValue");
				String newValue = resultSet.getString("Value");
				String trigger = resultSet.getString("Trig");
				BAELog newLog = new BAELog(time, admin, targetUser, action,
						attribute, oldValue, newValue, trigger);
				resultList.add(newLog);
			}
		} catch (Exception ex) {
			LOG.error("LogDBHelper getLogListLazy(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper getLogListLazy(): " + ex.getMessage(),
						ex);
			}
		}
		return resultList;
	}

	/**
	 * Count all logs for lazy loading
	 * 
	 * @param filters
	 *            Filters used for querying
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static int countAllLog(Map<String, Object> filters) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int resultCount = 0;
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();
			StringBuilder queryString = new StringBuilder(
					"SELECT COUNT(*) FROM Log");
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
				queryString.append(" WHERE " + whereClause);
			} else {
				queryString.append(" ");
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
			LOG.error("LogDBHelper countAllLog(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper countAllLog(): " + ex.getMessage(), ex);
			}
		}
		return resultCount;
	}

	/**
	 * Get all column names in log table
	 * 
	 * @exception Exception
	 *                Any exception
	 * @return A list containing all column names
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
					.executeQuery("SELECT * FROM Log WHERE ROWNUM <= 1");
			ResultSetMetaData rsmd = resultSet.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				resultList.add(rsmd.getColumnName(i));
			}

			resultList.remove("OLDVALUE");

		} catch (Exception ex) {
			LOG.error("LogDBHelper getColumnsName(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper getColumnsName(): " + ex.getMessage(),
						ex);
			}
		}
		return resultList;
	}

	/**
	 * Get an input stream of logs used to export
	 * 
	 * @param columns
	 *            Selected columns to export
	 * @param filters
	 *            Filters used for querying
	 * @param fileType
	 *            File type to export
	 * @exception Exception
	 *                Any exception
	 * @return the steam
	 */
	public static InputStream exportLog(List<String> columns,
			Map<String, Object> filters, String fileType) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		InputStream in = null;
		Map<String, Integer> parameterIndex = new HashMap<String, Integer>();

		try {
			connection = DBHelper.getDatabaseConnection();

			StringBuilder queryString = new StringBuilder("SELECT * FROM Log");
			StringBuilder whereClause = new StringBuilder();
			// concat all filters
			if (filters != null) {

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
					queryString.append(" WHERE " + whereClause);
				}
				
				queryString.append(" ORDER BY Time DESC");

				statement = connection.prepareStatement(queryString.toString());

				for (Map.Entry<String, Object> filter : filters.entrySet()) {
					if (!(filter.getValue().equals("") || filter.getValue() == null)) {
						String key = Character.toUpperCase(filter.getKey()
								.charAt(0)) + filter.getKey().substring(1);
						String value = filter.getValue().toString() + "%";
						statement.setString(parameterIndex.get(key), value);
					}

				}
			} else {
				queryString.append(" ORDER BY Time DESC");
				statement = connection.prepareStatement(queryString.toString());
			}

			resultSet = statement.executeQuery();

			if (fileType.equals("csv")) {
				String exportStatement = "";

				while (resultSet.next()) {
					for (String col : columns) {
						String value = resultSet.getString(col);
						exportStatement += ""
								+ ((value == null) ? "" : resultSet.getString(
										col).trim()) + ",";
					}
					exportStatement = exportStatement.substring(0,
							exportStatement.length() - 1);
					exportStatement += "\n";
				}

				String header = "";
				for (String col : columns) {
					header += col + ",";
				}
				header = header.substring(0, header.length() - 1);
				header += "\n";
				exportStatement = header + exportStatement;

				in = new ByteArrayInputStream(exportStatement.getBytes());
			} else {
				Document pdf_data = new Document(PageSize.A4);
				Font titleFont = new Font(Font.FontFamily.HELVETICA, 14,
						Font.BOLD);
				Font normalFont = new Font(Font.FontFamily.HELVETICA, 7,
						Font.NORMAL);
				Font headerFont = new Font(Font.FontFamily.HELVETICA, 8,
						Font.NORMAL);
				headerFont.setColor(255, 255, 255);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfWriter.getInstance(pdf_data, baos);
				pdf_data.setMargins(2, 2, 2, 2);
				pdf_data.open();

				pdf_data.addTitle("License Management Tool Audit Logs");

				Paragraph para = new Paragraph(
						"License Management Tool Audit Logs", titleFont);
				para.setAlignment(Element.ALIGN_CENTER);
				pdf_data.add(para);
				pdf_data.add(new Paragraph(" "));

				PdfPTable table = new PdfPTable(columns.size());
				PdfPCell table_cell;

				for (String col : columns) {
					table_cell = new PdfPCell(new Phrase(col, headerFont));
					table_cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table_cell.setBackgroundColor(new BaseColor(20, 20, 20));
					table_cell.setPadding(3);
					table_cell.setBorderColor(new BaseColor(200, 200, 200));
					table.addCell(table_cell);
				}

				while (resultSet.next()) {
					for (String col : columns) {
						String value = resultSet.getString(col);
						table_cell = new PdfPCell(new Phrase(""
								+ ((value == null) ? "" : resultSet.getString(
										col).trim()), normalFont));
						table_cell.setPadding(3);
						table_cell.setBorderColor(new BaseColor(200, 200, 200));
						table.addCell(table_cell);
					}
				}
				pdf_data.add(table);
				pdf_data.close();

				in = new ByteArrayInputStream((baos.toByteArray()));
			}

		} catch (Exception ex) {
			LOG.error("LogDBHelper exportLog(): " + ex.getMessage(), ex);
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
				LOG.error("LogDBHelper exportLog(): " + ex.getMessage(), ex);
			}
		}
		return in;
	}
}
