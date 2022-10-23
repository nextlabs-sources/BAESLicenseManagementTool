package com.nextlabs.bae.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

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
import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.Group;
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseDBHelper;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserDBHelper;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.helper.UserProjectDBHelper;

public class SynchronizationTask implements Runnable {
	private int progress;
	private int tempProgress;
	private String message;
	private Boolean error;
	private Timestamp timeStart;
	private int totalProgress;
	private int numberOfSuccess;
	private int numberOfFailure;
	private List<License> licenses;
	private List<User> users;
	private String[] attributesToFetch;
	private List<String> controlledAttributeList;
	private HashMap<String, String> attributeLabelMap;
	private List<String> usersFromDB;

	private List<String> errorMessages;

	private static final Log LOG = LogFactory
			.getLog(AssignLicensesToUsersTask.class);

	public SynchronizationTask() {
		this.timeStart = new Timestamp(new Date().getTime());
		this.progress = 0;
		this.error = false;
		this.message = "";

		controlledAttributeList = new ArrayList<String>();
		attributeLabelMap = new HashMap<String, String>();
		int index = 1;
		while (true) {
			String attribute = PropertyLoader.bAESProperties.getProperty("attr"
					+ index);
			String label = PropertyLoader.bAESProperties.getProperty("label"
					+ index);
			if (attribute == null || label == null) {
				break;
			} else {
				controlledAttributeList.add(attribute.trim());
				attributeLabelMap.put(attribute.trim(), label.trim());
				attributeLabelMap.put(label.trim(), attribute.trim());
				index++;
			}
		}

		LOG.debug("Contructor() Find " + controlledAttributeList.size()
				+ " attributes.");

		licenses = LicenseDBHelper.getAllLicense();

		// get the attribute list from properties file

		String filter = "(&(mail=*"
				+ PropertyLoader.bAESProperties.getProperty("email-filter")
				+ ")(|(objectClass=user)))";
		List<String> attributesToFetch = new ArrayList<String>();
		for (String licenseAttribute : controlledAttributeList) {
			attributesToFetch.add(licenseAttribute);
		}
		attributesToFetch.add("sAMAccountName");
		attributesToFetch.add("displayName");
		attributesToFetch.add("mail");
		attributesToFetch.add("userPrincipalName");
		attributesToFetch.add("distinguishedName");
		attributesToFetch.add(PropertyLoader.bAESProperties.getProperty("pa"));
		this.attributesToFetch = attributesToFetch.toArray(new String[0]);
		users = ActiveDirectoryHelper.getUsers(filter, this.attributesToFetch,
				900);
		usersFromDB = UserDBHelper.getUniqueUserFromDB();

		LOG.debug("Find " + licenses.size() + " licenses - " + users.size()
				+ " users from AD - " + usersFromDB.size() + " users from DB");

		totalProgress = licenses.size() + users.size() + usersFromDB.size() + 1;
		tempProgress = 0;
		errorMessages = new ArrayList<String>();
	}

	@Override
	public void run() {
		try {
			if (totalProgress == 0) {
				LOG.info("run() No user or license was found");
				return;
			}
			syncLicenseGroup();
			syncUser();
			deleteInvalidUsers();

		} catch (Exception e) {
			LOG.error("run() Application error: " + e.getMessage(), e);
			errorMessage("<strong><strong>Application error</strong></strong>: Please check the log for more possible causes. The synchronization has stopped");
		}
	}

