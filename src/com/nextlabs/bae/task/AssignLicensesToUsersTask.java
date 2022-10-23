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
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.TaskDBHelper;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;

public class AssignLicensesToUsersTask extends AsyncTask implements Runnable {
	@Expose
	private List<User> users;
	@Expose
	private List<License> licensesToAdd;
	@Expose
	private List<License> licensesToRemove;

	private static final Log LOG = LogFactory
			.getLog(AssignLicensesToUsersTask.class);

	public AssignLicensesToUsersTask() {
	};

	public AssignLicensesToUsersTask(List<User> users,
			List<License> licensesToAdd, List<License> licensesToRemove,
			String admin, String password, String trigger) {
		this.users = users;
		this.licensesToAdd = licensesToAdd;
		this.licensesToRemove = licensesToRemove;
		this.admin = admin;
		this.password = password;
		this.timeStart = new Timestamp(new Date().getTime());
		this.taskName = timeStart
				+ " : <strong>"
				+ trigger
				+ "</strong> : <strong>"
				+ getAdminUser().getDisplayName()
				+ "</strong>"
				+ " updated licenses of <strong>"
				+ users.size()
				+ "</strong> user(s) on <strong>"
				+ (((licensesToAdd == null) ? 0 : licensesToAdd.size()) + ((licensesToRemove == null) ? 0
						: licensesToRemove.size())) + "</strong> license(s)";
		this.progress = 0;
		this.trigger = trigger;
		this.error = false;
		this.message = "";
		this.totalProgress = users.size()
				* ((((licensesToAdd == null) ? 0 : licensesToAdd.size()) + ((licensesToRemove == null) ? 0
						: licensesToRemove.size())));
		this.tempProgress = 0;
		this.numberOfUsers = users.size();
		this.numberOfObjects = (((licensesToAdd == null) ? 0 : licensesToAdd
				.size()) + ((licensesToRemove == null) ? 0 : licensesToRemove
				.size()));
		this.numberOfSuccess = 0;
		this.numberOfFailure = 0;
	}

	@Override
	public void run() {
		try {
			StringBuilder messageTemp = new StringBuilder();
			LOG.info("AssignLicensesToUsersTask run(): Number of users: "
					+ users.size());

			if (users.size() == 0
					|| (((licensesToAdd == null) ? 0 : licensesToAdd.size()) + ((licensesToRemove == null) ? 0
							: licensesToRemove.size())) == 0) {
				LOG.info("AssignLicensesToUsersTask run(): Nothing to update.");
				progress = 100;
				message = "<p>Nothing to update</p>";
				TaskDBHelper.updateTask(getTimeStart(), getAdminUser()
						.getAduser(), "group", progress, tempProgress,
						(error) ? 1 : 0, message, numberOfSuccess,
						numberOfFailure);

				return;
			}

			int temp = 0;

			List<User> successfulUsers = new ArrayList<User>();
			List<License> validLicensesToAdd = new ArrayList<License>();
			List<License> validLicensesToRemove = new ArrayList<License>();

			if (licensesToAdd != null) {
				for (License license : licensesToAdd) {
					if (license.getLabel() == null) {
						LOG.debug("AssignLicensesToUsersTask run() License "
								+ license.getName() + " is invalid");
					} else {
						validLicensesToAdd.add(license);
					}
				}
			}

			if (licensesToRemove != null) {
				for (License license : licensesToRemove) {
					if (license.getLabel() == null) {
						LOG.debug("AssignLicensesToUsersTask run() License "
								+ license.getName() + " is invalid");
					} else {
						validLicensesToRemove.add(license);
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

				LOG.info("AssignLicensesToUsersTask run(): Updating user "
						+ user.getAduser());

				if (licensesToAdd != null) {
					LOG.info("AssignLicensesToUsersTask run(): Number of licenses to add: "
							+ licensesToAdd.size());
					for (License license : licensesToAdd) {
						userMods.add(new ModificationItem(
								DirContext.ADD_ATTRIBUTE, new BasicAttribute(
										license.getCategory(), license
												.getName())));
					}
				}

				if (licensesToRemove != null) {
					LOG.info("AssignLicensesToUsersTask run(): Number of licenses to remove: "
							+ licensesToRemove.size());
					for (License license : licensesToRemove) {
						userMods.add(new ModificationItem(
								DirContext.REMOVE_ATTRIBUTE,
								new BasicAttribute(license.getCategory(),
										license.getName())));
					}
				}
				String result = ActiveDirectoryHelper.modifyUserAttribute(
						userMods.toArray(new ModificationItem[0]), filter,
						getAdminUser().getDisplayName(), trigger);
				if (!result.equals("OK")) {
					messageTemp
							.append("<p class = \"red-text\">Error when updating <strong>"
									+ user.getAduser()
									+ "</strong>'s licenses on Active Directory: "
									+ result + "</p>");
					LOG.info("AssignLicensesToUsersTask run(): Failed to update user license on AD: "
							+ user.getAduser());
					error = true;
					numberOfFailure++;
				} else {
					messageTemp.append("<p>Successfully updated <strong>"
							+ user.getAduser()
							+ "</strong>'s licenses on Active Directory</p>");
					numberOfSuccess++;
					successfulUsers.add(user);
				}

				tempProgress++;
				temp++;
				progress = (tempProgress * 100) / totalProgress;
			}

			if (licensesToAdd != null) {
				UserLicenseDBHelper.assignLicensesToUsers(validLicensesToAdd,
						successfulUsers);
			}
			if (licensesToRemove != null) {
				UserLicenseDBHelper.removeLicensesFromUsers(
						validLicensesToRemove, successfulUsers);
			}

			progress = 100;
			message = messageTemp.toString();
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"license", progress, tempProgress, (error) ? 1 : 0,
					message, numberOfSuccess, numberOfFailure);

		} catch (Exception e) {
			LOG.error("AssignLicensesToUsersTask run(): " + e.getMessage(), e);
			message = "<p class = \"red-text\">Task encountered a problem: "
					+ ((e.getMessage() == null) ? "Unknown reason" : e
							.getMessage())
					+ ": Please contact administrator.</p>";
			progress = 101;
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"license", progress, tempProgress, 1, message,
					numberOfSuccess, numberOfFailure);
		}
	}
}
