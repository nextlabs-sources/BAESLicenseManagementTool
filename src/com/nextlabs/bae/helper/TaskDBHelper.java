package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextlabs.bae.task.AssignLicensesToUsersTask;
import com.nextlabs.bae.task.AssignProjectsToUsersTask;
import com.nextlabs.bae.task.AssignUsersToGroupTask;
import com.nextlabs.bae.task.AsyncTask;

public class TaskDBHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(TaskDBHelper.class);

	/**
	 * Create a new task
	 * 
	 * @param task
	 *            Task to be created
	 * @param taskType
	 *            Type of task
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean createTask(AsyncTask task, String taskType) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("INSERT INTO Task (timestart, admin, tasktype, totalprogress, tempprogress, progress, taskjson, error, message)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setTimestamp(1, task.getTimeStart());
			statement.setString(2, task.getAdminUser().getAduser());
			statement.setString(3, taskType);
			statement.setInt(4, task.getTotalProgress());
			statement.setInt(5, 0);
			statement.setInt(6, 0);
			statement.setString(7,
					new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
							.create().toJson(task));
			statement.setInt(8, 0);
			statement.setString(9, "");
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("TaskDBHelper createTask(): " + ex.getMessage(), ex);
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
				LOG.error("TaskDBHelper createTask(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Update task information in database
	 * 
	 * @param timestart
	 *            Task started time
	 * @param admin
	 *            Admin who performed the task
	 * @param tasktype
	 *            Type of task
	 * @param progress
	 *            Percentage of completeness
	 * @param tempprogress
	 *            Temporary percentage of completeness
	 * @param error
	 *            Task has error or not
	 * @param message
	 *            Message
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean updateTask(Timestamp timestart, String admin,
			String tasktype, int progress, int tempprogress, int error,
			String message, int successCount, int failCount) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("UPDATE Task SET progress = ?, tempprogress = ?, message = ?, error = ?, successcount =?, failcount = ? WHERE timestart = ? AND tasktype = ? AND admin = ?");
			statement.setInt(1, progress);
			statement.setInt(2, tempprogress);
			statement.setString(3, message);
			statement.setInt(4, error);
			statement.setInt(5, successCount);
			statement.setInt(6, failCount);
			statement.setTimestamp(7, timestart);
			statement.setString(8, tasktype);
			statement.setString(9, admin);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("TaskDBHelper updateTask(): " + ex.getMessage(), ex);
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
				LOG.error("TaskDBHelper updateTask(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Delete a task from database
	 * 
	 * @param timestart
	 *            Task started time
	 * @param admin
	 *            Admin who performed the task
	 * @param tasktype
	 *            Type of the task
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteTask(Timestamp timestart, String admin,
			String tasktype) {
		Connection connection = null;
		PreparedStatement statement = null;

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("DELETE FROM Task WHERE timestart = ? AND tasktype = ? AND admin = ?");
			statement.setTimestamp(1, timestart);
			statement.setString(2, tasktype);
			statement.setString(3, admin);
			statement.executeUpdate();
			connection.commit();
		} catch (Exception ex) {
			LOG.error("TaskDBHelper deleteTask(): " + ex.getMessage(), ex);
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
				LOG.error("TaskDBHelper deleteTask(): " + ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Get top ten recent completed tasks from database
	 * 
	 * @param taskType
	 *            Type of task
	 * @exception Exception
	 *                Any exception
	 * @return List of tasks
	 */
	public static List<AsyncTask> getRecentCompletedTask(String taskType) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		List<AsyncTask> tasks = new ArrayList<AsyncTask>();

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("SELECT * FROM (SELECT * FROM Task WHERE tasktype = ? AND progress >=100 ORDER BY timestart DESC) Tasks_temp WHERE rownum <= 10 ORDER BY rownum");
			statement.setString(1, taskType);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				AsyncTask task = null;
				if (taskType.equals("group")) {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignUsersToGroupTask.class);
				} else if (taskType.equals("license")) {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignLicensesToUsersTask.class);
				} else {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignProjectsToUsersTask.class);
				}

				if (task != null) {
					task.setTempProgress(resultSet.getInt("tempprogress"));
					task.setProgress(resultSet.getInt("progress"));
					task.setTimeStart(resultSet.getTimestamp("timestart"));
					task.setMessage(resultSet.getString("message"));
					task.setError((resultSet.getInt("error") == 0) ? false
							: true);
					task.setNumberOfSuccess(resultSet.getInt("successCount"));
					task.setNumberOfFailure(resultSet.getInt("failCount"));
					tasks.add(task);
				}
			}

			// clear old task from db
			if (tasks.size() > 0) {
				clearCompletedTaskFromDB(taskType, tasks.get(tasks.size() - 1)
						.getTimeStart());
			}

		} catch (Exception ex) {
			LOG.error(
					"TaskDBHelper getRecentCompletedTasks(): "
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
						"TaskDBHelper getRecentCompletedTasks(): "
								+ ex.getMessage(), ex);
			}
		}
		Collections.reverse(tasks);
		return tasks;
	}

	/**
	 * Get incompleted tasks from database
	 * 
	 * @param taskType
	 *            Type of task
	 * @exception Exception
	 *                Any exception
	 * @return List of tasks
	 */
	public static List<AsyncTask> getIncompletedTask(String taskType) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		List<AsyncTask> tasks = new ArrayList<AsyncTask>();

		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("SELECT * FROM Task WHERE progress < 100 AND tasktype = ? ORDER BY timestart");
			statement.setString(1, taskType);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				AsyncTask task = null;
				if (taskType.equals("group")) {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignUsersToGroupTask.class);
				} else if (taskType.equals("license")) {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignLicensesToUsersTask.class);
				} else {
					task = new Gson().fromJson(resultSet.getString("taskjson"),
							AssignProjectsToUsersTask.class);
				}

				if (task != null) {
					task.setTempProgress(resultSet.getInt("tempprogress"));
					task.setProgress(resultSet.getInt("progress"));
					task.setTimeStart(resultSet.getTimestamp("timestart"));
					task.setMessage(resultSet.getString("message"));
					task.setError((resultSet.getInt("error") == 0) ? false
							: true);
					tasks.add(task);
				}
			}

		} catch (Exception ex) {
			LOG.error("TaskDBHelper getIncompletedTask(): " + ex.getMessage(),
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
						"TaskDBHelper getIncompletedTask(): " + ex.getMessage(),
						ex);
			}
		}
		return tasks;
	}

	/**
	 * Clearing tasks from database, leaving only the most 10 recent tasks
	 * 
	 * @param taskType
	 *            Type of task
	 * @param timeStart
	 *            Task started time
	 * @exception Exception
	 *                Any exception
	 */
	public static void clearCompletedTaskFromDB(String taskType,
			Timestamp timeStart) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = DBHelper.getDatabaseConnection();
			statement = connection
					.prepareStatement("DELETE FROM Task WHERE tasktype = ? AND progress >=100 AND timestart < ?");
			statement.setString(1, taskType);
			statement.setTimestamp(2, timeStart);
			statement.executeUpdate();
			connection.commit();

		} catch (Exception ex) {
			LOG.error(
					"TaskDBHelper clearCompletedTaskFromDB(): "
							+ ex.getMessage(), ex);
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
						"TaskDBHelper clearCompletedTaskFromDB(): "
								+ ex.getMessage(), ex);
			}
		}
	}
}