	private void syncLicenseGroup() throws Exception {
		List<ModificationItem> groupMods = new ArrayList<ModificationItem>();
		for (License license : licenses) {
			if (license.getGroupEnabled() == 1) {
				LOG.debug("syncLicenseGroup() Group enabled for license "
						+ license.getName());

				String groupName = license.getGroupName();
				Group group = ActiveDirectoryHelper.getAdGroup(groupName, null);

				// if the license group doesn't exist
				if (group == null) {
					LOG.info("syncLicenseGroup() Detect inconsistency. License group doesn't exist for license "
							+ license.getName() + ". Try to create group");
					Boolean result = ActiveDirectoryHelper
							.createADGroup(groupName);

					if (!result) {
						LOG.error("syncLicenseGroup() Unable to create group "
								+ groupName);
						errorMessage("<strong><strong>Application Error</strong></strong>: Attempted to create group <strong><strong>"
								+ groupName
								+ "</strong></strong> in Active Directory but failed. Please check the log for more possible causes. "
								+ "The synchronization will be stopped.");
						error = true;
						throw new Exception("Application Error.");

					} else {

						// add all existing users of the license to the group;
						// the source of the users is the Active Directory
						groupMods.clear();
						String[] attributesToFetch = { "distinguishedName" };
						List<User> licenseUsers = ActiveDirectoryHelper
								.getUsers(
										"(&(mail=*"
												+ PropertyLoader.bAESProperties
														.getProperty("email-filter")
												+ ")(|(objectClass=user))("
												+ license.getCategory() + "="
												+ license.getName() + "))",
										attributesToFetch, 900);
						for (User userToAssign : licenseUsers) {
							groupMods
									.add(new ModificationItem(
											DirContext.ADD_ATTRIBUTE,
											new BasicAttribute(
													"member",
													userToAssign
															.getAttribute("distinguishedName"))));
						}
						String resultString = ActiveDirectoryHelper
								.modifyGroupAttribute(groupMods
										.toArray(new ModificationItem[0]),
										groupName);
						if (!resultString.equals("OK")) {
							errorMessage("<strong><strong>Application Error </strong></strong>:Error when fixing <strong><strong>"
									+ groupName
									+ "</strong></strong> membership on Active Directory: "
									+ resultString
									+ ". Please check the log for more possible causes. "
									+ "The synchronization process will be stopped.");
							LOG.error("synchronizeGroup(): Failed to update license group "
									+ groupName);
							error = true;
							throw new Exception("Application Error.");
						} else {
							LOG.info("synchronizeGroup(): Succeeded to update license group "
									+ groupName);
						}
					}
				} else {
					String[] attributesToFetch = { "sAMAccountName",
							"distinguishedName" };
					String members = ActiveDirectoryHelper.getUsersAsString(
							"memberOf="
									+ group.getAttribute("distinguishedName"),
							"distinguishedName", attributesToFetch, 900);

					LOG.debug("synchronizeGroup() members = " + members);

					String usersOfLicense = ActiveDirectoryHelper
							.getUsersAsString(
									"(&(mail=*"
											+ PropertyLoader.bAESProperties
													.getProperty("email-filter")
											+ ")(|(objectClass=user))("
											+ license.getCategory() + "="
											+ license.getName() + "))",
									"distinguishedName", attributesToFetch, 900);

					LOG.debug("synchronizeGroup() usersOfLicense = "
							+ usersOfLicense);

					if (members == null || usersOfLicense == null) {
						errorMessage("<strong><strong>Application Error</strong></strong>: Error when fixing <strong><strong>"
								+ groupName
								+ "</strong></strong> membership on Active Directory. Please check the log for more possible causes. "
								+ "The synchronization process will be stopped.");
						LOG.error("synchronizeGroup(): Failed to update license group "
								+ groupName);
						error = true;
						throw new Exception("Application Error");
					} else {
						if (!members.equals(usersOfLicense)) {
							LOG.info("synchronizeGroup() Members of group "
									+ groupName
									+ " does not equal user list of license "
									+ license.getName() + ". Attempt to fix.");
							List<String> memberList = Arrays.asList(members
									.split(", "));
							List<String> userListOfLicense = Arrays
									.asList(usersOfLicense.split(", "));

							groupMods.clear();

							for (String user : userListOfLicense) {

								if (user.length() == 0) {
									continue;
								}

								if (!memberList.contains(user)) {
									groupMods
											.add(new ModificationItem(
													DirContext.ADD_ATTRIBUTE,
													new BasicAttribute(
															"member", user)));
									LOG.debug("synchronizeGroup() Adding "
											+ user);
								}
							}

							for (String user : memberList) {

								if (user.length() == 0) {
									continue;
								}

								if (!userListOfLicense.contains(user)) {
									groupMods
											.add(new ModificationItem(
													DirContext.REMOVE_ATTRIBUTE,
													new BasicAttribute(
															"member", user)));
									LOG.debug("synchronizeGroup() Removing "
											+ user);
								}
							}

							String resultString = ActiveDirectoryHelper
									.modifyGroupAttribute(groupMods
											.toArray(new ModificationItem[0]),
											groupName);
							if (!resultString.equals("OK")) {
								errorMessage("<strong><strong>Application Error</strong></strong>: Error when fixing <strong><strong>"
										+ groupName
										+ "</strong></strong> membership on Active Directory: "
										+ resultString
										+ ". Please check the log for more possible causes."
										+ " The synchronization process will be stopped.");
								LOG.error("synchronizeGroup(): Failed to update license group "
										+ groupName);
								error = true;
								throw new Exception("Application Error");
							} else {
								LOG.info("synchronizeGroup(): Succeeded to update license group "
										+ groupName);
							}
						} else {
							LOG.info("synchronizeGroup() License "
									+ license.getName() + " and group "
									+ groupName + " are consistent");
						}
					}
				}
			} else {
				LOG.info("synchorizedGroup() Group is not enabled for license "
						+ license.getName() + ". Nothing to be done");
				numberOfSuccess++;
			}
			tempProgress++;
			progress = (tempProgress * 100) / totalProgress;
		}
	}

