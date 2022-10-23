package com.nextlabs.bae.bean;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseDBHelper;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;
import com.nextlabs.bae.helper.Pair;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.task.AssignLicensesToUsersTask;
import com.nextlabs.bae.task.AssignProjectsToUsersTask;
import com.nextlabs.bae.task.AssignUsersToGroupTask;
import com.nextlabs.bae.task.AsyncTask;

/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

@ManagedBean(name = "manageUserDetailsBean")
@ViewScoped
public class ManageUserDetailsBean implements Serializable {

	private static final Log LOG = LogFactory
			.getLog(ManageUserDetailsBean.class);
	private static final long serialVersionUID = 1L;
	private List<Pair<String, String>> attributeValues;
	private List<Pair<String, String>> userAttributeList;
	private boolean displayUser;
	private User searchUser;
	private String selectedAttribute;
	private DualListModel<String> pickLicenses;
	private DualListModel<String> pickProjects;
	private List<Project> userCurrentProjects;
	private String filterAvailableProjects;
	private String filterAvailableLicenses;
	private int filterLimit;
	private List<String> temporaryCurrentProjects;
	private List<String> temporaryCurrentLicenses;
	private List<String> initialCurrentProjects;
	private List<String> initialCurrentLicenses;
	private boolean assignLicensesWithProject;
	private List<License> invalidLicenses;
	private List<Project> invalidProjects;

	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	transient private ManageExecutorServiceBean taskBean;

	/**
	 * Executed after the bean is constructed
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	@PostConstruct
	public void init() {
		try {
			if (PropertyLoader.BAEADFrontEndPropertiesPath == null
					|| PropertyLoader.BAESToolConstantPath == null
					|| !userSessionBean.isLoggedIn()) {
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

			pickLicenses = new DualListModel<String>();
			pickProjects = new DualListModel<String>();
			filterLimit = 1000;
			filterAvailableProjects = "";
			filterAvailableLicenses = "";
			assignLicensesWithProject = true;
			invalidLicenses = new ArrayList<License>();
			invalidProjects = new ArrayList<Project>();
			userCurrentProjects = new ArrayList<Project>();

			// get user from context
			String filter = (String) FacesContext.getCurrentInstance()
					.getExternalContext().getFlash().get("selectedUser");
			getUser(filter);
		} catch (Exception e) {
			LOG.error("ManageUserDetailsBean init(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	public ManageUserDetailsBean() {
	}

	/**
	 * Unauthorized access to the page - redirect
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	private void unauthorize() {
		LOG.info("ManageUserDetailsBean unauthorize(): Not logged in");
		// not logged in
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (Exception e) {
			LOG.error("ManageUserDetailsBean unauthorize(): " + e.getMessage(),
					e);
		}
	}
	
	private void logoutOnSync() {
		try {
			userSessionBean.logout();
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (IOException e) {
			LOG.error("ManageGroupBean init(): " + e.getMessage(), e);
		}
	}

	/**
	 * Return manage user detail page after getting the selected user
	 * 
	 * @param filter
	 *            Filter to be searched by AD
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return manage user detail page
	 */
	public String getUser(String filter) {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return null;
		}
		
