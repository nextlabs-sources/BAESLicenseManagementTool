package com.nextlabs.bae.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseDBHelper;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.helper.UserProjectDBHelper;
import com.nextlabs.bae.model.LazyLicenseDataModel;
import com.nextlabs.bae.model.LazyUserDataModel;
import com.nextlabs.bae.task.AssignLicensesToUsersTask;
import com.nextlabs.bae.task.AssignUsersToGroupTask;
import com.nextlabs.bae.task.AsyncTask;

/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

@ManagedBean(name = "manageLicenseBean")
@ViewScoped
public class ManageLicenseBean implements Serializable {

	private static final Log LOG = LogFactory.getLog(ManageLicenseBean.class);
	private static final long serialVersionUID = 1L;

	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	transient private ManageExecutorServiceBean taskBean;
	private List<License> selectedLicenses;
	private License newLicense;
	private String confirmedNewLicenseName;
	private Date newEffectiveDate;
	private Date newExpiredDate;
	private List<License> licenseList;
	private LazyLicenseDataModel licenseLazyList;
	private LazyUserDataModel userByLicenseLazyList;
	private List<User> usersOfSelectedLicense;
	private String selectedLicenseValue;
	private License selectedLicense;
	private boolean disableDelete;
	private List<String> inputUsers;
	private List<String> temporaryInputUsers;
	private List<String> inputProjects;
	private List<String> temporaryInputProjects;
	private List<String> inputProjectsForLicense;
	private List<String> toAssignProjectsInUpdate;
	private List<String> toRemoveProjectsInUpdate;
	private List<String> selectedLicenseProjects;
	private String inputMethod;
	private String bulkAction;
	private List<Project> createLicenseProjectInput;
	private StreamedContent exportFile;
	private String exportFileType;
	private List<String> columnNames;
	private List<String> selectedColumns;
	private UploadedFile importFile;
	private String importFileType;
	private int showDeactivation;
	private int activeCount;
	private int inactiveCount;
	private int oldGroupFlag;
	private boolean newLicenseGroupEnabled;
	private boolean editLicenseGroupEnabled;
	private boolean editGroupNameDisabled;

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

