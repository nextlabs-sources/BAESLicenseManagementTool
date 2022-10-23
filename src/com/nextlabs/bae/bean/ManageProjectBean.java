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
import javax.faces.context.FacesContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.event.data.FilterEvent;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;
import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.helper.UserProjectDBHelper;
import com.nextlabs.bae.model.LazyLicenseDataModel;
import com.nextlabs.bae.model.LazyProjectDataModel;
import com.nextlabs.bae.model.LazyUserDataModel;
import com.nextlabs.bae.task.AssignLicensesToUsersTask;
import com.nextlabs.bae.task.AssignProjectsToUsersTask;
import com.nextlabs.bae.task.AssignUsersToGroupTask;
import com.nextlabs.bae.task.AsyncTask;

@ManagedBean(name = "manageProjectBean")
@ViewScoped
public class ManageProjectBean implements Serializable {
	private static final Log LOG = LogFactory.getLog(ManageProjectBean.class);
	private static final long serialVersionUID = 1L;

	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	transient private ManageExecutorServiceBean taskBean;
	private LazyProjectDataModel projectLazyList;
	private Project selectedProject;
	private String selectedProjectValue;
	private List<Project> selectedProjects;
	private boolean disableDelete;
	private Project newProject;
	private List<User> usersOfSelectedProject;
	private List<License> licensesOfSelectedProject;
	private LazyUserDataModel userByProjectLazyList;
	private LazyLicenseDataModel lazyLicensesOfSelectedProject;
	private List<String> inputUsers;
	private String pSelectedProject;
	private String bulkAction;
	private List<String> temporaryInputUsers;
	private int showDeactivation;
	private int showLicenseDeactivation;
	private int activeCount;
	private int inactiveCount;
	private boolean assignLicensesWithProject;
	private List<User> usersToPerform;

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
			showLicenseDeactivation = 0;
			projectLazyList = new LazyProjectDataModel(showDeactivation);
			inputUsers = new ArrayList<String>();
			selectedProjects = new ArrayList<Project>();
			newProject = new Project();
			disableDelete = true;
			bulkAction = "assign";
			activeCount = ProjectDBHelper.countAllProject(null, 0);
			inactiveCount = ProjectDBHelper.countAllProject(null, 1);
			assignLicensesWithProject = true;
			lazyLicensesOfSelectedProject = new LazyLicenseDataModel(0);
			userByProjectLazyList = new LazyUserDataModel();
			selectedProjects = new ArrayList<Project>();
		} catch (Exception e) {
			LOG.error("ManageProjectBean init(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
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
			LOG.error("ManageProjectBean unauthorize(): " + e.getMessage(), e);
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

		projectLazyList = new LazyProjectDataModel(showDeactivation);
		if (selectedProjects != null) {
			selectedProjects.clear();
		} else {
			selectedProjects = new ArrayList<Project>();
		}
		disableDelete = true;
	}

	/**
	 * Executed when project list is filtered
	 * 
	 * @param event
	 *            FilterEvent
	 */
	public void onFilter(FilterEvent event) {
		if (selectedProjects != null) {
			selectedProjects.clear();
		} else {
			selectedProjects = new ArrayList<Project>();
		}
		disableDelete = true;
	}

	/**
	 * Create new project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void createNewProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageProjectBean createNewProject(): FCreating new project");

			// check unique
			if (ProjectDBHelper.getProject(newProject.getName().trim()) != null) {
				returnMessage("create-new-form", FacesMessage.SEVERITY_ERROR,
						"PROJECT_DUPLICATE_MSG", "PROJECT_DUPLICATE_DES");
				return;
			}

			// add to database
			Boolean result = ProjectDBHelper.createNewProject(newProject
					.getName().trim(), newProject.getDescription().trim());

			if (!result) {
				returnUnexpectedError("create-new-form");
				return;
			}

			String filter = "(&(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")(" + PropertyLoader.bAESProperties.getProperty("pa")
					+ "=" + newProject.getName().trim() + "))";
			String[] attributesToFetch = { "sAMAccountName", "mail",
					"displayName", "distinguishedName" };
			if (ActiveDirectoryHelper.getSuggestedUsers(filter,
					attributesToFetch, 1).size() > 0) {
				LOG.info("ManageProjectBean createNewProject() Newly created project has already existed in the AD. Updating the DB");

				List<User> usersToPerform = ActiveDirectoryHelper
						.getUsersByProject(newProject.getName(),
								attributesToFetch);
				List<Project> projects = new ArrayList<Project>();
				projects.add(newProject);
				result = UserProjectDBHelper.assignProjectsToUsers(projects,
						usersToPerform);

				if (!result) {
					returnUnexpectedError("create-new-form");
					return;
				}
			}

			// hide dialog on success
			RequestContext.getCurrentInstance().execute(
					"PF('create-new-dialog').hide()");

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"PROJECT_SUCCESSFUL_MSG", "PROJECT_SUCCESSFUL_CREATE_DES");

			activeCount++;
		} catch (Exception e) {
			LOG.error(
					"ManageProjectBean createNewProject(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Prepare data for viewing a project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getProjectDetails() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			usersOfSelectedProject = new ArrayList<User>();

			LOG.info("ManageProjectBean getProjectDetails(): Selected project: "
					+ selectedProjectValue);
			selectedProject = ProjectDBHelper.getProject(selectedProjectValue);
			LdapContext context = ActiveDirectoryHelper.getLDAPContext(
					userSessionBean.getUserName(),
					userSessionBean.getPassword());

			lazyLicensesOfSelectedProject.setConstraintName("project");
			lazyLicensesOfSelectedProject
					.setConstraintValue(selectedProjectValue);

			userByProjectLazyList.setConstraintName("project");
			userByProjectLazyList.setConstraintValue(selectedProject.getName());

			RequestContext.getCurrentInstance().execute(
					"PF('view-project').show()");

			context.close();
		} catch (Exception e) {
			LOG.error(
					"ManageProjectBean getProjectDetails(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Update license list of a selected project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateProjectLicenseList() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			licensesOfSelectedProject = LicenseProjectDBHelper
					.getLicenseByProject(selectedProjectValue,
							showLicenseDeactivation);
			lazyLicensesOfSelectedProject = new LazyLicenseDataModel("project",
					selectedProject.getName(), showLicenseDeactivation);
		} catch (Exception e) {
			LOG.error(
					"ManageProjectBean updateProjectLicenseList(): "
							+ e.getMessage(), e);
		}
	}

	/**
	 * Get selected project from database
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getEditProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageProjectBean getEditProject(): Selected project: "
					+ selectedProjectValue);
			selectedProject = ProjectDBHelper.getProject(selectedProjectValue);
			pSelectedProject = selectedProjectValue;
		} catch (Exception e) {
			LOG.error("ManageProjectBean getEditProject(): " + e.getMessage(),
					e);
		}
	}

	/**
	 * Delete project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void deleteProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageProjectBean deleteProject(): Deleting project "
					+ selectedProject.getName());

			// delete project from database - all relationships will be cascaded
			Boolean result = ProjectDBHelper.deleteProject(selectedProject
					.getName());

			if (!result) {
				returnUnexpectedError(null);
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"PROJECT_SUCCESSFUL_MSG",
						"PROJECT_SUCCESSFUL_DELETE_DES");
				disableDelete = true;
				inactiveCount--;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('delete-dialog').hide()");
			return;

		} catch (Exception e) {
			LOG.error("ManageProjectBean deleteProject(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Deactivate project
	 * 
	 * @exception Exception
	 *                Any exception
	 */

	public void deactivateProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LdapContext context = ActiveDirectoryHelper.getLDAPContext(
					userSessionBean.getUserName(),
					userSessionBean.getPassword());

			LOG.info("ManageProjectBean deactivateProject(): ManageProjectBean deactivateProject(): Deactivate project "
					+ selectedProject.getName());

			// validate project's licenses currently assigned to users
			// get users who need to be removed
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

			usersToPerform = ActiveDirectoryHelper.getUsersByProject(
					selectedProject.getName(),
					attributesToFetch.toArray(new String[0]));

			// may need to spawn a lot of tasks - execute asynchronously
			getTaskBean().executeSpawningTask(new Runnable() {

				@Override
				public void run() {

					List<Project> projectList = new ArrayList<Project>();
					projectList.add(selectedProject);

					for (User user : usersToPerform) {
						// remove licenses of the project from user if user has
						// the
						// license and user does not have any license's projects
						List<License> licensesToRemove = UserLicenseDBHelper
								.licensesToRemoveOnDeactivatingProject(user,
										selectedProject.getName());

						List<User> userList = new ArrayList<User>();
						userList.add(user);

						LOG.info("ManageProjectBean deactivateProject(): Removing license due to deactivating project: "
								+ user.getAduser()
								+ " - "
								+ licensesToRemove.size());

						// spawning tasks
						if (licensesToRemove.size() > 0) {

							// remove license from users
							AsyncTask licenseTask = new AssignLicensesToUsersTask(
									userList, null, licensesToRemove,
									userSessionBean.getUserName(),
									userSessionBean.getPassword(),
									"DEACTIVATE PROJECT");

							getTaskBean().executeLicenseTask(licenseTask);

							// remove license group from users
							AsyncTask groupTask = new AssignUsersToGroupTask(
									userList, null, licensesToRemove,
									userSessionBean.getUserName(),
									userSessionBean.getPassword(),
									"DEACTIVATE PROJECT");

							getTaskBean().executeGroupTask(groupTask);
						}
					}

					// remove license_project relationship
					LicenseProjectDBHelper
							.removeAllProjectRelationship(selectedProject
									.getName());
				}
			});

			// remove project from users
			List<Project> projectList = new ArrayList<Project>();
			projectList.add(selectedProject);
			if (usersToPerform.size() > 0) {
				AsyncTask projectTask = new AssignProjectsToUsersTask(
						usersToPerform, null, projectList,
						userSessionBean.getUserName(),
						userSessionBean.getPassword(), "DEACTIVATE PROJECT");
				getTaskBean().executeProjectTask(projectTask);
			} else {
				LOG.info("ManageProjectBean deactivateProject(): No users to remove");
			}

			// deactivate project in database
			Boolean result = ProjectDBHelper.toggleDeactivationProject(
					selectedProject.getName(), 1);

			if (!result) {
				returnUnexpectedError(null);
				RequestContext.getCurrentInstance().execute(
						"PF('deactivate-dialog').hide()");
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"PROJECT_SUCCESSFUL_MSG",
						"PROJECT_SUCCESSFUL_DEACTIVATE_DES");
				disableDelete = true;
				activeCount--;
				inactiveCount++;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('deactivate-dialog').hide()");

			context.close();
			return;

		} catch (Exception e) {
			LOG.error(
					"ManageProjectBean deactivateProject(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
			RequestContext.getCurrentInstance().execute(
					"PF('deactivate-dialog').hide()");
		}
	}

	/**
	 * Activate project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void activateProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageProjectBean activateProject(): Activating project "
					+ selectedProject.getName());

			// deactivating project in database
			Boolean result = ProjectDBHelper.toggleDeactivationProject(
					selectedProject.getName(), 0);

			if (!result) {
				returnUnexpectedError(null);
			} else {
				returnMessage(null, FacesMessage.SEVERITY_INFO,
						"PROJECT_SUCCESSFUL_MSG",
						"PROJECT_SUCCESSFUL_ACTIVATE_DES");
				disableDelete = true;
				activeCount++;
				inactiveCount--;
			}
			RequestContext.getCurrentInstance().execute(
					"PF('activate-dialog').hide()");

			return;
		} catch (Exception e) {
			LOG.error("ManageProjectBean activateProject(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
			RequestContext.getCurrentInstance().execute(
					"PF('activate-dialog').hide()");
		}
	}

	/*
	 * Executed when toggling selection on project table
	 * 
	 * @apram event ToggleSelectEvent
	 */
	public void onToggleSelect(ToggleSelectEvent event) {
		if (selectedProjects == null || selectedProjects.isEmpty()) {
			disableDelete = true;
		} else {
			disableDelete = false;
		}
	}

	/**
	 * Executed when unselecting a project row
	 * 
	 * @param event
	 *            Unselect event
	 */
	public void onRowUnselect(UnselectEvent event) {
		if (selectedProjects == null || selectedProjects.isEmpty()) {
			disableDelete = true;
		}
	}

	/*
	 * Executed when selecting a project row
	 * 
	 * @param event Select event
	 */
	public void onRowSelect(SelectEvent event) {
		disableDelete = false;
	}

	/*
	 * Edit project
	 * 
	 * @exception Any exception
	 */
	public void updateProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			// check unique
			if (ProjectDBHelper.getProject(selectedProject.getName().trim()) != null
					&& !selectedProject.getName().trim()
							.equals(pSelectedProject.trim())) {
				returnMessage("edit-form", FacesMessage.SEVERITY_ERROR,
						"PROJECT_DUPLICATE_MSG", "PROJECT_DUPLICATE_DES");
				return;
			}

			Boolean result = ProjectDBHelper.updateProject(selectedProject
					.getName().trim(), selectedProject.getDescription().trim());

			if (!result) {
				returnUnexpectedError("edit-form");
				return;
			}

			// hide dialog on success
			RequestContext.getCurrentInstance().execute(
					"PF('edit-project').hide()");

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"PROJECT_SUCCESSFUL_MSG", "PROJECT_SUCCESSFUL_UPDATE_DES");
		} catch (Exception e) {
			LOG.error("ManageProjectBean updateProject(): " + e.getMessage());
			returnUnexpectedError(null);
		}
	}

	/**
	 * Get project from database for bulk assignment
	 */
	public void getProjectForBulkUpdate() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		selectedProject = ProjectDBHelper.getProject(selectedProjectValue);
		temporaryInputUsers = new ArrayList<String>();
	}

	/**
	 * Executed when bulk action got changed
	 */
	public void changeActionListener() {
		if (inputUsers != null) {
			inputUsers.clear();
		} else {
			inputUsers = new ArrayList<String>();
		}
	}

	/**
	 * Get suggested list for users
	 * 
	 * @param query
	 *            Partial sAMAccountName
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return User list
	 */
	public List<String> autoCompleteForUserInput(String query) {
		List<String> results = new ArrayList<String>();
		try {
			String filter;
			LOG.info("ManageProjectBean autoCompleteForUserInput(): Bulk action "
					+ bulkAction);
			if (bulkAction.equals("assign")) {
				filter = "(&(sAMAccountName="
						+ query
						+ "*)(objectClass=user)(mail=*"
						+ PropertyLoader.bAESProperties
								.getProperty("email-filter") + ")(!("
						+ PropertyLoader.bAESProperties.getProperty("pa") + "="
						+ selectedProjectValue + ")))";
			} else {
				filter = "(&(sAMAccountName="
						+ query
						+ "*)(objectClass=user)(mail=*"
						+ PropertyLoader.bAESProperties
								.getProperty("email-filter") + ")("
						+ PropertyLoader.bAESProperties.getProperty("pa") + "="
						+ selectedProjectValue + "))";
			}

			// get auto complete list
			List<User> userList = ActiveDirectoryHelper.getUsers(filter, null,
					10);
			for (User user : userList) {
				String userToAdd = user.getAttribute("sAMAccountName");
				if (!temporaryInputUsers.contains(userToAdd)) {
					results.add(userToAdd);
				}
			}
		} catch (Exception e) {
			LOG.error(
					"ManageProjectBean autoCompleteForUserInput(): "
							+ e.getMessage(), e);
			returnUnexpectedError(null);
		}
		return results;
	}

	/**
	 * Executed when a user is selected during project bulk assignment
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
			LOG.error("ManageProjectBean onUserSelect(): " + e.getMessage(), e);
		}
	}

	/**
	 * Executed when a user is unselected during project bulk assignment
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
			LOG.error("ManageProjectBean onUserUnselect(): " + e.getMessage(),
					e);
		}
	}

	/**
	 * Bulk assign/retract project
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void bulkAssignRetractProject() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			usersToPerform = new ArrayList<User>();
			List<Project> projectList = new ArrayList<Project>();
			projectList.add(selectedProject);

			if ((inputUsers == null || inputUsers.isEmpty())) {
				LOG.info("ManageProjectBean bulkAssignRetractProject(): Empty user input");
				returnMessage("bulk-update-project-form",
						FacesMessage.SEVERITY_ERROR,
						"PROJECT_INVALID_INPUT_MSG",
						"PROJECT_BULK_ASSIGNMENT_USER_EMPTY_DES");
			}

			// build user to perform list
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

			usersToPerform = ActiveDirectoryHelper.getUsers(
					inputUsers.toArray(new String[0]),
					attributesToFetch.toArray(new String[0]));

			// may need to spawn a lot of tasks - execute asynchronously
			getTaskBean().executeSpawningTask(new Runnable() {
				@Override
				public void run() {
					List<Project> projectList = new ArrayList<Project>();
					projectList.add(selectedProject);

					for (User user : usersToPerform) {

						// modify ad
						if (bulkAction.equals("assign")) {
							List<License> licensesToAdd = new ArrayList<License>();
							if (assignLicensesWithProject) {
								List<License> licenseOfAssignedProject = LicenseProjectDBHelper
										.getLicenseByProject(
												selectedProject.getName(), 0);

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
										licensesToAdd.add(license);
									}
								}

								List<User> userList = new ArrayList<User>();
								userList.add(user);

								// spawning tasks
								if (licensesToAdd.size() > 0) {
									// assign licenses to user
									AsyncTask licenseTask = new AssignLicensesToUsersTask(
											userList, licensesToAdd, null,
											userSessionBean.getUserName(),
											userSessionBean.getPassword(),
											"BULK ASSIGN PROJECT TO MULTIPLE USERS");

									getTaskBean().executeLicenseTask(
											licenseTask);

									// assign license groups to user
									AsyncTask groupTask = new AssignUsersToGroupTask(
											userList, licensesToAdd, null,
											userSessionBean.getUserName(),
											userSessionBean.getPassword(),
											"BULK ASSIGN PROJECT TO MULTIPLE USERS");

									getTaskBean().executeGroupTask(groupTask);
								} else {
									LOG.info("ManageProjectBean bulkAssignRetractProject(): No licenses to assign together with project");
								}
							}

						} else {
							// remove licenses of the project from user if user
							// has the
							// license and user does not have any license's
							// projects
							List<License> licensesToRemove = UserLicenseDBHelper
									.licensesToRemoveOnRemovingUserProject(
											user, selectedProject.getName(),
											projectList);

							List<User> userList = new ArrayList<User>();
							userList.add(user);

							// spawning tasks
							if (licensesToRemove.size() > 0) {
								// remove licenses from user
								AsyncTask licenseTask = new AssignLicensesToUsersTask(
										userList, null, licensesToRemove,
										userSessionBean.getUserName(),
										userSessionBean.getPassword(),
										"BULK ASSIGN PROJECT TO MULTIPLE USERS");

								getTaskBean().executeLicenseTask(licenseTask);

								// remove license groups from user
								AsyncTask task = new AssignUsersToGroupTask(
										userList, null, licensesToRemove,
										userSessionBean.getUserName(),
										userSessionBean.getPassword(),
										"BULK ASSIGN PROJECT TO MULTIPLE USERS");

								getTaskBean().executeGroupTask(task);
							} else {
								LOG.info("ManageProjectBean bulkAssignRetractProject(): No licenses to remove together with project");
							}
						}
					}
				}
			});

			// assign/remove projects to/from users
			if (usersToPerform.size() > 0) {
				if (bulkAction.equals("assign")) {
					AsyncTask projectTask = new AssignProjectsToUsersTask(
							usersToPerform, projectList, null,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN PROJECT TO MULTIPLE USERS");
					getTaskBean().executeProjectTask(projectTask);
				} else {
					AsyncTask projectTask = new AssignProjectsToUsersTask(
							usersToPerform, null, projectList,
							userSessionBean.getUserName(),
							userSessionBean.getPassword(),
							"BULK ASSIGN PROJECT TO MULTIPLE USERS");
					getTaskBean().executeProjectTask(projectTask);
				}
			} else {
				LOG.info("ManageProjectBean bulkAssignRetractProject(): No user to update project");
			}

			RequestContext.getCurrentInstance().execute(
					"PF('bulk-update-dialog').hide()");

			returnMessage(null, FacesMessage.SEVERITY_INFO,
					"PROJECT_SUCCESSFUL_MSG", "PROJECT_SUCCESSFUL_"
							+ bulkAction.toUpperCase() + "_DES");

			inputUsers = new ArrayList<String>();
		} catch (Exception e) {
			LOG.info("ManageProjectBean bulkAssignRetractProject(): "
					+ e.getMessage());
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
	 * Redirect to manage user details page when clicking on user
	 * 
	 * @param user
	 *            sAMAccountName of user
	 * 
	 * @return manage user detail page (redirected)
	 */
	public String getUserDetails(String user) {
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

	public LazyProjectDataModel getProjectLazyList() {
		return projectLazyList;
	}

	public void setProjectLazyList(LazyProjectDataModel projectLazyList) {
		this.projectLazyList = projectLazyList;
	}

	public Project getSelectedProject() {
		return selectedProject;
	}

	public void setSelectedProject(Project selectedProject) {
		this.selectedProject = selectedProject;
	}

	public List<Project> getSelectedProjects() {
		return selectedProjects;
	}

	public void setSelectedProjects(List<Project> selectedProjects) {
		this.selectedProjects = selectedProjects;
	}

	public boolean isDisableDelete() {
		return disableDelete;
	}

	public void setDisableDelete(boolean disableDelete) {
		this.disableDelete = disableDelete;
	}

	public String getSelectedProjectValue() {
		return selectedProjectValue;
	}

	public void setSelectedProjectValue(String selectedProjectValue) {
		this.selectedProjectValue = selectedProjectValue;
	}

	public Project getNewProject() {
		return newProject;
	}

	public void setNewProject(Project newProject) {
		this.newProject = newProject;
	}

	public List<User> getUsersOfSelectedProject() {
		return usersOfSelectedProject;
	}

	public void setUsersOfSelectedProject(List<User> usersOfSelectedProject) {
		this.usersOfSelectedProject = usersOfSelectedProject;
	}

	public List<License> getLicensesOfSelectedProject() {
		return licensesOfSelectedProject;
	}

	public void setLicensesOfSelectedProject(
			List<License> licensesOfSelectedProject) {
		this.licensesOfSelectedProject = licensesOfSelectedProject;
	}

	public int getShowLicenseDeactivation() {
		return showLicenseDeactivation;
	}

	public void setShowLicenseDeactivation(int showLicenseDeactivation) {
		this.showLicenseDeactivation = showLicenseDeactivation;
	}

	public List<String> getInputUsers() {
		return inputUsers;
	}

	public void setInputUsers(List<String> inputUsers) {
		this.inputUsers = inputUsers;
	}

	public String getpSelectedProject() {
		return pSelectedProject;
	}

	public void setpSelectedProject(String pSelectedProject) {
		this.pSelectedProject = pSelectedProject;
	}

	public String getBulkAction() {
		return bulkAction;
	}

	public void setBulkAction(String bulkAction) {
		this.bulkAction = bulkAction;
	}

	public int getShowDeactivation() {
		return showDeactivation;
	}

	public void setShowDeactivation(int showDeactivation) {
		this.showDeactivation = showDeactivation;
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

	public boolean isAssignLicensesWithProject() {
		return assignLicensesWithProject;
	}

	public void setAssignLicensesWithProject(boolean assignLicensesWithProject) {
		this.assignLicensesWithProject = assignLicensesWithProject;
	}

	public LazyLicenseDataModel getLazyLicensesOfSelectedProject() {
		return lazyLicensesOfSelectedProject;
	}

	public void setLazyLicensesOfSelectedProject(
			LazyLicenseDataModel lazyLicensesOfSelectedProject) {
		this.lazyLicensesOfSelectedProject = lazyLicensesOfSelectedProject;
	}

	public LazyUserDataModel getUserByProjectLazyList() {
		return userByProjectLazyList;
	}

	public void setUserByProjectLazyList(LazyUserDataModel userByProjectLazyList) {
		this.userByProjectLazyList = userByProjectLazyList;
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
