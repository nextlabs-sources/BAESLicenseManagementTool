package com.nextlabs.bae.bean;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.naming.directory.Attribute;

import oracle.net.aso.e;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.event.data.PageEvent;
import org.primefaces.event.data.SortEvent;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.SearchInput;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.model.LazyADDataModel;
import com.nextlabs.bae.model.LazyLicenseDataModel;
import com.nextlabs.bae.model.LazyProjectDataModel;
import com.nextlabs.bae.task.AssignLicensesToUsersTask;
import com.nextlabs.bae.task.AssignProjectsToUsersTask;
import com.nextlabs.bae.task.AssignUsersToGroupTask;
import com.nextlabs.bae.task.AsyncTask;

/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

@ManagedBean(name = "manageUserBean")
@ViewScoped
public class ManageUserBean implements Serializable {
	private static final Log LOG = LogFactory.getLog(ManageUserBean.class);
	private static final long serialVersionUID = 1L;
	private List<User> resultList;
	private LazyADDataModel resultListLazy;
	private List<User> selectedUsers;
	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	transient private ManageExecutorServiceBean taskBean;
	private LazyLicenseDataModel licenseForAssignment;
	private LazyProjectDataModel projectForAssignment;
	private License selectedToAssignLicense;
	private Project selectedToAssignProject;
	private boolean disableBulk;
	private String bulkAction;
	private boolean disableLicenseButton;
	private boolean disableProjectButton;
	private boolean displayResult;
	private boolean assignLicensesWithProject;
	private List<SearchInput<String, String>> searchFields;
	private List<User> usersToPerform;

