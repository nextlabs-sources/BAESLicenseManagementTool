package com.nextlabs.bae.task;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.annotations.Expose;
import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.TaskDBHelper;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserProjectDBHelper;

public class AssignProjectsToUsersTask extends AsyncTask implements Runnable {
	@Expose
	private List<User> users;
	@Expose
	private List<Project> projectsToAdd;
	@Expose
	private List<Project> projectsToRemove;

	private static final Log LOG = LogFactory
			.getLog(AssignProjectsToUsersTask.class);

	public AssignProjectsToUsersTask() {
	};

	public AssignProjectsToUsersTask(List<User> users,
			List<Project> projectsToAdd, List<Project> projectsToRemove,
			String admin, String password, String trigger) {
		this.users = users;
		this.projectsToAdd = projectsToAdd;
		this.projectsToRemove = projectsToRemove;
		this.admin = admin;
		this.password = password;
		this.timeStart = new Timestamp(new Date().getTime());
		this.taskName = timeStart
				+ " : <strong>"
				+ trigger
				+ "</strong> : <strong>"
				+ getAdminUser().getDisplayName()
				+ "</strong>"
				+ " updated projects of <strong>"
				+ users.size()
				+ "</strong> user(s) on <strong>"
				+ (((projectsToAdd == null) ? 0 : projectsToAdd.size()) + ((projectsToRemove == null) ? 0
						: projectsToRemove.size())) + "</strong> project(s)";
		this.progress = 0;
		this.trigger = trigger;
		this.error = false;
		this.message = "";
		this.totalProgress = users.size()
				* ((((projectsToAdd == null) ? 0 : projectsToAdd.size()) + ((projectsToRemove == null) ? 0
						: projectsToRemove.size())));
		this.tempProgress = 0;
		this.numberOfUsers = users.size();
		this.numberOfObjects = (((projectsToAdd == null) ? 0 : projectsToAdd
				.size()) + ((projectsToRemove == null) ? 0 : projectsToRemove
				.size()));
		this.numberOfSuccess = 0;
		this.numberOfFailure = 0;
	}

	@Override
	public void run() {
		try {
			LOG.info("AssignProjectsToUsersTask run(): Number of users: "
					+ users.size());

			if (users.size() == 0
					|| (((projectsToAdd == null) ? 0 : projectsToAdd.size()) + ((projectsToRemove == null) ? 0
							: projectsToRemove.size())) == 0) {
				LOG.info("AssignProjectsToUsersTask run(): Nothing to update.");
				progress = 100;
				message = "<p>Nothing to update</p>";
				TaskDBHelper.updateTask(getTimeStart(), getAdminUser()
						.getAduser(), "group", progress, tempProgress,
						(error) ? 1 : 0, message, numberOfSuccess,
						numberOfFailure);
				return;
			}

			int temp = 0;
			StringBuilder messageTemp = new StringBuilder();

			List<User> successfulUsers = new ArrayList<User>();
			List<Project> validProjectsToAdd = new ArrayList<Project>();
			List<Project> validProjectsToRemove = new ArrayList<Project>();

			if (projectsToAdd != null) {
				for (Project project : projectsToAdd) {
					if (ProjectDBHelper.getProject(project.getName()) != null) {
						validProjectsToAdd.add(project);
					}
				}
			}

			if (projectsToRemove != null) {
				for (Project project : projectsToRemove) {
					if (ProjectDBHelper.getProject(project.getName()) != null) {
						validProjectsToRemove.add(project);
					}
				}
			}

			for (User user : users) {

				if (temp < tempProgress) {
					message = "Restoring progress ... ";
					temp++;
					continue;
				}

				List<ModificationItem> userMods = new ArrayList<ModificationItem>();
				String filter = "sAMAccountName=" + user.getAduser();
				user = ActiveDirectoryHelper.getUser(filter, null);

				LOG.info("Updating user " + user.getAduser());

				if (projectsToAdd != null) {
					LOG.info("AssignProjectsToUsersTask run(): Number of projects to add: "
							+ projectsToAdd.size());
					for (Project project : projectsToAdd) {
						userMods.add(new ModificationItem(
								DirContext.ADD_ATTRIBUTE, new BasicAttribute(
										PropertyLoader.bAESProperties
												.getProperty("pa"), project
												.getName())));
					}
				}

				if (projectsToRemove != null) {
					LOG.info("AssignProjectsToUsersTask run(): Number of projects to remove: "
							+ projectsToRemove.size());
					for (Project project : projectsToRemove) {
						userMods.add(new ModificationItem(
								DirContext.REMOVE_ATTRIBUTE,
								new BasicAttribute(
										PropertyLoader.bAESProperties
												.getProperty("pa"), project
												.getName())));
					}
				}
				String result = ActiveDirectoryHelper.modifyUserAttribute(
						userMods.toArray(new ModificationItem[0]), filter,
						getAdminUser().getDisplayName(), trigger);
				if (!result.equals("OK")) {
					messageTemp
							.append("<p class = \"red-text\">Error when updating <strong>"
									+ user.getAduser()
									+ "</strong>'s projects on Active Directory: "
									+ result + "</p>");
					LOG.info("AssignProjectsToUsersTask run(): Failed to update user project on AD: "
							+ user.getAduser());
					error = true;
					numberOfFailure++;
				} else {
					messageTemp.append("<p>Successfully updated <strong>"
							+ user.getAduser()
							+ "</strong>'s projects on Active Directory</p>");
					numberOfSuccess++;
					successfulUsers.add(user);
				}

				tempProgress++;
				temp++;
				progress = (tempProgress * 100) / totalProgress;
			}

			if (projectsToAdd != null) {
				UserProjectDBHelper.assignProjectsToUsers(validProjectsToAdd,
						successfulUsers);

			}
			if (projectsToRemove != null) {
				UserProjectDBHelper.removeProjectsFromUsers(
						validProjectsToRemove, successfulUsers);
			}

			progress = 100;
			message = messageTemp.toString();
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"project", progress, tempProgress, (error) ? 1 : 0,
					message, numberOfSuccess, numberOfFailure);

		} catch (Exception e) {
			LOG.error("AssignProjectsToUsersTask run(): " + e.getMessage(), e);
			message = "<p class = \"red-text\">Task encountered a problem: "
					+ ((e.getMessage() == null) ? "Unknown reason" : e
							.getMessage())
					+ ": Please contact administrator.</p>";
			progress = 101;
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"project", progress, tempProgress, 1, message,
					numberOfSuccess, numberOfFailure);
		}
	}
}