			showDeactivation = 0;
			// lazy loading license list
			licenseLazyList = new LazyLicenseDataModel(showDeactivation);
			// lazy loading the user list of a license
			userByLicenseLazyList = new LazyUserDataModel();
			newLicense = new License();
			selectedLicenses = new ArrayList<License>();
			disableDelete = true;
			inputMethod = "users";
			bulkAction = "assign";
			exportFileType = "csv";
			importFileType = "csv";
			columnNames = LicenseDBHelper.getColumnsName();
			selectedColumns = new ArrayList<String>();
			selectedLicenseProjects = new ArrayList<String>();
			toAssignProjectsInUpdate = new ArrayList<String>();
			toRemoveProjectsInUpdate = new ArrayList<String>();
			importFile = null;
			activeCount = LicenseDBHelper.countAllLicense(null, 0);
			inactiveCount = LicenseDBHelper.countAllLicense(null, 1);
			inputProjects = new ArrayList<String>();
			inputUsers = new ArrayList<String>();
			newLicenseGroupEnabled = false;
		} catch (Exception e) {
			LOG.error("ManageLicenseBean init(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}

	}

	public ManageLicenseBean() {
	}

	/**
	 * Unauthorized access to the page - redirect
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	private void unauthorize() {
		LOG.info("Not logged in");
		// not logged in
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (Exception e) {
			LOG.error("ManageLicenseBean unauthorize(): " + e.getMessage(), e);
			returnUnexpectedError(null);
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
	 * Toggle activation/deactivation of license list
	 */
	public void toggleDeactivation() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		licenseLazyList = new LazyLicenseDataModel(showDeactivation);
		if (selectedLicenses != null) {
			selectedLicenses.clear();
		} else {
			selectedLicenses = new ArrayList<License>();
		}
		disableDelete = true;
	}

	/**
	 * Handle row select event
	 * 
	 * @param event
	 *            Select event
	 */
	public void onRowSelect(SelectEvent event) {
		disableDelete = false;
	}

	/**
	 * Handle toggle licenes selection
	 * 
	 * @param event
	 *            Toggle event
	 */
	public void onToggleSelect(ToggleSelectEvent event) {
		if (selectedLicenses == null || selectedLicenses.isEmpty()) {
			disableDelete = true;
		} else {
			disableDelete = false;
		}
	}

	/**
	 * Handle row unselect event
	 * 
	 * @param event
	 *            Unselect event
	 */
	public void onRowUnselect(UnselectEvent event) {
		if (selectedLicenses == null || selectedLicenses.isEmpty()) {
			disableDelete = true;
		}
	}

	/**
	 * Disable selection based on security group
	 * 
	 * @param attribute
	 *            License set
	 * 
	 * @return true or false
	 */
	public boolean disableSelectFor(String attribute) {
		if (userSessionBean.getAuthorizedAttributeList().contains(
				userSessionBean.getAttributeLabelMap().get(attribute))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Execute when filter table
	 * 
	 * @param event
	 *            Filter event
	 */
	public void onFilter(FilterEvent event) {
		if (selectedLicenses != null) {
			selectedLicenses.clear();
		} else {
			selectedLicenses = new ArrayList<License>();
		}
		disableDelete = true;
	}

	/**
	 * Create new license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void createNewLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLicenseBean createnNewLicense(): Creating new license");
			// check match
			if (!confirmedNewLicenseName.equals(newLicense.getName())) {
				returnMessage("create-new-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_INVALID_INPUT_MSG",
						"LICENSE_NAME_NOT_MATCH_DES");
				return;
			}

			// check unique
			if (LicenseDBHelper.getLicense(newLicense.getName().trim()) != null) {
				returnMessage("create-new-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_DUPLICATE_MSG", "LICENSE_DUPLICATE_DES");
				return;
			}

			// check date
			LOG.info("ManageLicenseBean createNewLicense(): Date "
					+ newEffectiveDate + " " + newExpiredDate);

			if (!(((newEffectiveDate != null) && (newExpiredDate != null)) || ((newEffectiveDate == null) && (newExpiredDate == null)))) {
				returnMessage("create-new-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_INVALID_INPUT_MSG",
						"LICENSE_INVALID_DATE_EMPTY_DES");
				return;
			}

			if ((newEffectiveDate != null) && (newExpiredDate != null)
					&& (newExpiredDate.getTime() < newEffectiveDate.getTime())) {
				returnMessage("create-new-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_INVALID_INPUT_MSG", "LICENSE_INVALID_DATE_DES");
				return;
			}

			// add to database

			newLicense.setCategory(userSessionBean.getAttributeLabelMap().get(
					newLicense.getLabel()));

			newLicense.setGroupEnabled(newLicenseGroupEnabled ? 1 : 0);

			Boolean result = LicenseDBHelper.createLicense(newLicense.getName()
					.trim(), newLicense.getParties().trim(), newLicense
					.getCategory(), newLicense.getLabel().trim(),
					(newEffectiveDate == null) ? null : new Timestamp(
							newEffectiveDate.getTime()),
					(newExpiredDate == null) ? null : new Timestamp(
							newExpiredDate.getTime()), newLicense
							.getGroupEnabled(),
					(newLicense.getGroupName() == null) ? null : newLicense
							.getGroupName().trim());

			if (!result) {
				returnUnexpectedError("create-new-form");
				return;
			}

			// assign projects to license
			if (inputProjectsForLicense != null) {
				result = LicenseProjectDBHelper.assignProjectsToLicense(
						newLicense.getName(), inputProjectsForLicense);
				if (!result) {
					returnUnexpectedError("create-new-form");
					return;
				}
			}

			// create new license group if applicable
			if (newLicense.getGroupEnabled() == 1) {
				try {
					result = ActiveDirectoryHelper.createADGroup(newLicense
							.getGroupName());
				} catch (NameAlreadyBoundException ne) {
					LicenseDBHelper.updateLicense(newLicense.getName().trim(),
							newLicense.getParties().trim(),
							(newEffectiveDate == null) ? null : new Timestamp(
									newEffectiveDate.getTime()),
							(newExpiredDate == null) ? null : new Timestamp(
									newExpiredDate.getTime()), 0, null);
					returnMessage("create-new-form",
							FacesMessage.SEVERITY_ERROR,
							"GROUP_NAME_EXIST_MSG", "GROUP_NAME_EXIST_DES");
					return;
				}
				if (!result) {
					returnUnexpectedError("create-new-form");
					return;
				}
			}

			// if the license exists in the AD, need to update the DB as well
			String filter = "(&(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")(" + newLicense.getCategory().trim() + "="
					+ newLicense.getName().trim() + "))";
			String[] attributesToFetch = { "sAMAccountName", "mail",
					"displayName", "distinguishedName" };
			if (ActiveDirectoryHelper.getSuggestedUsers(filter,
					attributesToFetch, 1).size() > 0) {
				LOG.info("ManageLicenseBean createNewLicense() Newly created license has already existed in the AD. Updating the DB");

				List<User> usersToPerform = ActiveDirectoryHelper
						.getUsersByLicense(newLicense, attributesToFetch);
				List<License> licenses = new ArrayList<License>();
				licenses.add(newLicense);
				result = UserLicenseDBHelper.assignLicensesToUsers(licenses,
						usersToPerform);

				if (!result) {
					returnUnexpectedError("create-new-form");
					return;
				}

				if (newLicense.getGroupEnabled() == 1) {
					List<ModificationItem> groupMods = new ArrayList<ModificationItem>();
					String groupName = newLicense.getGroupName();

					for (User user : usersToPerform) {
						LOG.info("ManageLicenseBean createNewLicense(): Adding "
								+ user.getAduser() + " to group " + groupName);

						groupMods
								.add(new ModificationItem(
										DirContext.ADD_ATTRIBUTE,
										new BasicAttribute(
												"member",
												user.getAttribute("distinguishedName"))));
					}

					String resultString = ActiveDirectoryHelper
							.modifyGroupAttribute(
									groupMods.toArray(new ModificationItem[0]),
									groupName);
					if (!resultString.equals("OK")) {
						returnUnexpectedError("create-new-form");
						return;
					}
				}
			}

			// hide dialog on success
			RequestContext.getCurrentInstance().execute(
					"PF('create-new-dialog').hide()");

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"LICENSE_SUCCESSFUL_MSG", "LICENSE_SUCCESSFUL_CREATE_DES");

			activeCount++;
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean createNewLicense(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
	}

	public void dummyListener() {

	}

	/**
	 * Set new license group name based on license name
	 */
	public void populateNewGroupName() {
		newLicense.setGroupName(newLicense.getName().replaceAll(
				"[^a-zA-Z0-9 -]", "_"));
	}

	/**
	 * Set edit license group name based on license name
	 */
	public void populateEditGroupName() {
		if (selectedLicense.getGroupName() == null
				|| selectedLicense.getGroupName().length() == 0) {
			selectedLicense.setGroupName(selectedLicense.getName().replaceAll(
					"[^a-zA-Z0-9 -]", "_"));
		}
	}

	/**
	 * Handle project filter when creating license
	 * 
	 * @param event
	 *            Filter event
	 */
	public void filterProjectCreateLicense(FilterEvent event) {
		LOG.info(event.getFilters().size());
	}

	/**
	 * Prepare data for viewing license details
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getLicenseDetails() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			usersOfSelectedLicense = new ArrayList<User>();

			LOG.info("Selected license: " + selectedLicenseValue);
			selectedLicense = LicenseDBHelper.getLicense(selectedLicenseValue);

			oldGroupFlag = selectedLicense.getGroupEnabled();
			editLicenseGroupEnabled = (selectedLicense.getGroupEnabled() == 0) ? false
					: true;

			selectedLicenseProjects.clear();
			for (Project p : LicenseProjectDBHelper
					.getProjectByLicense(selectedLicenseValue)) {
				selectedLicenseProjects.add(p.getName());
			}

			userByLicenseLazyList.setConstraintName("license");
			userByLicenseLazyList.setConstraintValue(selectedLicense.getName());

			RequestContext.getCurrentInstance().execute(
					"PF('view-license').show()");
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean getLicenseDetails(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Preparation for editing license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getEditLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			selectedLicense = LicenseDBHelper.getLicense(selectedLicenseValue);
			oldGroupFlag = selectedLicense.getGroupEnabled();
			editLicenseGroupEnabled = (selectedLicense.getGroupEnabled() == 0) ? false
					: true;
			if (editLicenseGroupEnabled) {
				editGroupNameDisabled = true;
			} else {
				editGroupNameDisabled = false;
			}
			newEffectiveDate = selectedLicense.getEffectiveDate();
			newExpiredDate = selectedLicense.getExpiration();
			selectedLicenseProjects = new ArrayList<String>();
			for (Project p : LicenseProjectDBHelper
					.getProjectByLicense(selectedLicense.getName())) {
				selectedLicenseProjects.add(p.getName());
			}
			inputProjectsForLicense = selectedLicenseProjects;
			toAssignProjectsInUpdate = new ArrayList<String>();
			toRemoveProjectsInUpdate = new ArrayList<String>();
		} catch (Exception e) {
			LOG.error("ManageLicenseBean getEditLicense(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Update license details
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			// check date
			LOG.info("ManageLicenseBean updateLicense(): Date entered "
					+ newEffectiveDate + " " + newExpiredDate);

			if (!(((newEffectiveDate != null) && (newExpiredDate != null)) || ((newEffectiveDate == null) && (newExpiredDate == null)))) {
				returnMessage("edit-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_INVALID_INPUT_MSG",
						"LICENSE_INVALID_DATE_EMPTY_DES");
				return;
			}

			if ((newEffectiveDate != null) && (newExpiredDate != null)
					&& (newExpiredDate.getTime() < newEffectiveDate.getTime())) {
				returnMessage("edit-form", FacesMessage.SEVERITY_ERROR,
						"LICENSE_INVALID_INPUT_MSG", "LICENSE_INVALID_DATE_DES");
				return;
			}

			selectedLicense.setGroupEnabled(editLicenseGroupEnabled ? 1 : 0);

			// update license details
			Boolean result = LicenseDBHelper.updateLicense(selectedLicense
					.getName().trim(), selectedLicense.getParties().trim(),
					(newEffectiveDate == null) ? null : new Timestamp(
							newEffectiveDate.getTime()),
					(newExpiredDate == null) ? null : new Timestamp(
							newExpiredDate.getTime()), selectedLicense
							.getGroupEnabled(), selectedLicense.getGroupName());
			if (!result) {
				returnUnexpectedError("edit-form");
				return;
			}

			// assign new projects
			result = LicenseProjectDBHelper.assignProjectsToLicense(
					selectedLicense.getName(), toAssignProjectsInUpdate);

			if (!result) {
				returnUnexpectedError("edit-form");
				return;
			}

			// remove unselected projects
			result = LicenseProjectDBHelper.removeProjectsFromLicense(
					selectedLicense.getName(), toRemoveProjectsInUpdate);

			if (!result) {
				returnUnexpectedError("edit-form");
				return;
			}

			// get user list that need to be removed
			List<Project> updatedProjectList = LicenseProjectDBHelper
					.getProjectByLicense(selectedLicense.getName());

			List<String> usersToRemove = new ArrayList<String>();

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

			if (updatedProjectList.size() > 0) {

				usersToRemove = UserLicenseDBHelper
						.usersToRemoveLicenseOnUpdatingLicenseProject(selectedLicense);

				// spawning tasks
				if (usersToRemove.size() > 0) {
					List<User> usersToRemoveFinal = ActiveDirectoryHelper
							.getUsers(usersToRemove.toArray(new String[0]),
									attributesToFetch.toArray(new String[0]));
					List<License> licenseList = new ArrayList<License>();
					licenseList.add(selectedLicense);

					// remove license from users
					AsyncTask licenseTask = new AssignLicensesToUsersTask(
							usersToRemoveFinal, null, licenseList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"UPDATE LICENSE'S PROJECTS");

					getTaskBean().executeLicenseTask(licenseTask);

					// remove users from license groups
					if (oldGroupFlag == 1) {
						AsyncTask task = new AssignUsersToGroupTask(
								usersToRemoveFinal, null, licenseList,
								userSessionBean.getUserName(),
								userSessionBean.getPassword(),
								"UPDATE LICENSE'S PROJECTS");

						getTaskBean().executeGroupTask(task);

					}
				} else {
					LOG.info("ManageLicenseBean updateLicense(): No users to remove license when updating license's project");
				}
			}

			// update license group
			if (oldGroupFlag == 0 && selectedLicense.getGroupEnabled() == 1) {
				LOG.info("ManageLicenseBean updateLicense() Enable license group for "
						+ selectedLicense.getName());
				try {
					result = ActiveDirectoryHelper
							.createADGroup(selectedLicense.getGroupName());
				} catch (NameAlreadyBoundException ne) {
					LicenseDBHelper.updateLicense(selectedLicense.getName()
							.trim(), selectedLicense.getParties().trim(),
							(newEffectiveDate == null) ? null : new Timestamp(
									newEffectiveDate.getTime()),
							(newExpiredDate == null) ? null : new Timestamp(
									newExpiredDate.getTime()), 0, null);
					returnMessage("edit-form", FacesMessage.SEVERITY_ERROR,
							"GROUP_NAME_EXIST_MSG", "GROUP_NAME_EXIST_DES");
					return;
				}

				if (!result) {
					returnUnexpectedError("edit-form");
					return;
				}

				// get all users of the license

				List<String> currentUserListString = UserLicenseDBHelper
						.getUserNameByLicense(selectedLicense.getName());

				currentUserListString.removeAll(usersToRemove);
				List<User> groupMembers = ActiveDirectoryHelper.getUsers(
						currentUserListString.toArray(new String[0]),
						attributesToFetch.toArray(new String[0]));

				if (groupMembers.size() > 0) {
					// add users to license groups
					List<License> licenseList = new ArrayList<License>();
					licenseList.add(selectedLicense);
					AsyncTask task = new AssignUsersToGroupTask(groupMembers,
							licenseList, null, userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"UPDATE LICENSE'S PROJECTS");

					getTaskBean().executeGroupTask(task);
				}

			}

			if (oldGroupFlag == 1 && selectedLicense.getGroupEnabled() == 0) {
				LOG.info("ManageLicenseBean updateLicense() Disable license group for "
						+ selectedLicense.getName());

				result = ActiveDirectoryHelper.deleteADGroup(
						selectedLicense.getGroupName(),
						userSessionBean.getUserName(),
						userSessionBean.getPassword());

				if (!result) {
					returnUnexpectedError("edit-form");
					return;
				}
			}

			oldGroupFlag = selectedLicense.getGroupEnabled();

			// hide dialog on success
			RequestContext.getCurrentInstance().execute(
					"PF('edit-license').hide()");

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"LICENSE_SUCCESSFUL_MSG", "LICENSE_SUCCESSFUL_UPDATE_DES");
		} catch (Exception e) {
			LOG.error("ManageLicenseBean updateLicense(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Return suggested project for license association
	 * 
	 * @param query
	 *            Partial name of the project
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of suggested projects
	 */
	public List<String> autoCompleteForLicenseProjectInput(String query) {
		List<String> results = new ArrayList<String>();
		try {
			List<Project> projectList = ProjectDBHelper
					.getProjectsWithLimitAndPartialName(15, query, 0);
			for (Project project : projectList) {
				if (!selectedLicenseProjects.contains(project.getName())) {
					results.add(project.getName());
				}
			}

		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean autoCompleteForLicenseProjectInput(): "
							+ e.getMessage(), e);
			returnUnexpectedError("bulk-update-license-form");
		}
		return results;
	}

	/**
	 * Executed when a project is selected for license association
	 * 
	 * @param event
	 *            Select event
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void onProjectForLicenseSelect(SelectEvent event) {
		try {
			String object = event.getObject().toString();
			selectedLicenseProjects.add(event.getObject().toString());
			if (!toAssignProjectsInUpdate.contains(object)) {
				toAssignProjectsInUpdate.add(object);
			}
			if (toRemoveProjectsInUpdate.contains(object)) {
				toRemoveProjectsInUpdate.remove(object);
			}
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean onProjectForLicenseSelect(): "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Executed when a project is unselected for license association - updating
	 * lists of projects to assign/remove
	 * 
	 * @param event
	 *            Unselect event
	 */
	public void onProjectForLicenseUnselect(UnselectEvent event) {
		try {
			String object = event.getObject().toString();
			selectedLicenseProjects.remove(event.getObject().toString());
			if (toAssignProjectsInUpdate.contains(object)) {
				toAssignProjectsInUpdate.remove(object);
			}
			if (!toRemoveProjectsInUpdate.contains(object)) {
				toRemoveProjectsInUpdate.add(object);
			}
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean onProjectForLicenseUnselect(): "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Delete licenses
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void deleteLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLicenseBean deleteLicense(): Deleting license "
					+ selectedLicense);
			// delete security group
			if (selectedLicense.getGroupEnabled() == 1) {
				Boolean result = ActiveDirectoryHelper.deleteADGroup(
						selectedLicense.getName(),
						userSessionBean.getUserName(),
						userSessionBean.getPassword());
				if (!result) {
					returnUnexpectedError(null);
					return;
				}
			}

			// delete license from database
			Boolean result = LicenseDBHelper.deleteLicense(selectedLicense
					.getName());

			if (!result) {
				returnUnexpectedError(null);
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"LICENSE_SUCCESSFUL_MSG",
						"LICENSE_SUCCESSFUL_DELETE_DES");
				disableDelete = true;
				inactiveCount--;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('delete-dialog').hide()");
			return;

		} catch (Exception e) {
			LOG.error("ManageLicenseBean deleteLicense(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}

	}

	/**
	 * Deactivate license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void deactivateLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLicenseBean deactivateLicense(): Deactivating license "
					+ selectedLicense);

			// remove group membership
			List<License> licenseToRemove = new ArrayList<License>();
			licenseToRemove.add(selectedLicense);
			String[] attributesToFetch = { "sAMAccountName", "mail",
					"displayName", "distinguishedName" };
			List<User> usersToRemove = ActiveDirectoryHelper.getUsersByLicense(
					selectedLicense, attributesToFetch);

			if (usersToRemove.size() > 0) {

				// remove license from its users
				AsyncTask licenseTask = new AssignLicensesToUsersTask(
						usersToRemove, null, licenseToRemove,
						userSessionBean.getUserName(),
						userSessionBean.getPassword(), "DEACTIVATE LICENSE");

				getTaskBean().executeLicenseTask(licenseTask);

				// remove license group from its members
				AsyncTask groupTask = new AssignUsersToGroupTask(usersToRemove,
						null, licenseToRemove, userSessionBean.getUserName(),
						userSessionBean.getPassword(), "DEACTIVATE LICENSE");

				getTaskBean().executeGroupTask(groupTask);
			} else {
				LOG.info("ManageLicenseBean() deactivateLicense(): No users to remove license");
			}

			// deactivating license in database
			Boolean result = LicenseDBHelper.toggleActivationLicense(
					selectedLicense.getName(), 1);

			if (!result) {
				returnUnexpectedError(null);
				return;
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"LICENSE_SUCCESSFUL_MSG",
						"LICENSE_SUCCESSFUL_DEACTIVATE_DES");
				disableDelete = true;
				activeCount--;
				inactiveCount++;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('deactivate-dialog').hide()");
			return;

		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean deactivateLicense(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}

	}

	/**
	 * Activate license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void activateLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LdapContext context = ActiveDirectoryHelper.getLDAPContext(
					userSessionBean.getUserName(),
					userSessionBean.getPassword());

			LOG.info("ManageLicenseBean activateLicense(): Activating license "
					+ selectedLicense);

			// activating license in database
			Boolean result = LicenseDBHelper.toggleActivationLicense(
					selectedLicense.getName(), 0);

			if (!result) {
				returnUnexpectedError(null);
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"LICENSE_SUCCESSFUL_MSG",
						"LICENSE_SUCCESSFUL_ACTIVATE_DES");
				disableDelete = true;
				inactiveCount--;
				activeCount++;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('activate-dialog').hide()");

			context.close();
			return;

		} catch (Exception e) {
			LOG.error("ManageLicenseBean activateLicense(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}

	}

	/**
	 * Prepare data for bulk update license
	 */
	public void getLicenseForBulkUpdate() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			selectedLicense = LicenseDBHelper.getLicense(selectedLicenseValue);
			selectedLicenseProjects = new ArrayList<String>();
			for (Project p : LicenseProjectDBHelper
					.getProjectByLicense(selectedLicense.getName())) {
				selectedLicenseProjects.add(p.getName());
			}

			inputUsers = new ArrayList<String>();
			inputProjects = new ArrayList<String>();

			temporaryInputProjects = new ArrayList<String>();
			temporaryInputUsers = new ArrayList<String>();
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean getLicenseForBulkUpdate(): "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Clear inputs when bulk update target changed
	 */
	public void changeActionListener() {
		if (inputProjects != null) {
			inputProjects.clear();
		} else {
			inputProjects = new ArrayList<String>();
		}
		if (inputUsers != null) {
			inputUsers.clear();
		} else {
			inputUsers = new ArrayList<String>();
		}
	}

	/**
	 * Execute when a user is selected for bulk assignment - updating user list
	 * 
	 * @param event
	 *            Select event
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void onUserSelect(SelectEvent event) {
		try {
			temporaryInputUsers.add(event.getObject().toString());
		} catch (Exception e) {
			LOG.error("ManageLicenseBean onUserSelect(): " + e.getMessage(), e);
		}
	}

	/**
	 * Execute when a project is selected for bulk assignment - updating project
	 * list
	 * 
	 * @param event
	 *            Select event
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void onProjectSelect(SelectEvent event) {
		try {
			temporaryInputProjects.add(event.getObject().toString());
		} catch (Exception e) {
			LOG.error("ManageLicenseBean onProjectSelect(): " + e.getMessage(),
					e);
		}
	}

	/**
	 * Execute when a user is unselected for bulk assignment - updating user
	 * list
	 * 
	 * @param event
	 *            Unselect event
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void onUserUnselect(UnselectEvent event) {
		try {
			temporaryInputUsers.remove(event.getObject().toString());
		} catch (Exception e) {
			LOG.error("ManageLicenseBean onUserUnselect(): " + e.getMessage(),
					e);
		}
	}

	/**
	 * Execute when a project is unselected for bulk assignment - updating
	 * project list
	 * 
	 * @param event
	 *            Unselect event
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void onProjectUnselect(UnselectEvent event) {
		try {
			temporaryInputProjects.remove(event.getObject().toString());
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean onProjectUnselect(): " + e.getMessage(),
					e);
		}
	}

	/**
	 * Get suggested user list when bulk assigning license
	 * 
	 * @param query
	 *            Partial user sAMAccountName
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of user names
	 */
	public List<String> autoCompleteForUserInput(String query) {
		List<String> results = new ArrayList<String>();
		try {
			String filter = "(&(sAMAccountName=" + query
					+ "*)(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")";

			// add project constraint

			if (!selectedLicenseProjects.isEmpty()) {
				String projectConstraint = "(|";
				for (String project : selectedLicenseProjects) {
					projectConstraint += "("
							+ PropertyLoader.bAESProperties.getProperty("pa")
							+ "=" + project + ")";
				}

				projectConstraint += ")";
				filter += projectConstraint;
			}

			if (bulkAction.equals("assign")) {
				filter += "(!(" + selectedLicense.getCategory() + "="
						+ selectedLicenseValue + ")))";
			} else {
				filter += "(" + selectedLicense.getCategory() + "="
						+ selectedLicenseValue + "))";
			}

			// get auto complete list
			String[] selection = { "sAMAccountName" };
			List<User> userList = ActiveDirectoryHelper.getUsers(filter,
					selection, 10);
			for (User user : userList) {
				String userToAdd = user.getAttribute("sAMAccountName");
				if (!temporaryInputUsers.contains(userToAdd)) {
					results.add(userToAdd);
				}
			}
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean autoCompleteForUserInput(): "
							+ e.getMessage(), e);
			returnUnexpectedError("bulk-update-license-form");
		}
		return results;
	}

	/**
	 * Get suggested project list when bulk assigning license
	 * 
	 * @param query
	 *            Partial project name
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return List of project names
	 */
	public List<String> autoCompleteForProjectInput(String query) {
		List<String> results = new ArrayList<String>();
		try {
			if (!selectedLicenseProjects.isEmpty()) {
				for (String project : selectedLicenseProjects) {
					results.add(project);
				}
			} else {
				List<Project> projectList = ProjectDBHelper
						.getProjectsWithLimitAndPartialName(10, query, 0);
				for (Project project : projectList) {
					if (!temporaryInputProjects.contains(project.getName())) {
						results.add(project.getName());
					}
				}
			}

		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean autoCompleteForProjectInput(): "
							+ e.getMessage(), e);
			returnUnexpectedError("bulk-update-license-form");
		}
		return results;
	}

	/**
	 * Bulk grant/retract license method
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void bulkGrantRetractLicense() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			List<FacesMessage> warningMessages = new ArrayList<FacesMessage>();
			List<User> usersToPerform = new ArrayList<User>();
			List<License> licenseList = new ArrayList<License>();
			licenseList.add(selectedLicense);

			// use set to prevent duplicate
			Set<String> finalUserListString = new HashSet<String>();

			if (inputMethod.equals("users-by-project")) {
				if ((inputProjects == null || inputProjects.isEmpty())) {
					LOG.info("ManageLicenseBean bulkGrantRetractLicense(): Empty project input");
					returnMessage("bulk-update-license-form",
							FacesMessage.SEVERITY_ERROR,
							"LICENSE_INVALID_INPUT_MSG",
							"LICENSE_BULK_ASSIGNMENT_PROJECT_EMPTY_DES");
					return;
				}

				for (String project : inputProjects) {
					for (User user : UserProjectDBHelper
							.getUserListByProject(project)) {
						// add all members of the project to the final list
						finalUserListString.add(user.getAduser());
					}
				}
			}

			if (inputMethod.equals("users")) {
				if ((inputUsers == null || inputUsers.isEmpty())) {
					LOG.info("ManageLicenseBean bulkGrantRetractLicense(): Empty user input");
					returnMessage("bulk-update-license-form",
							FacesMessage.SEVERITY_ERROR,
							"LICENSE_INVALID_INPUT_MSG",
							"LICENSE_BULK_ASSIGNMENT_USER_EMPTY_DES");
					return;
				}

				for (String user : inputUsers) {
					finalUserListString.add(user);
				}
			}

			LOG.info("ManageLicenseBean bulkGrantRetractLicense(): Final user list size :"
					+ finalUserListString.size());
			String[] attributesToFetch = { selectedLicense.getCategory(),
					"sAMAccountName", "displayName", "mail",
					"userPrincipalName", "distinguishedName",
					PropertyLoader.bAESProperties.getProperty("pa") };
			List<User> finalUserList = ActiveDirectoryHelper.getUsers(
					finalUserListString.toArray(new String[0]),
					attributesToFetch);

			for (User user : finalUserList) {
				Attributes attributes = user.getAttributes();
				Attribute currentLicenseAttribute = attributes
						.get(selectedLicense.getCategory());

				if (bulkAction.equals("assign")) {

					// if only user doesn't have the license
					if (currentLicenseAttribute == null
							|| !currentLicenseAttribute
									.contains(selectedLicense.getName().trim())) {
						usersToPerform.add(new User(attributes));

					} else {
						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAduser() + " has already had "
										+ selectedLicense.getName()
										+ " license"));
					}
				} else {

					// if only user has the license
					if (currentLicenseAttribute != null
							&& currentLicenseAttribute.contains(selectedLicense
									.getName().trim())) {
						usersToPerform.add(new User(attributes));
					} else {
						// skip the user
						warningMessages.add(new FacesMessage(
								FacesMessage.SEVERITY_WARN,
								PropertyLoader.bAESConstant
										.getProperty("USER_SKIP_WARNING_MSG"),
								user.getAduser() + " does not have "
										+ selectedLicense.getName()
										+ " license"));
					}
				}
			}

			// spawning tasks
			if (usersToPerform.size() > 0) {
				if (bulkAction.equals("assign")) {
					// assign license to selected users
					AsyncTask licenseTask = new AssignLicensesToUsersTask(
							usersToPerform, licenseList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeLicenseTask(licenseTask);

					// assign license groupt to selected users
					AsyncTask groupTask = new AssignUsersToGroupTask(
							usersToPerform, licenseList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeGroupTask(groupTask);
				} else {
					// remove license from selected users
					AsyncTask licenseTask = new AssignLicensesToUsersTask(
							usersToPerform, null, licenseList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeLicenseTask(licenseTask);

					// remove license group from selected users
					AsyncTask groupTask = new AssignUsersToGroupTask(
							usersToPerform, null, licenseList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN LICENSE TO MULTIPLE USERS");

					getTaskBean().executeGroupTask(groupTask);
				}
			} else {
				LOG.info("ManageLicenseBean bulkGrantRetractLicense(): No user to update license");
			}

			RequestContext.getCurrentInstance().execute(
					"PF('bulk-update-dialog').hide()");

			if (warningMessages.isEmpty()) {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"LICENSE_SUCCESSFUL_MSG", "LICENSE_SUCCESSFUL_"
								+ bulkAction.toUpperCase() + "_DES");
			} else {
				for (FacesMessage message : warningMessages) {
					FacesContext.getCurrentInstance().addMessage(null, message);
				}
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"LICENSE_SUCCESSFUL_MSG", "LICENSE_SUCCESSFUL_"
								+ bulkAction.toUpperCase() + "_WARNING_DES");
			}

			inputUsers = new ArrayList<String>();
			inputProjects = new ArrayList<String>();
		} catch (Exception e) {
			LOG.error(
					"ManageLicenseBean bulkGrantRetractLicense(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}

	}

	/**
	 * Export license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void handleExport() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLicenseBean handleExport(): Export file");
			InputStream exportStream = LicenseDBHelper.exportLicense(
					selectedColumns, exportFileType);
			if (exportStream == null) {
				returnUnexpectedError(null);
				return;
			}
			exportFile = new DefaultStreamedContent(exportStream, "text/plain",
					"licenses." + exportFileType);

		} catch (Exception e) {
			LOG.error("ManageLicenseBean handleExport(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Upload file
	 * 
	 * @param event
	 *            FileUploadEvent
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void uploadFile(FileUploadEvent event) {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			importFile = event.getFile();
			FacesContext.getCurrentInstance()
					.addMessage(
							"import-form",
							new FacesMessage(FacesMessage.SEVERITY_INFO,
									"File Uploaded",
									"Click Import to import the file"));
		} catch (Exception e) {
			LOG.error("ManageLicenseBean uploadFile(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Import license
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void handleImport() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLicenseBean handleImport(): Import file");
			String content = new String(importFile.getContents(), "UTF-8");
			LOG.info("ManageLicenseBean handleImport(): Content Type: "
					+ importFile.getContentType());
			Boolean result = LicenseDBHelper.importLicense(importFileType,
					content);
			if (!result) {
				returnUnexpectedError("import-form");
				return;
			}
			// hide dialog on success
			RequestContext.getCurrentInstance().execute(
					"PF('import-license').hide()");

			FacesContext.getCurrentInstance().addMessage(
					null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Successful",
							"Licenses have been imported"));

		} catch (Exception e) {
			LOG.error("ManageLicenseBean handleImport(): " + e.getMessage(), e);
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

	/**
	 * Redirect to user detail page upon clicking on user
	 * 
	 * @param user
	 *            User sAMAccountName
	 * 
	 * @return manage user detail page (redirected)
	 */
	public String getUserDetails(String user) {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
		}

		FacesContext.getCurrentInstance().getExternalContext().getFlash()
				.put("selectedUser", "sAMAccountName=" + user);
		return "manageUserDetails?faces-redirect=true";
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

	public List<License> getSelectedLicenses() {
		return selectedLicenses;
	}

	public void setSelectedLicenses(List<License> selectedLicenses) {
		this.selectedLicenses = selectedLicenses;
	}

	public List<License> getLicenseList() {
		return licenseList;
	}

	public void setLicenseList(List<License> licenseList) {
		this.licenseList = licenseList;
	}

	public String getSelectedLicenseValue() {
		return selectedLicenseValue;
	}

	public void setSelectedLicenseValue(String selectedLicenseValue) {
		this.selectedLicenseValue = selectedLicenseValue;
	}

	public License getSelectedLicense() {
		return selectedLicense;
	}

	public void setSelectedLicense(License selectedLicense) {
		this.selectedLicense = selectedLicense;
	}

	public List<User> getUsersOfSelectedLicense() {
		return usersOfSelectedLicense;
	}

	public void setUsersOfSelectedLicense(List<User> usersOfSelectedLicense) {
		this.usersOfSelectedLicense = usersOfSelectedLicense;
	}

	public boolean getDisableDelete() {
		return disableDelete;
	}

	public void setDisableDelete(boolean disableDelete) {
		this.disableDelete = disableDelete;
	}

	public License getNewLicense() {
		return newLicense;
	}

	public void setNewLicense(License newLicense) {
		this.newLicense = newLicense;
	}

	public Date getNewEffectiveDate() {
		return newEffectiveDate;
	}

	public void setNewEffectiveDate(Date newEffectiveDate) {
		this.newEffectiveDate = newEffectiveDate;
	}

	public Date getNewExpiredDate() {
		return newExpiredDate;
	}

	public void setNewExpiredDate(Date newExpiredDate) {
		this.newExpiredDate = newExpiredDate;
	}

	public LazyDataModel<License> getLicenseLazyList() {
		return licenseLazyList;
	}

	public void setLicenseLazyList(LazyLicenseDataModel licenseLazyList) {
		this.licenseLazyList = licenseLazyList;
	}

	public List<String> getInputUsers() {
		return inputUsers;
	}

	public void setInputUsers(List<String> inputUsers) {
		this.inputUsers = inputUsers;
	}

	public String getInputMethod() {
		return inputMethod;
	}

	public void setInputMethod(String inputMethod) {
		this.inputMethod = inputMethod;
	}

	public String getBulkAction() {
		return bulkAction;
	}

	public void setBulkAction(String bulkAction) {
		this.bulkAction = bulkAction;
	}

	public List<String> getInputProjects() {
		return inputProjects;
	}

	public void setInputProjects(List<String> inputProjects) {
		this.inputProjects = inputProjects;
	}

	public String getConfirmedNewLicenseName() {
		return confirmedNewLicenseName;
	}

	public void setConfirmedNewLicenseName(String confirmedNewLicenseName) {
		this.confirmedNewLicenseName = confirmedNewLicenseName;
	}

	public List<String> getTemporaryInputUsers() {
		return temporaryInputUsers;
	}

	public void setTemporaryInputUsers(List<String> temporaryInputUsers) {
		this.temporaryInputUsers = temporaryInputUsers;
	}

	public List<String> getTemporaryInputProjects() {
		return temporaryInputProjects;
	}

	public void setTemporaryInputProjects(List<String> temporaryInputProjects) {
		this.temporaryInputProjects = temporaryInputProjects;
	}

	public List<Project> getCreateLicenseProjectInput() {
		return createLicenseProjectInput;
	}

	public void setCreateLicenseProjectInput(
			List<Project> createLicenseProjectInput) {
		this.createLicenseProjectInput = createLicenseProjectInput;
	}

	public StreamedContent getExportFile() {
		return exportFile;
	}

	public void setExportFile(StreamedContent exportFile) {
		this.exportFile = exportFile;
	}

	public String getExportFileType() {
		return exportFileType;
	}

	public void setExportFileType(String exportFileType) {
		this.exportFileType = exportFileType;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	public List<String> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public UploadedFile getImportFile() {
		return importFile;
	}

	public void setImportFile(UploadedFile importFile) {
		this.importFile = importFile;
	}

	public String getImportFileType() {
		return importFileType;
	}

	public void setImportFileType(String importFileType) {
		this.importFileType = importFileType;
	}

	public int getShowDeactivation() {
		return showDeactivation;
	}

	public void setShowDeactivation(int showDeactivation) {
		this.showDeactivation = showDeactivation;
	}

	public List<String> getInputProjectsForLicense() {
		return inputProjectsForLicense;
	}

	public void setInputProjectsForLicense(List<String> inputProjectsForLicense) {
		this.inputProjectsForLicense = inputProjectsForLicense;
	}

	public List<String> getSelectedLicenseProjects() {
		return selectedLicenseProjects;
	}

	public void setSelectedLicenseProjects(List<String> selectedLicenseProjects) {
		this.selectedLicenseProjects = selectedLicenseProjects;
	}

	public List<String> getToAssignProjectsInUpdate() {
		return toAssignProjectsInUpdate;
	}

	public void setToAssignProjectsInUpdate(
			List<String> toAssignProjectsInUpdate) {
		this.toAssignProjectsInUpdate = toAssignProjectsInUpdate;
	}

	public List<String> getToRemoveProjectsInUpdate() {
		return toRemoveProjectsInUpdate;
	}

	public void setToRemoveProjectsInUpdate(
			List<String> toRemoveProjectsInUpdate) {
		this.toRemoveProjectsInUpdate = toRemoveProjectsInUpdate;
	}

	public int getActiveCount() {
		return activeCount;
	}

	public void setActiveCount(int activeCount) {
		this.activeCount = activeCount;
	}

	public int getInactiveCount() {
		return inactiveCount;
	}

	public void setInactiveCount(int inactiveCount) {
		this.inactiveCount = inactiveCount;
	}

	public LazyDataModel<User> getUserByLicenseLazyList() {
		return userByLicenseLazyList;
	}

	public void setUserByLicenseLazyList(LazyUserDataModel userByLicenseLazyList) {
		this.userByLicenseLazyList = userByLicenseLazyList;
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

	public boolean isNewLicenseGroupEnabled() {
		return newLicenseGroupEnabled;
	}

	public void setNewLicenseGroupEnabled(boolean newLicenseGroupEnabled) {
		this.newLicenseGroupEnabled = newLicenseGroupEnabled;
	}

	public boolean isEditLicenseGroupEnabled() {
		return editLicenseGroupEnabled;
	}

	public void setEditLicenseGroupEnabled(boolean editLicenseGroupEnabled) {
		this.editLicenseGroupEnabled = editLicenseGroupEnabled;
	}

	public boolean isEditGroupNameDisabled() {
		return editGroupNameDisabled;
	}

	public void setEditGroupNameDisabled(boolean editGroupNameDisabled) {
		this.editGroupNameDisabled = editGroupNameDisabled;
	}

}