	private void syncUser() throws Exception {
		List<User> userToPerform = new ArrayList<User>();
		List<Project> projectToAdd = new ArrayList<Project>();
		List<Project> projectToRemove = new ArrayList<Project>();
		List<License> licenseToAdd = new ArrayList<License>();
		List<License> licenseToRemove = new ArrayList<License>();
		for (User user : users) {
			userToPerform.clear();
			userToPerform.add(user);

			// synchronize projects
			String projectStringFromDB = UserProjectDBHelper
					.getProjectsByUserAsString(user.getAduser());
			LOG.debug("syncUser() projectString from AB is "
					+ projectStringFromDB);
			String projectStringFromAD = user
					.getAttribute(PropertyLoader.bAESProperties
							.getProperty("pa"));

			List<String> projectListFromAD = Arrays.asList(projectStringFromAD
					.split(", "));
			Collections.sort(projectListFromAD);

			StringBuilder projectBuilderFromAD = new StringBuilder();
			for (int i = 0; i < projectListFromAD.size(); i++) {
				projectBuilderFromAD.append(projectListFromAD.get(i) + ", ");
			}

			if (projectBuilderFromAD.length() > 0) {
				projectStringFromAD = projectBuilderFromAD.substring(0,
						projectBuilderFromAD.length() - 2);
			}

			LOG.debug("syncUser() projectString from DB is "
					+ projectStringFromAD);

			if (projectStringFromDB.equals(projectStringFromAD)) {
				LOG.debug("syncUser project of user " + user.getAduser()
						+ " is consistent. ");
			} else {

				LOG.debug("syncUser projects of user " + user.getAduser()
						+ " is inconsistent. Attempt to fix");

				List<String> projectListFromDB = Arrays
						.asList(projectStringFromDB.split(", "));
				projectToAdd.clear();
				projectToRemove.clear();

				for (String projectName : projectListFromAD) {

					if (projectName.length() == 0) {
						continue;
					}

					if (!projectListFromDB.contains(projectName)) {
						Project project = ProjectDBHelper
								.getProject(projectName);
						if (project == null) {
							LOG.debug("syncUser() Project " + projectName
									+ " of user " + user.getAduser()
									+ " doesn't exist in the database.");

							errorMessage("Project <strong><strong>"
									+ projectName
									+ "</strong></strong> exists in the Active Directory for user <strong><strong>"
									+ user.getAduser()
									+ "</strong></strong> but it doesn't exist in the database. "
									+ "No synchronization will be applied to this value since the database should be the master source of projects.");
						} else {
							projectToAdd.add(project);
						}
					}
				}

				Boolean result = UserProjectDBHelper.assignProjectsToUsers(
						projectToAdd, userToPerform);
				if (!result) {
					LOG.error("syncUser() Failed to update projects of user "
							+ user.getAduser() + " in the database");
					errorMessage("<strong><strong>Application Error</strong></strong>: Failed to update user <strong><strong>"
							+ user.getAduser()
							+ "</strong></strong>. "
							+ "Please check the log for more possible causes. "
							+ "The synchronization process will be stopped.");
					error = true;
					throw new Exception("Application Error");
				}

				for (String projectName : projectListFromDB) {
					if (projectName.length() == 0) {
						continue;
					}

					if (!projectListFromDB.contains(projectName)) {
						projectToRemove.add(ProjectDBHelper
								.getProject(projectName));
					}
				}

				result = UserProjectDBHelper.removeProjectsFromUsers(
						projectToRemove, userToPerform);
				if (!result) {
					LOG.error("syncUser() Failed to update projects of user "
							+ user.getAduser() + " in the database");
					errorMessage("<strong><strong>Application Error</strong></strong>: Failed to update user <strong><strong>"
							+ user.getAduser()
							+ "</strong></strong>. "
							+ "Please check the log for more possible causes. "
							+ "The synchronization process will be stopped.");
					error = true;
					throw new Exception("Application Error");
				}
			}

			// synchronize license
			for (String attribute : controlledAttributeList) {
				String licenseStringFromDB = UserLicenseDBHelper
						.getLicensesByUserAsString(user.getAduser(), attribute);
				LOG.debug("syncUser() licenseStringFromDB for " + attribute
						+ " is " + licenseStringFromDB);

				String licenseStringFromAD = user.getAttribute(attribute);
				List<String> licenseListFromAD = Arrays
						.asList(licenseStringFromAD.split(", "));
				Collections.sort(licenseListFromAD);

				StringBuilder licenseBuilderFromAD = new StringBuilder();
				for (int i = 0; i < licenseListFromAD.size(); i++) {
					licenseBuilderFromAD
							.append(licenseListFromAD.get(i) + ", ");
				}

				if (licenseBuilderFromAD.length() > 0) {
					licenseStringFromAD = licenseBuilderFromAD.substring(0,
							licenseBuilderFromAD.length() - 2);
				}
				LOG.debug("syncUser() licenseStringFromAD for " + attribute
						+ " is " + licenseStringFromAD);

				if (licenseStringFromAD.equals(licenseStringFromDB)) {
					LOG.debug("syncUser attribute " + attribute + " of user "
							+ user.getAduser() + " is consistent. ");

					// check if license assignment is actually valid
					// based on license-project relationship
					for (String licenseName : licenseListFromAD) {

						if (licenseName.length() == 0) {
							continue;
						}
						Boolean valid = false;
						List<String> licenseProjects = LicenseProjectDBHelper
								.getProjectNameByLicense(licenseName);
						if (licenseProjects.size() == 0) {
							valid = true;
						} else {
							for (String p : licenseProjects) {
								if (projectListFromAD.contains(p)) {
									valid = true;
									break;
								}
							}
						}
						if (!valid) {
							LOG.info("syncUser() License " + licenseName
									+ " of user " + user.getAduser()
									+ " is invalid");
							errorMessage("License <strong><strong>"
									+ licenseName
									+ "</strong></strong> of user <strong><strong>"
									+ user.getAduser()
									+ "</strong></strong> is invalid as the user isn't a member of any license's projects");
						}
					}
				} else {
					LOG.debug("syncUser attribute " + attribute + " of user "
							+ user.getAduser()
							+ " is inconsistent. Attempt to fix");
					licenseToAdd.clear();
					licenseToRemove.clear();

					List<String> licenseListFromDB = Arrays
							.asList(licenseStringFromDB.split(", "));

					for (String licenseName : licenseListFromAD) {

						if (licenseName.length() == 0) {
							continue;
						}

						License license = LicenseDBHelper
								.getLicense(licenseName);
						if (!licenseListFromDB.contains(licenseName)) {
							// if the license doesn't exist in the database
							if (license == null) {
								LOG.debug("syncUser() License " + licenseName
										+ " of user " + user.getAduser()
										+ " doesn't exist in the database.");

								errorMessage("License <strong><strong>"
										+ licenseName
										+ "</strong></strong> exists in the Active Directory for user <strong><strong>"
										+ user.getAduser()
										+ "</strong></strong> but it doesn't exist in the database. "
										+ "No synchronization will be applied to this value since the database should be the master source of licenses. The license value can be manually removed in the Manage User Details screen");
							} else {
								licenseToAdd.add(license);
							}
						}

						if (license != null) {
							// check if license assignment is actually valid
							// based on license-project relationship
							Boolean valid = false;
							List<String> licenseProjects = LicenseProjectDBHelper
									.getProjectNameByLicense(licenseName);
							if (licenseProjects.size() == 0) {
								valid = true;
							} else {
								for (String p : licenseProjects) {
									if (projectListFromAD.contains(p)) {
										valid = true;
										break;
									}
								}
							}
							if (!valid) {
								LOG.info("syncUser() License " + licenseName
										+ " of user " + user.getAduser()
										+ " is invalid");
								errorMessage("License <strong><strong>"
										+ licenseName
										+ "</strong></strong> of user <strong><strong>"
										+ user.getAduser()
										+ "</strong></strong> is invalid as the user isn't a member of any license's projects");
							}
						}
					}

					Boolean result = UserLicenseDBHelper.assignLicensesToUsers(
							licenseToAdd, userToPerform);
					if (!result) {
						LOG.error("syncUser() Failed to update licenses of user "
								+ user.getAduser() + " in the database");
						errorMessage("<strong><strong>Application Error</strong></strong>: Failed to update user <strong><strong>"
								+ user.getAduser()
								+ "</strong></strong>. "
								+ "Please check the log for more possible causes. "
								+ "The synchronization process will be stopped.");
						error = true;
						throw new Exception("Application Error");
					}

					for (String licenseName : licenseListFromDB) {
						if (licenseName.length() == 0) {
							continue;
						}

						if (!licenseListFromAD.contains(licenseName)) {
							licenseToRemove.add(LicenseDBHelper
									.getLicense(licenseName));
						}
					}

					result = UserLicenseDBHelper.removeLicensesFromUsers(
							licenseToRemove, userToPerform);
					if (!result) {
						LOG.error("syncUser() Failed to update licenses of user "
								+ user.getAduser() + " in the database");
						errorMessage("<strong><strong>Application Error</strong></strong>: Failed to update user <strong><strong>"
								+ user.getAduser()
								+ "</strong></strong>. "
								+ "Please check the log for more possible causes. "
								+ "The synchronization process will be stopped.");
						error = true;
						throw new Exception("Application Error");
					}
				}
			}
			tempProgress++;
			progress = (tempProgress * 100) / totalProgress;
		}
	}