		try {
			if (filter == null) {
				if (userSessionBean.getLastViewUser() == null
						|| userSessionBean.getLastViewUser().equals("")) {
					displayUser = false;
					returnMessage(null, FacesMessage.SEVERITY_ERROR,
							"USER_ERROR_PASSING_DETAILS_MSG",
							"USER_ERROR_PASSING_DETAILS_DES");
					return null;
				} else {
					filter = userSessionBean.getLastViewUser();
				}
			}
			displayUser = true;

			searchUser = ActiveDirectoryHelper.getUser(filter, null);

			if (searchUser == null) {
				LOG.error("ManageUserDetailsBean getUser(): Returned user is null.");
				FacesContext
						.getCurrentInstance()
						.addMessage(
								null,
								new FacesMessage(
										FacesMessage.SEVERITY_ERROR,
										"User not found!",
										"The selected user cannot be found. The search scope might have been changed. You can refer to the log file for more information"));
				return null;
			}

			LOG.info("ManageUserDetailsBean getUser(): Displaying details of user "
					+ searchUser.getAttribute("sAMAccountName"));

			// get all user displayed attributes
			userAttributeList = new ArrayList<Pair<String, String>>();
			for (String attributeName : userSessionBean
					.getUserDisplayedAttributeList()) {
				userAttributeList.add(new Pair<String, String>(
						userSessionBean.getUserDisplayedAttributeLabelMap()
								.get(attributeName), searchUser
								.getAttribute(attributeName)));
			}

			updateAttributeValues();

			updateUserProjectList();

			userSessionBean.setLastViewUser("sAMAccountName="
					+ searchUser.getAttribute("sAMAccountName"));

		} catch (Exception e) {
			LOG.error("ManageUserDetailsBean getUser(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
		return "manageUserDetails";
	}

	/**
	 * Update attribute values of user in the UI
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateAttributeValues() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			attributeValues = new ArrayList<Pair<String, String>>();

			for (String key : userSessionBean.getControlledAttributeList()) {

				attributeValues.add(new Pair<String, String>(userSessionBean
						.getAttributeLabelMap().get(key), searchUser
						.getAttribute(key)));
			}

			userAttributeList = new ArrayList<Pair<String, String>>();
			for (String attributeName : userSessionBean
					.getUserDisplayedAttributeList()) {
				userAttributeList.add(new Pair<String, String>(
						userSessionBean.getUserDisplayedAttributeLabelMap()
								.get(attributeName), searchUser
								.getAttribute(attributeName)));
			}
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateAttributeValues(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Get suggested list for search box (the first 10 users)
	 * 
	 * @param query
	 *            Search keyword
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of users
	 */
	public List<String> autoCompleteKeyword(String query) {
		List<String> results = new ArrayList<String>();
		try {
			// get search by
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			String queryBy = request
					.getParameter("search-form:search-by_input");
			String filter = "(&(" + queryBy + "=" + query
					+ "*)(sAMAccountName=" + query
					+ "*)(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ "))";

			// get auto complete list
			String[] returnAttributes = { queryBy };
			List<User> userList = ActiveDirectoryHelper.getUsers(filter,
					returnAttributes, 10);
			for (User user : userList) {
				results.add(user.getAttribute(queryBy));
			}
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean autoCOmpleteKeyword(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
		return results;
	}

	/**
	 * Initialize Primefaces pick list for licenses
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getPickListForLicenses() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			LOG.info("ManageUserDetailsBean getPickListForLicenses(): Seleccted attribute: "
					+ selectedAttribute);
			if (selectedAttribute != null) {
				selectedAttribute = userSessionBean.getAttributeLabelMap().get(
						selectedAttribute);
				if (selectedAttribute == null) {
					selectedAttribute = "";
				}

				String licenseString = searchUser
						.getAttribute(selectedAttribute);
				LOG.info("ManageUserDetailsBean getPickListForLicenses(): Value of "
						+ selectedAttribute + " is " + licenseString);
				String[] licenseArray = licenseString.split(", ");
				List<String> source = new ArrayList<String>();
				List<String> target = new ArrayList<String>();
				invalidLicenses.clear();

				// build target list with existing licenses
				if (!licenseString.trim().equals("")) {
					for (String t : licenseArray) {
						if (LicenseDBHelper.getLicense(t.trim()) != null) {
							target.add(t.trim());
						} else {
							invalidLicenses.add(new License(t.trim(), null,
									selectedAttribute, null, null, null, 0, 0,
									null));
						}
					}
				}

				// build source list with all licenses except those which are in
				// target list

				// project checking is applied as well
				List<License> sourceLicenseFromDB = LicenseProjectDBHelper
						.getLicensesByCategoryWithPartialNameAndLimitAndProjectConstraints(
								selectedAttribute, filterAvailableLicenses,
								filterLimit, userCurrentProjects, 0);
				for (License license : sourceLicenseFromDB) {
					if (!target.contains(license.getName().trim())) {
						source.add(license.getName());
					}
				}
				pickLicenses = new DualListModel<String>(source, target);
				temporaryCurrentLicenses = target;
				initialCurrentLicenses = target;
			} else {
				returnMessage(null, FacesMessage.SEVERITY_ERROR,
						"USER_ERROR_PASSING_ATTR_MSG",
						"USER_ERROR_PASSING_ATTR_DES");
			}
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean getPickListForLicense(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	public void onLicensePickListTransfer(TransferEvent event) {
		temporaryCurrentLicenses = pickLicenses.getTarget();
	}

	/**
	 * Update license pick list on the fly
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateLicensePickList() {
		try {
			List<String> source = new ArrayList<String>();
			List<License> sourceLicenseFromDB = LicenseProjectDBHelper
					.getLicensesByCategoryWithPartialNameAndLimitAndProjectConstraints(
							selectedAttribute, filterAvailableLicenses,
							filterLimit, userCurrentProjects, 0);

			for (License license : sourceLicenseFromDB) {
				if (!temporaryCurrentLicenses
						.contains(license.getName().trim())) {
					source.add(license.getName());
				}
			}

			pickLicenses.setSource(source);
			pickLicenses.setTarget(temporaryCurrentLicenses);
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateLicensePickList(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Udpate user attributes in AD
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateAttribute() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			List<License> licensesToAdd = new ArrayList<License>();
			List<License> licensesToRemove = new ArrayList<License>();
			List<User> userList = new ArrayList<User>();
			userList.add(searchUser);

			for (String license : initialCurrentLicenses) {
				if (!pickLicenses.getTarget().contains(license)) {
					License temp = LicenseDBHelper.getLicense(license);
					if (temp != null) {
						licensesToRemove.add(LicenseDBHelper
								.getLicense(license));
					} else {
						licensesToRemove
								.add(new License(license, null,
										selectedAttribute, null, null, null, 0,
										0, null));
					}
				}
			}

			for (String license : pickLicenses.getTarget()) {
				if (!initialCurrentLicenses.contains(license)) {
					licensesToAdd.add(LicenseDBHelper.getLicense(license));
				}
			}

			// do the modification
			if ((licensesToRemove.size() + licensesToAdd.size()) > 0) {

				// remove licenses from user
				AsyncTask licenseTask = new AssignLicensesToUsersTask(userList,
						licensesToAdd, licensesToRemove,
						userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeLicenseTask(licenseTask);

				// remove user from license groups
				AsyncTask groupTask = new AssignUsersToGroupTask(userList,
						licensesToAdd, licensesToRemove,
						userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeGroupTask(groupTask);
			} else {
				LOG.info("ManageUserDetailsBean updateAttribute(): No changes made to license list of user");
			}

			// get updated version of user attribute from ad
			searchUser = ActiveDirectoryHelper.getUser("sAMAccountName="
					+ searchUser.getAduser(), null);

			// update attribute values
			updateAttributeValues();

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"USER_SUCCESSFUL_MSG", "USER_SUCCESSFUL_UPDATE_ATTR_DES");
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateAttribute(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	public void removeInvalidLicenses() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			List<User> userList = new ArrayList<User>();
			userList.add(searchUser);
			// remove licenses from user
			AsyncTask licenseTask = new AssignLicensesToUsersTask(userList,
					null, invalidLicenses, userSessionBean.getUserName(),
					userSessionBean.getPassword(), "MANAGE USER DETAILS");

			getTaskBean().executeLicenseTask(licenseTask);
			// get updated version of user attribute from ad
			searchUser = ActiveDirectoryHelper.getUser("sAMAccountName="
					+ searchUser.getAduser(), null);

			// update attribute values
			updateAttributeValues();

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"USER_SUCCESSFUL_MSG",
					"USER_SUCCESSFUL_REMOVE_INVALID_LICENSE_DES");
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean removeInvalidLicense(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Determine whether an admin can edit an attribute or not
	 * 
	 * @param attribute
	 *            Attribute to be evaluated
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return true or false
	 */
	public boolean disableEditFor(String attribute) {
		if (userSessionBean.getAuthorizedAttributeList().contains(
				userSessionBean.getAttributeLabelMap().get(attribute))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Initialize Primefaces pick list for projects
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getPickListForProjects() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			List<Project> sourceProjectFromDB = ProjectDBHelper
					.getProjectsWithLimitAndPartialName(filterLimit,
							filterAvailableProjects, 0);
			List<String> source = new ArrayList<String>();
			List<String> target = new ArrayList<String>();

			for (Project project : userCurrentProjects) {
				if (!invalidProjects.contains(project)) {
					target.add(project.getName().trim());
				}
			}
			for (Project project : sourceProjectFromDB) {
				if (!target.contains(project.getName().trim())) {
					source.add(project.getName());
				}
			}

			pickProjects = new DualListModel<String>(source, target);
			temporaryCurrentProjects = target;
			initialCurrentProjects = target;
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateLicensePickList(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Update temporaryCurrentProjects on tranfering pick list project
	 */
	public void onProjectPickListTransfer(TransferEvent event) {
		temporaryCurrentProjects = pickProjects.getTarget();
	}

	/**
	 * Update project pick list on the fly
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateProjectPickList() {
		try {
			List<Project> sourceProjectFromDB = ProjectDBHelper
					.getProjectsWithLimitAndPartialName(filterLimit,
							filterAvailableProjects, 0);

			List<String> source = new ArrayList<String>();
			for (Project project : sourceProjectFromDB) {
				if (!temporaryCurrentProjects
						.contains(project.getName().trim())) {
					source.add(project.getName());
				}
			}

			pickProjects.setSource(source);
			pickProjects.setTarget(temporaryCurrentProjects);
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateProjectPickList(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	public void removeInvalidProjects() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			List<User> userList = new ArrayList<User>();
			userList.add(searchUser);
			AsyncTask projectTask = new AssignProjectsToUsersTask(userList,
					null, invalidProjects, userSessionBean.getUserName(),
					userSessionBean.getPassword(), "MANAGE USER DETAILS");

			getTaskBean().executeProjectTask(projectTask);

			// get updated version of user attribute from ad
			searchUser = ActiveDirectoryHelper.getUser("sAMAccountName="
					+ searchUser.getAduser(), null);

			// update attribute values
			updateAttributeValues();

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"USER_SUCCESSFUL_MSG",
					"USER_SUCCESSFUL_REMOVE_INVALID_PROJECT_DES");
		} catch (Exception e) {
			LOG.error(
					"ManageUserDetailsBean updateProjectPickList(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Update user project attribute in AD
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateUserProjects() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			List<Project> projectsToAdd = new ArrayList<Project>();
			List<Project> projectsToRemove = new ArrayList<Project>();
			List<User> userList = new ArrayList<User>();
			userList.add(searchUser);
			List<License> licensesToRemove = new ArrayList<License>();
			Map<String, Boolean> checkDuplicatesRemove = new HashMap<String, Boolean>();
			List<License> licensesToAssign = new ArrayList<License>();
			Map<String, Boolean> checkDuplicatesAssign = new HashMap<String, Boolean>();

			for (String project : initialCurrentProjects) {
				if (!pickProjects.getTarget().contains(project)) {
					projectsToRemove.add(new Project(project, "", 0));
				}
			}

			for (Project project : projectsToRemove) {
				List<License> temp = UserLicenseDBHelper
						.licensesToRemoveOnRemovingUserProject(searchUser,
								project.getName(), projectsToRemove);

				if (temp.size() > 0) {
					for (License l : temp) {
						if (checkDuplicatesRemove.get(l.getName()) == null) {
							licensesToRemove.add(l);
							checkDuplicatesRemove.put(l.getName(), true);
						}
					}
				}
			}

			// remove licenses of the project from user if user has the
			// license and user does not have any license's project

			if (licensesToRemove.size() > 0) {
				// remove licenses from user
				AsyncTask licenseTask = new AssignLicensesToUsersTask(userList,
						null, licensesToRemove, userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeLicenseTask(licenseTask);

				// remove user from ad groups
				AsyncTask groupTask = new AssignUsersToGroupTask(userList,
						null, licensesToRemove, userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeGroupTask(groupTask);
			} else {
				LOG.info("ManageUserDetailsBean updateProjectAttribute(): No licenses to remove together with project");
			}

			for (String project : pickProjects.getTarget()) {
				if (!initialCurrentProjects.contains(project)) {
					projectsToAdd.add(new Project(project, "", 0));
					if (assignLicensesWithProject) {
						List<License> licenseOfAssignedProject = LicenseProjectDBHelper
								.getLicenseByProject(project, 0);
						for (License license : licenseOfAssignedProject) {
							// assign user project's license if user does
							// not have the license
							if (searchUser.getAttributes().get(
									license.getCategory()) == null
									|| !searchUser.getAttributes()
											.get(license.getCategory())
											.contains(license.getName().trim())
									|| (searchUser.getAttributes()
											.get(license.getCategory())
											.contains(license.getName().trim()) && checkDuplicatesRemove
											.get(license.getName()) != null)) {
								if (checkDuplicatesAssign
										.get(license.getName()) == null) {
									licensesToAssign.add(license);
									checkDuplicatesAssign.put(
											license.getName(), true);
								}
							}
						}
					}
				}
			}

			if (licensesToAssign.size() > 0) {
				// assign licenses to user
				AsyncTask licenseTask = new AssignLicensesToUsersTask(userList,
						licensesToAssign, null, userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeLicenseTask(licenseTask);

				// add user to groups
				AsyncTask groupTask = new AssignUsersToGroupTask(userList,
						licensesToAssign, null, userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeGroupTask(groupTask);
			} else {
				LOG.info("ManageUserDetailsBean updateProjectAttribute(): No license to assign");
			}

			// do the modification
			if ((projectsToAdd.size() + projectsToRemove.size()) > 0) {
				AsyncTask projectTask = new AssignProjectsToUsersTask(userList,
						projectsToAdd, projectsToRemove,
						userSessionBean.getUserName(),
						userSessionBean.getPassword(), "MANAGE USER DETAILS");

				getTaskBean().executeProjectTask(projectTask);
			} else {
				LOG.info("ManageUserDetailsBean updateProjectAttribute(): No changes made to project list of user");
			}

			// get updated version of user attribute from ad
			searchUser = ActiveDirectoryHelper.getUser("sAMAccountName="
					+ searchUser.getAduser(), null);

			// update project list
			updateUserProjectList();

			// update attribute values
			updateAttributeValues();

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"USER_SUCCESSFUL_MSG", "USER_SUCCESSFUL_UPDATE_PROJ_DES");
		} catch (Exception e) {
			LOG.info(
					"ManageUserDetailsBean updateProjectAttribute(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Update project list of user
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateUserProjectList() {
		try {
			userCurrentProjects.clear();
			invalidProjects.clear();
			String userProjectString = searchUser
					.getAttribute(PropertyLoader.bAESProperties
							.getProperty("pa"));
			if (!(userProjectString == null
					|| userProjectString.equals("NOT SET") || userProjectString
					.trim().equals(""))) {
				String[] projectsArray = userProjectString.split(", ");
				for (String project : projectsArray) {
					if (ProjectDBHelper.getProject(project.trim()) == null) {
						Project temp = new Project(project.trim(), "", 0);
						userCurrentProjects.add(temp);
						invalidProjects.add(temp);
					} else {
						userCurrentProjects.add(ProjectDBHelper
								.getProject(project.trim()));
					}
				}
			}
		} catch (Exception e) {
			LOG.info(
					"ManageUserDetailsBean updateUserProjectList(): "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Refresh data of the page
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void refreshFrontEnd() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			LOG.info("ManageUserDetailsBean refreshFrontEnd() started");
			searchUser = ActiveDirectoryHelper.getUser("sAMAccountName="
					+ searchUser.getAduser(), null);
			updateAttributeValues();
			updateUserProjectList();
			LOG.info("ManageUserDetailsBean refreshFrontEnd() completed");
		} catch (Exception e) {
			LOG.info(
					"ManageUserDetailsBean refreshFrontEnd(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
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

	/*
	 * getters and setters
	 */

	public UserSessionBean getUserSessionBean() {
		return userSessionBean;
	}

	public void setUserSessionBean(UserSessionBean userSessionBean) {
		this.userSessionBean = userSessionBean;
	}

	public boolean isDisplayUser() {
		return displayUser;
	}

	public void setDisplayUser(boolean displayUser) {
		this.displayUser = displayUser;
	}

	public List<Pair<String, String>> getAttributeValues() {
		return attributeValues;
	}

	public void setAttributeValues(List<Pair<String, String>> attributeValues) {
		this.attributeValues = attributeValues;
	}

	public String getSelectedAttribute() {
		return selectedAttribute;
	}

	public void setSelectedAttribute(String selectedAttribute) {
		this.selectedAttribute = selectedAttribute;
	}

	public DualListModel<String> getPickLicenses() {
		if (pickLicenses != null) {
			return pickLicenses;
		} else {
			return new DualListModel<String>();
		}
	}

	public void setPickLicenses(DualListModel<String> pickLicenses) {
		this.pickLicenses = pickLicenses;
	}

	public User getSearchUser() {
		return searchUser;
	}

	public void setSearchUser(User searchUser) {
		this.searchUser = searchUser;
	}

	public DualListModel<String> getPickProjects() {
		if (pickProjects != null) {
			return pickProjects;
		} else {
			return new DualListModel<String>();
		}
	}

	public void setPickProjects(DualListModel<String> pickProjects) {
		this.pickProjects = pickProjects;
	}

	public List<Project> getUserCurrentProjects() {
		return userCurrentProjects;
	}

	public void setUserCurrentProjects(List<Project> userCurrentProjects) {
		this.userCurrentProjects = userCurrentProjects;
	}

	public String getFilterAvailableProjects() {
		return filterAvailableProjects;
	}

	public void setFilterAvailableProjects(String filterAvailableProjects) {
		this.filterAvailableProjects = filterAvailableProjects;
	}

	public String getFilterAvailableLicenses() {
		return filterAvailableLicenses;
	}

	public void setFilterAvailableLicenses(String filterAvailableLicenses) {
		this.filterAvailableLicenses = filterAvailableLicenses;
	}

	public int getFilterLimit() {
		return filterLimit;
	}

	public void setFilterLimit(int filterLimit) {
		this.filterLimit = filterLimit;
	}

	public List<Pair<String, String>> getUserAttributeList() {
		return userAttributeList;
	}

	public void setUserAttributeList(
			List<Pair<String, String>> userAttributeList) {
		this.userAttributeList = userAttributeList;
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

	public List<License> getInvalidLicenses() {
		return invalidLicenses;
	}

	public void setInvalidLicenses(List<License> invalidLicenses) {
		this.invalidLicenses = invalidLicenses;
	}

	public List<Project> getInvalidProjects() {
		return invalidProjects;
	}

	public void setInvalidProjects(List<Project> invalidProjects) {
		this.invalidProjects = invalidProjects;
	}

}