	/**
	 * Execute after the bean is constructed
	 * 
	 * @exception e
	 *                Any exception
	 */
	@PostConstruct
	public void init() {
		try {
			if (!userSessionBean.isLoggedIn()
					|| PropertyLoader.BAEADFrontEndPropertiesPath == null
					|| PropertyLoader.BAESToolConstantPath == null) {
				unauthorize();
				return;
			}

			if (userSessionBean.getIsAdmin()) {
				if (!userSessionBean.getIsLicenseManager()) {
					FacesContext.getCurrentInstance().getExternalContext()
							.redirect("synchronize.xhtml");
					return;
				} else {
					if (getTaskBean().getSyncStatus()) {
						FacesContext.getCurrentInstance().getExternalContext()
								.redirect("synchronize.xhtml");
						return;
					}
				}
			} else {
				if (getTaskBean().getSyncStatus()) {
					logoutOnSync();
					return;
				}
			}

			disableBulk = true;
			disableLicenseButton = true;
			disableProjectButton = true;
			displayResult = false;

			// check if there has already been a result list
			// if yes retrieve the search fields and result list from
			// userSesssionBean
			// if no initialize new search

			if (userSessionBean.getSearchFields() != null
					&& !userSessionBean.getSearchFields().isEmpty()) {
				// resultList = userSessionBean.getLastSearchResult();
				displayResult = true;
				searchFields = userSessionBean.getSearchFields();
				searchUser();
			} else {
				// initialize search field
				searchFields = new ArrayList<SearchInput<String, String>>();
				for (String searchField : userSessionBean
						.getSearchFieldsAttributeList()) {
					searchFields.add(new SearchInput<String, String>(
							searchField, ""));
				}
			}

			licenseForAssignment = new LazyLicenseDataModel(0);
			projectForAssignment = new LazyProjectDataModel(0);
			selectedUsers = new ArrayList<User>();

			bulkAction = "assign";

			assignLicensesWithProject = true;
		} catch (Exception e) {
			LOG.error("ManageUserBean init(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}

	}

	public ManageUserBean() {
	}

	/**
	 * Unauthorized access to the page - redirect
	 * 
	 * @exception e
	 *                Any exception
	 */
	private void unauthorize() {
		LOG.info("ManageUserBean unathorize(): Not logged in");
		// not logged in
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (Exception e) {
			LOG.error("ManageUserBean unathorize(): " + e.getMessage(), e);
		}
	}

	private void logoutOnSync() {
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (IOException e) {
			LOG.error("ManageGroupBean init(): " + e.getMessage(), e);
		}
	}

	/**
	 * Get suggested input for any search field
	 * 
	 * @param query
	 *            Partial input
	 * 
	 * @exception e
	 *                Any exception
	 * 
	 * @return suggestion list
	 */
	public List<String> getSuggestedInput(String query) {
		List<String> result = new ArrayList<String>();
		result.add(query + "*");
		try {
			// get search field attribute name
			String attributeName = (String) UIComponent
					.getCurrentComponent(FacesContext.getCurrentInstance())
					.getAttributes().get("attribute");

			if (attributeName.equalsIgnoreCase(PropertyLoader.bAESProperties
					.getProperty("pa"))) {
				// if search field is not project
				List<Project> projects = ProjectDBHelper
						.getProjectsWithLimitAndPartialName(10, query, 0);
				for (Project project : projects) {
					result.add(project.getName());
				}
			} else {
				// if search field is project
				String filter = "(&(mail=*"
						+ PropertyLoader.bAESProperties
								.getProperty("email-filter")
						+ ")(|(objectClass=user))(" + attributeName + "="
						+ query + "*))";
				String[] returnAttributes = { attributeName };
				List<User> userList = ActiveDirectoryHelper.getSuggestedUsers(
						filter, returnAttributes, 10);
				for (User user : userList) {
					result.add(user.getAttribute(attributeName));
				}
			}
		} catch (Exception e) {
			LOG.error("ManageUserBean getSuggestedInput(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
		return result;
	}

	/**
	 * Search User
	 * 
	 * @exception e
	 *                Any exception
	 * 
	 * @return page
	 */
	public String searchUser() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return null;
		}

		try {
			Boolean criteriaSpecified = false;
			String filter = "(&(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")(|(objectClass=user))";
			for (SearchInput<String, String> searchField : searchFields) {
				if (searchField != null && searchField.getInputs() != null) {
					filter += "(" + ((searchField.isOperator()) ? "|" : "&")
							+ "";
					for (String input : searchField.getInputs()) {
						filter += "(" + searchField.getElement0() + "=" + input
								+ ")";
					}
					filter += ")";
					criteriaSpecified = true;
				}
			}
			filter += ")";

			if (PropertyLoader.bAESProperties.getProperty(
					"ad-search-force-criteria").equals("true")
					&& !criteriaSpecified) {
				FacesContext.getCurrentInstance().addMessage(
						null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR,
								"No criteria!",
								"Please enter at least one search criteria"));
				return null;
			}

			String[] returnAttributes = { "sAMAccountName", "displayName",
					"email", "department" };
			resultList = ActiveDirectoryHelper.getUsers(filter,
					returnAttributes, 900);

			/*
			 * resultListLazy = new LazyADDataModel(filter, returnAttributes,
			 * userSessionBean.getUserName(), userSessionBean.getPassword());
			 */

			// set displaysearchresultflag to true
			displayResult = true;

			// refresh selection list
			if (selectedUsers != null) {
				selectedUsers.clear();
			} else {
				selectedUsers = new ArrayList<User>();
			}
			disableBulk = true;

		} catch (Exception e) {
			LOG.error("ManageUserBean searchUser(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
		return null;
	}

	/**
	 * Redirect to manage user detail page
	 * 
	 * @param sAMAccountName
	 *            sAMAccountName of user
	 * 
	 * @return manage user detail page (redirected)
	 */
	public String getUserDetails(String sAMAccountName) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash()
				.put("selectedUser", "sAMAccountName=" + sAMAccountName);
		return "manageUserDetails?faces-redirect=true";
	}

	/**
	 * Executed when a user row is selected - enable bulk assignment button
	 * 
	 * @param event
	 *            Select event
	 */
	public void onRowSelect(SelectEvent event) {
		disableBulk = false;
	}

	/**
	 * Executed when user selection is toggled - toggle bulk assignment button
	 * 
	 * @param event
	 *            Toggle select event
	 */
	public void onToggleSelect(ToggleSelectEvent event) {
		if (selectedUsers == null || selectedUsers.isEmpty()) {
			disableBulk = true;
		} else {
			disableBulk = false;
		}
	}

	/**
	 * Executed when a user row is unselected - disable bulk assignment button
	 * if selection is empty
	 * 
	 * @param event
	 *            Unselected event
	 */
	public void onRowUnselect(UnselectEvent event) {
		if (selectedUsers == null || selectedUsers.isEmpty()) {
			disableBulk = true;
		}
	}

	/**
	 * Executed when user table got page changed - disable bulk button and clear
	 * user selection
	 * 
	 * @param event
	 *            Page event
	 */
	public void onUserListPage(PageEvent event) {
		if (selectedUsers != null) {
			selectedUsers.clear();
		} else {
			selectedUsers = new ArrayList<User>();
		}
		disableBulk = true;
	}

	/**
	 * Executed when user table got sorted - disable bulk button and clear user
	 * selection
	 * 
	 * @param event
	 *            Sort event
	 */
	public void onUserListSort(SortEvent event) {
		if (selectedUsers != null) {
			selectedUsers.clear();
		} else {
			selectedUsers = new ArrayList<User>();
		}
		disableBulk = true;
	}

	/**
	 * Executed when user table got filtered - disable bulk button and clear
	 * user selection
	 * 
	 * @param event
	 *            Filter event
	 */
	public void onUserListFilter(FilterEvent event) {
		if (selectedUsers != null) {
			selectedUsers.clear();
		} else {
			selectedUsers = new ArrayList<User>();
		}
		disableBulk = true;
	}

	/**
	 * Enable assigning/removing button on license selected
	 * 
	 * @param event
	 *            Select event
	 */
	public void onLicenseSelect(SelectEvent event) {
		disableLicenseButton = false;
	}

	/**
	 * Disable assigning/removing button on license unselected
	 * 
	 * @param event
	 *            Unselect event
	 */
	public void onLicenseUnselect(UnselectEvent event) {
		disableLicenseButton = true;
	}

	/**
	 * Enable assigning/removing button on project selected
	 * 
	 * @param event
	 *            Select event
	 */
	public void onProjectSelect(SelectEvent event) {
		disableProjectButton = false;
	}

	/*
	 * Disable assigning/removing button on project unselected
	 * 
	 * @param event Unselect event
	 */
	public void onProjectUnselect(UnselectEvent event) {
		disableProjectButton = true;
	}

	/**
	 * Disable assigning/removing button on changing license page
	 * 
	 * @param event
	 *            Page event
	 */
	public void onLicenseListPage(PageEvent event) {
		disableLicenseButton = true;
	}

	/**
	 * Disable assigning/removing button on changing project page
	 * 
	 * @param event
	 *            Page event
	 */
	public void onProjectListPage(PageEvent event) {
		disableProjectButton = true;
	}

	/*
	 * Disable assigning/removing button on filtering license
	 * 
	 * @param event Filter event
	 */
	public void onLicenseListFilter(FilterEvent event) {
		disableLicenseButton = true;
	}

	/**
	 * Disable assigning/removing button on filtering project
	 * 
	 * @param event
	 *            Filter event
	 */
	public void onProjectListFilter(FilterEvent event) {
		disableProjectButton = true;
	}

	/**
	 * Show bulk assign license dialog
	 */
	public void licenseDialogShow() {
		disableLicenseButton = true;
		selectedToAssignLicense = null;
		RequestContext.getCurrentInstance().execute(
				"PF('bulk-license-dialog').show()");
	}

	/**
	 * Show bulk assign project dialog
	 */
	public void projectDialogShow() {
		disableProjectButton = true;
		selectedToAssignProject = null;
		RequestContext.getCurrentInstance().execute(
				"PF('bulk-project-dialog').show()");
	}

	/**
	 * Bulk assign remove license
	 * 
	 * @exception e
	 *                Any exception
	 * 
	 * @return page
	 */
	public String bulkAssignRemoveLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return null;
		}