	private void deleteInvalidUsers() throws Exception {

		List<String> invalidUsers = new ArrayList<String>();
		String[] attributes = { "sAMAccountName" };
		for (String user : usersFromDB) {

			if (ActiveDirectoryHelper.getUser("sAMAccountName=" + user,
					attributes) == null) {
				LOG.info("User " + user +" doesn't exist in AD but has relationships in DB. Add for removal");
				invalidUsers.add(user);		
			}
			tempProgress++;
			progress = (tempProgress * 100) / totalProgress;
		}

		Boolean result = UserDBHelper.removeUsersFromDatabase(invalidUsers);
		if (!result) {
			LOG.error("syncUser() Failed to remove invalid users from Database");
			errorMessage("<strong><strong>Application Error</strong></strong>: Failed to remove invalid users from the Database. "
					+ "Please check the log for more possible causes. "
					+ "The synchronization process will be stopped.");
			error = true;
			throw new Exception("Application Error");
		}
		tempProgress++;
		progress = (tempProgress * 100) / totalProgress;
	}

	private void errorMessage(String message) {
		errorMessages.add("<p class = \"red-text\">" + message + "</p>");
	}

	public InputStream exportTask() {
		InputStream in = null;
		try {
			Document pdf_data = new Document(PageSize.A4);
			Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
			Font normalFont = new Font(Font.FontFamily.HELVETICA, 7,
					Font.NORMAL);
			Font headerFont = new Font(Font.FontFamily.HELVETICA, 8,
					Font.NORMAL);
			headerFont.setColor(255, 255, 255);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfWriter.getInstance(pdf_data, baos);
			pdf_data.setMargins(2, 2, 2, 2);
			pdf_data.open();

			pdf_data.addTitle("License Management Tool Synchronization Details");

			Paragraph para = new Paragraph(
					"License Management Tool Synchronization Details",
					titleFont);
			para.setAlignment(Element.ALIGN_CENTER);
			pdf_data.add(para);

			para = new Paragraph("Time start: " + timeStart, normalFont);
			para.setAlignment(Element.ALIGN_RIGHT);
			pdf_data.add(para);

			pdf_data.add(new Paragraph(" "));

			PdfPTable table = new PdfPTable(1);
			PdfPCell table_cell;

			table_cell = new PdfPCell(new Phrase("Error Messages", headerFont));
			table_cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			table_cell.setBackgroundColor(new BaseColor(20, 20, 20));
			table_cell.setPadding(3);
			table_cell.setBorderColor(new BaseColor(200, 200, 200));
			table.addCell(table_cell);

			if (errorMessages.size() == 0) {
				table_cell = new PdfPCell(new Phrase("No record to display",
						normalFont));
				table_cell.setPadding(3);
				table_cell.setBorderColor(new BaseColor(200, 200, 200));
				table.addCell(table_cell);
			} else {
				for (String message : errorMessages) {
					message = message
							.replaceAll("<p class = \"red-text\">", "");
					message = message.replaceAll("</p>", "");
					message = message.replaceAll("<strong><strong>", "\"");
					message = message.replaceAll("</strong></strong>", "\"");
					table_cell = new PdfPCell(new Phrase(message, normalFont));
					table_cell.setPadding(3);
					table_cell.setBorderColor(new BaseColor(200, 200, 200));
					table.addCell(table_cell);
				}
			}

			pdf_data.add(table);
			pdf_data.close();

			in = new ByteArrayInputStream((baos.toByteArray()));
		} catch (Exception e) {
			LOG.error("exportTask() Failed to export task");
		}
		return in;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Boolean getError() {
		return error;
	}

	public void setError(Boolean error) {
		this.error = error;
	}

	public Timestamp getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(Timestamp timeStart) {
		this.timeStart = timeStart;
	}

	public int getTotalProgress() {
		return totalProgress;
	}

	public void setTotalProgress(int totalProgress) {
		this.totalProgress = totalProgress;
	}

	public int getNumberOfSuccess() {
		return numberOfSuccess;
	}

	public void setNumberOfSuccess(int numberOfSuccess) {
		this.numberOfSuccess = numberOfSuccess;
	}

	public int getNumberOfFailure() {
		return numberOfFailure;
	}

	public void setNumberOfFailure(int numberOfFailure) {
		this.numberOfFailure = numberOfFailure;
	}

	public List<License> getLicenses() {
		return licenses;
	}

	public void setLicenses(List<License> licenses) {
		this.licenses = licenses;
	}

	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

}
