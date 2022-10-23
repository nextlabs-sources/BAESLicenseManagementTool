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

public class AssignUsersToGroupTask extends AsyncTask implements Runnable {
	@Expose
	private List<User> users;
	@Expose
	private List<License> licensesToAdd;
	@Expose
	private List<License> licensesToRemove;

	private static final Log LOG = LogFactory
			.getLog(AssignUsersToGroupTask.class);

	public AssignUsersToGroupTask() {
	};

	public AssignUsersToGroupTask(List<User> users,
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
				+ "</strong> updated membership of <strong>"
				+ users.size()
				+ "</strong> user(s) on <strong>"
				+ (((licensesToAdd == null) ? 0 : licensesToAdd.size()) + ((licensesToRemove == null) ? 0
						: licensesToRemove.size()))
				+ "</strong> license group(s)";
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
			int temp = 0;
			StringBuilder messageTemp = new StringBuilder();

			if (users.size() == 0
					|| (((licensesToAdd == null) ? 0 : licensesToAdd.size()) + ((licensesToRemove == null) ? 0
							: licensesToRemove.size())) == 0) {
				LOG.info("AssignUsersToGroupTask run(): Nothing to update.");
				progress = 100;
				message = "<p>Nothing to update</p>";
				TaskDBHelper.updateTask(getTimeStart(), getAdminUser()
						.getAduser(), "group", progress, tempProgress,
						(error) ? 1 : 0, message, numberOfSuccess,
						numberOfFailure);
				return;
			}

			LOG.info("Number of users: " + users.size());

			if (licensesToAdd != null) {
				LOG.info("AssignUsersToGroupTask run(): Number of license groups to add: "
						+ licensesToAdd.size());
				for (License license : licensesToAdd) {

					if (temp < tempProgress) {
						message = "Restoring progress ... ";
						temp++;
						continue;
					}

					if (license.getGroupEnabled() == 0) {
						messageTemp
								.append("<p>Skipped group update for license <strong>"
										+ license.getName()
										+ "</strong> as license group is disabled for this license.</p>");
						numberOfSuccess++;
						continue;
					}

					List<ModificationItem> groupMods = new ArrayList<ModificationItem>();
					String groupName = license.getGroupName();

					for (User user : users) {
						LOG.info("AssignUsersToGroupTask run(): Adding "
								+ user.getAduser() + " to group " + groupName);

						if (user.getAttributes() == null) {
							user = ActiveDirectoryHelper.getUser(
									"sAMAccountName=" + user.getAduser(), null);
						}

						groupMods
								.add(new ModificationItem(
										DirContext.ADD_ATTRIBUTE,
										new BasicAttribute(
												"member",
												user.getAttribute("distinguishedName"))));
					}

					if (ActiveDirectoryHelper.getAdGroup(groupName, null) == null) {
						messageTemp
								.append("<p class = \"yellow-text\">License group <strong>"
										+ groupName
										+ "</strong> had been deleted while the task was running. Update process for this group was skipped. </p>");
						numberOfSuccess++;
					} else {

						String result = ActiveDirectoryHelper
								.modifyGroupAttribute(groupMods
										.toArray(new ModificationItem[0]),
										groupName);
						if (!result.equals("OK")) {
							messageTemp
									.append("<p class = \"red-text\">Error when updating <strong>"
											+ groupName
											+ "</strong> membership on Active Directory: "
											+ result + "</p>");
							LOG.info("AssignUsersToGroupTask run(): Failed to update AD group "
									+ groupName);
							error = true;
							numberOfFailure++;
						} else {
							messageTemp
									.append("<p>Successfully updated <strong>"
											+ groupName
											+ "</strong> membership on Active Directory</p>");
							numberOfSuccess++;
						}
					}

					tempProgress++;
					temp++;
					progress = (tempProgress * 100) / totalProgress;
				}
			}
			if (licensesToRemove != null) {
				LOG.info("AssignUsersToGroupTask run(): Number of groups to remove: "
						+ licensesToRemove.size());
				for (License license : licensesToRemove) {
					if (temp < tempProgress) {
						message = "Restoring progress ... ";
						temp++;
						continue;
					}

					if (license.getGroupEnabled() == 0) {
						messageTemp
								.append("<p>Skipped group update for license <strong>"
										+ license.getName()
										+ "</strong> as license group is disabled for this license.</p>");
						numberOfSuccess++;
						continue;
					}

					List<ModificationItem> groupMods = new ArrayList<ModificationItem>();
					String groupName = license.getGroupName();

					for (User user : users) {
						LOG.info("AssignUsersToGroupTask run(): Removing "
								+ user.getAduser() + " from group " + groupName);
						if (user.getAttributes() == null) {
							user = ActiveDirectoryHelper.getUser(
									"sAMAccountName=" + user.getAduser(), null);
						}

						groupMods.add(new ModificationItem(
								DirContext.REMOVE_ATTRIBUTE,
								new BasicAttribute("member", user
										.getAttribute("distinguishedName"))));
					}

					String result = ActiveDirectoryHelper.modifyGroupAttribute(
							groupMods.toArray(new ModificationItem[0]),
							groupName);
					if (!result.equals("OK")) {
						messageTemp
								.append("<p class = \"red-text\">Error when updating <strong>"
										+ groupName
										+ "</strong> membership on Active Directory: "
										+ result + "</p>");
						LOG.info("AssignUsersToGroupTask run(): Failed to update AD group "
								+ groupName);
						error = true;
						numberOfFailure++;
					} else {
						messageTemp
								.append("<p>Successfully updated <strong>"
										+ groupName
										+ "</strong> membership on Active Directory</p>");
						numberOfSuccess++;
					}
					tempProgress++;
					temp++;
					progress = (tempProgress * 100) / totalProgress;
				}
			}
			progress = 100;
			message = messageTemp.toString();
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"group", progress, tempProgress, (error) ? 1 : 0, message,
					numberOfSuccess, numberOfFailure);

		} catch (Exception e) {
			LOG.error("AssignUsersToGroupTask run(): " + e.getMessage(), e);
			message = "<p class = \"red-text\">Task encountered a problem: "
					+ ((e.getMessage() == null) ? "Unknown reason" : e
							.getMessage())
					+ ": Please contact administrator.</p>";
			progress = 101;
			TaskDBHelper.updateTask(getTimeStart(), getAdminUser().getAduser(),
					"group", progress, tempProgress, 1, message,
					numberOfSuccess, numberOfFailure);
		}
	}
}