		List<FacesMessage> warningMessages = new ArrayList<FacesMessage>();
		try {
			LOG.info("ManageUserBean bulkAssignRemoveLicense(): " + bulkAction
					+ " " + selectedToAssignLicense.getName()
					+ " to selected users");

			usersToPerform = new ArrayList<User>();

			String[] attributesToFetch = {
					selectedToAssignLicense.getCategory(), "sAMAccountName",
					"displayName", "mail", "userPrincipalName",
					"distinguishedName",
					PropertyLoader.bAESProperties.getProperty("pa") };

			selectedUsers = ActiveDirectoryHelper.getUsers(
					selectedUsers.toArray(new User[0]), attributesToFetch);

			for (User user : selectedUsers) {
				if (LicenseProjectDBHelper.getProjectByLicense(
						selectedToAssignLicense.getName()).size() != 0
						&& !UserLicenseDBHelper.validateUserLicense(user,
								selectedToAssignLicense, null)
						&& bulkAction.equals("assign")) {
					warningMessages.add(new FacesMessage(
							FacesMessage.SEVERITY_WARN,
							PropertyLoader.bAESConstant
									.getProperty("USER_SKIP_WARNING_MSG"), user
									.getAttribute("sAMaccountName")
									+ " is not a member of any "
									+ selectedToAssignLicense.getName()
									+ "'s projects. "));
					continue;
				}

				Attribute currentLicenseAttribute = user.getAttributes().get(
						selectedToAssignLicense.getCategory());

				if (bulkAction.equals("assign")) {

					// if only user does not have the license
					if (currentLicenseAttribute == null
							|| !currentLicenseAttribute
									.contains(selectedToAssignLicense.getName()
											.trim())) {
						usersToPerform.add(user);
					} else {

						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAttribute("sAMaccountName")
										+ " has already had "
										+ selectedToAssignLicense.getName()
										+ " license"));
					}
				} else {

					// if only user has the license
					if (currentLicenseAttribute != null
							&& currentLicenseAttribute
									.contains(selectedToAssignLicense.getName()
											.trim())) {
						usersToPerform.add(user);
					} else {

						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAttribute("sAMaccountName")
										+ " does not have "
										+ selectedToAssignLicense.getName()
										+ " license"));
					}
				}
			}

			List<License> licenseList = new ArrayList<License>();
			licenseList.add(selectedToAssignLicense);

			// spawning tasks
			if (usersToPerform.size() > 0) {
				if (bulkAction.equals("assign")) {
					// update user attributes
					AsyncTask licenseTask = new AssignLicensesToUsersTask(
							usersToPerform, licenseList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");
					getTaskBean().executeLicenseTask(licenseTask);

					// add user to groups
					AsyncTask groupTask = new AssignUsersToGroupTask(
							usersToPerform, licenseList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeGroupTask(groupTask);
				} else {
					// update user attributes
					AsyncTask licenseTask = new AssignLicensesToUsersTask(
							usersToPerform, null, licenseList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");
					getTaskBean().executeLicenseTask(licenseTask);

					// remove users from license group
					AsyncTask groupTask = new AssignUsersToGroupTask(
							usersToPerform, null, licenseList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeGroupTask(groupTask);
				}
			} else {
				LOG.info("ManageUserBean bulkAssignRemoveLicense(): No users to update license");
			}

			RequestContext.getCurrentInstance().execute(
					"PF('bulk-license-dialog').hide()");
			if (warningMessages.isEmpty()) {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"USER_SUCCESSFUL_MSG",
						"USER_SUCCESSFUL_" + bulkAction.toUpperCase()
								+ "_LICENSE_DES");
			} else {
				for (FacesMessage message : warningMessages) {
					FacesContext.getCurrentInstance().addMessage(null, message);
				}
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"USER_SUCCESSFUL_MSG",
						"USER_SUCCESSFUL_" + bulkAction.toUpperCase()
								+ "_LICENSE_WARNING_DES");

			}
		} catch (Exception e) {
			LOG.error(
					"ManageUserBean bulkAssignRemoveLicense(): "
							+ e.getMessage(), e);
			RequestContext.getCurrentInstance().execute(
					"PF('bulk-license-dialog').hide()");
			returnUnexpectedError(null);
		}
		return null;
	}

	/**
	 * Bulk assign/remove project
	 * 
	 * @exception e
	 *                Any exception
	 * 
	 * @return page
	 */
	public String bulkAssignRemoveProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return null;
		}

		List<FacesMessage> warningMessages = new ArrayList<FacesMessage>();
		usersToPerform = new ArrayList<User>();
		List<Project> projectList = new ArrayList<Project>();
		projectList.add(selectedToAssignProject);

		try {
			LOG.info("ManageUserBean bulkAssignRemoveProject(): " + bulkAction
					+ " " + selectedToAssignProject.getName()
					+ " to selected users");

			List<String> attributesToFetch = new ArrayList<String>();
			for (String licenseAttribute : userSessionBean
					.getControlledAttributeList()) {
				attributesToFetch.add(licenseAttribute);
			}
			attributesToFetch.add("sAMAccountName");
			attributesToFetch.add("displayName");
			attributesToFetch.add("mail");
			attributesToFetch.add("userPrincipalName");
			attributesToFetch.add("distinguishedName");
			attributesToFetch.add(PropertyLoader.bAESProperties
					.getProperty("pa"));

			// get list of users to update project
			selectedUsers = ActiveDirectoryHelper.getUsers(
					selectedUsers.toArray(new User[0]),
					attributesToFetch.toArray(new String[0]));

			for (User user : selectedUsers) {
				if (bulkAction.equals("assign")) {

					// if only user is a member of the project
					if (user.getAttributes().get(
							PropertyLoader.bAESProperties.getProperty("pa")) == null
							|| !user.getAttributes()
									.get(PropertyLoader.bAESProperties
											.getProperty("pa"))
									.contains(
											selectedToAssignProject.getName()
													.trim())) {
						usersToPerform.add(user);
					} else {

						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAttribute("sAMaccountName")
										+ " has already been a member of "
										+ selectedToAssignProject.getName()));
						continue;
					}
				} else {
					// if only user is not a member of the project
					if (user.getAttributes().get(
							PropertyLoader.bAESProperties.getProperty("pa")) != null
							&& user.getAttributes()
									.get(PropertyLoader.bAESProperties
											.getProperty("pa"))
									.contains(
											selectedToAssignProject.getName()
													.trim())) {
						usersToPerform.add(user);
					} else {
						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAttribute("sAMaccountName")
										+ " is not a member of "
										+ selectedToAssignProject.getName()));
						continue;
					}
				}
			}

			// may need to spawn a lot of tasks - execute asynchronously
			getTaskBean().executeSpawningTask(new Runnable() {

				@Override
				public void run() {
					List<Project> projectList = new ArrayList<Project>();
					projectList.add(selectedToAssignProject);

					for (User user : usersToPerform) {
						List<User> userList = new ArrayList<User>();
						userList.add(user);

						if (bulkAction.equals("assign")) {

							if (assignLicensesWithProject) {
								List<License> licenseOfAssignedProject = LicenseProjectDBHelper
										.getLicenseByProject(
												selectedToAssignProject
														.getName(), 0);

								List<License> licensesToAssign = new ArrayList<License>();

								for (License license : licenseOfAssignedProject) {

									// assign user project's license if user
									// does
									// not have the license
									if (user.getAttributes().get(
											license.getCategory()) == null
											|| !user.getAttributes()
													.get(license.getCategory())
													.contains(
															license.getName()
																	.trim())) {
										licensesToAssign.add(license);
									}
								}

								// spawning tasks
								if (licensesToAssign.size() > 0) {

									// assign licenses to user
									AsyncTask licenseTask = new AssignLicensesToUsersTask(
											userList, licensesToAssign, null,
											userSessionBean.getUserName(),
											userSessionBean.getPassword(),
											"BULK ASSIGN PROJECT TO MULTIPLE USERS");
									getTaskBean().executeLicenseTask(
											licenseTask);

									// assign user to groups
									AsyncTask groupTask = new AssignUsersToGroupTask(
											userList, licensesToAssign, null,
											userSessionBean.getUserName(),
											userSessionBean.getPassword(),
											"BULK ASSIGN PROJECT TO MULTIPLE USERS");

									getTaskBean().executeGroupTask(groupTask);
								} else {
									LOG.info("ManageUserBean bulkAssignRemoveProject(): No licenses to assign together with project");
								}
							}
						} else {

							// remove licenses of the project from user if
							// only user
							// has the license and has no license's projects

							List<License> licensesToRemove = UserLicenseDBHelper
									.licensesToRemoveOnRemovingUserProject(
											user,
											selectedToAssignProject.getName(),
											projectList);

							// spawning tasks
							if (licensesToRemove.size() > 0) {

								// remove licenses from user
								AsyncTask licenseTask = new AssignLicensesToUsersTask(
										userList, null, licensesToRemove,
										userSessionBean.getUserName(),
										userSessionBean.getPassword(),
										"BULK ASSIGN PROJECT TO MULTIPLE USERS");
								getTaskBean().executeLicenseTask(licenseTask);

								// remove user from license groups
								AsyncTask task = new AssignUsersToGroupTask(
										userList, null, licensesToRemove,
										userSessionBean.getUserName(),
										userSessionBean.getPassword(),
										"BULK ASSIGN PROJECT TO MULTIPLE USERS");

								getTaskBean().executeGroupTask(task);
							} else {
								LOG.info("ManageUserBean bulkAssignRemoveProject(): No licenses to remove together with project");
							}

						}
					}
				}
			});

			// update user project in AD
			if (usersToPerform.size() > 0) {
				if (bulkAction.equals("assign")) {
					// update user attributes
					AsyncTask projectTask = new AssignProjectsToUsersTask(
							usersToPerform, projectList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN PROJECT TO MULTIPLE USERS");
					getTaskBean().executeProjectTask(projectTask);
				} else {
					// update user attributes
					AsyncTask projectTask = new AssignProjectsToUsersTask(
							usersToPerform, null, projectList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN PROJECT TO MULTIPLE USERS");
					getTaskBean().executeProjectTask(projectTask);
				}
			} else {
				LOG.info("ManageUserBean bulkAssignRemoveProject(): No users to update project");
			}

			RequestContext.getCurrentInstance().execute(
					"PF('bulk-project-dialog').hide()");
			if (warningMessages.isEmpty()) {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"USER_SUCCESSFUL_MSG",
						"USER_SUCCESSFUL_" + bulkAction.toUpperCase()
								+ "_PROJECT_DES");
			} else {
				for (FacesMessage message : warningMessages) {
					FacesContext.getCurrentInstance().addMessage(null, message);
				}

				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"USER_SUCCESSFUL_MSG",
						"USER_SUCCESSFUL_" + bulkAction.toUpperCase()
								+ "_PROJECT_WARNING_DES");
			}
		} catch (Exception e) {
			LOG.error(
					"ManageUserBean bulkAssignRemoveProject(): "
							+ e.getMessage(), e);
			RequestContext.getCurrentInstance().execute(
					"PF('bulk-project-dialog').hide()");
			returnUnexpectedError(null);
		}
		return null;
	}

	public void returnUnexpectedError(String id) {
		returnMessage(id, FacesMessage.SEVERITY_ERROR, "UNEXPECTED_ERROR_MSG",
				"UNEXPECTED_ERROR_DES");
	}

	public void returnMessage(String id, Severity level, String sum, String des) {
		FacesContext.getCurrentInstance().addMessage(
				id,
				new FacesMessage(level, PropertyLoader.bAESConstant
						.getProperty(sum), PropertyLoader.bAESConstant
						.getProperty(des)));
	}

	/* getters and setters */

	public UserSessionBean getUserSessionBean() {
		return userSessionBean;
	}

	public void setUserSessionBean(UserSessionBean userSessionBean) {
		this.userSessionBean = userSessionBean;
	}

	public List<User> getResultList() {
		return resultList;
	}

	public void setResultList(List<User> resultList) {
		this.resultList = resultList;
	}

	public List<User> getSelectedUsers() {
		return selectedUsers;
	}

	public void setSelectedUsers(List<User> selectedUsers) {
		this.selectedUsers = selectedUsers;
	}

	public boolean isDisableBulk() {
		return disableBulk;
	}

	public void setDisableBulk(boolean disableBulk) {
		this.disableBulk = disableBulk;
	}

	public LazyLicenseDataModel getLicenseForAssignment() {
		return licenseForAssignment;
	}

	public void setLicenseForAssignment(
			LazyLicenseDataModel licenseForAssignment) {
		this.licenseForAssignment = licenseForAssignment;
	}

	public LazyProjectDataModel getProjectForAssignment() {
		return projectForAssignment;
	}

	public void setProjectForAssignment(
			LazyProjectDataModel projectForAssignment) {
		this.projectForAssignment = projectForAssignment;
	}

	public License getSelectedToAssignLicense() {
		return selectedToAssignLicense;
	}

	public void setSelectedToAssignLicense(License selectedToAssignLicense) {
		this.selectedToAssignLicense = selectedToAssignLicense;
	}

	public Project getSelectedToAssignProject() {
		return selectedToAssignProject;
	}

	public void setSelectedToAssignProject(Project selectedToAssignProject) {
		this.selectedToAssignProject = selectedToAssignProject;
	}

	public String getBulkAction() {
		return bulkAction;
	}

	public void setBulkAction(String bulkAction) {
		this.bulkAction = bulkAction;
	}

	public boolean isDisableLicenseButton() {
		return disableLicenseButton;
	}

	public void setDisableLicenseButton(boolean disableLicenseButton) {
		this.disableLicenseButton = disableLicenseButton;
	}

	public boolean isDisableProjectButton() {
		return disableProjectButton;
	}

	public void setDisableProjectButton(boolean disableProjectButton) {
		this.disableProjectButton = disableProjectButton;
	}

	public boolean isDisplayResult() {
		return displayResult;
	}

	public void setDisplayResult(boolean displayResult) {
		this.displayResult = displayResult;
	}

	public List<SearchInput<String, String>> getSearchFields() {
		return searchFields;
	}

	public void setSearchFields(List<SearchInput<String, String>> searchFields) {
		this.searchFields = searchFields;
	}

	public LazyADDataModel getResultListLazy() {
		return resultListLazy;
	}

	public void setResultListLazy(LazyADDataModel resultListLazy) {
		this.resultListLazy = resultListLazy;
	}

	public boolean isAssignLicensesWithProject() {
		return assignLicensesWithProject;
	}

	public void setAssignLicensesWithProject(boolean assignLicensesWithProject) {
		this.assignLicensesWithProject = assignLicensesWithProject;
	}

	public ManageExecutorServiceBean getTaskBean() {
		if (taskBean == null) {
			FacesContext context = FacesContext.getCurrentInstance();
			taskBean = context.getApplication().evaluateExpressionGet(context,
					"#{manageExecutorServiceBean}",
					ManageExecutorServiceBean.class);
		}

		return taskBean;
	}

	public void setTaskBean(ManageExecutorServiceBean taskBean) {
		this.taskBean = taskBean;
	}

}
