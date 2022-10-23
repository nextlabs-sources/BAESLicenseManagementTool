package com.nextlabs.bae.bean;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.DBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.SearchInput;
import com.nextlabs.bae.helper.User;

/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

@ManagedBean(name = "userSessionBean")
@SessionScoped
public class UserSessionBean implements Serializable {

	private static final Log LOG = LogFactory.getLog(UserSessionBean.class);
	private static final long serialVersionUID = 1L;
	private String userName;
	private String password;
	private String errorMessage;
	private boolean isLoggedIn;
	private List<String> controlledAttributeList;
	private List<String> controlledAttributeLabel;
	private List<String> securityGroups;
	private List<String> authorizedAttributeList;
	private List<String> authorizedAttributeLabel;
	private List<String> userDisplayedAttributeList;
	private HashMap<String, String> userDisplayedAttributeLabelMap;
	private List<String> searchFieldsAttributeList;
	private HashMap<String, String> searchFieldsAttributeLabelMap;
	private HashMap<String, String> attributeLabelMap;
	private HashMap<String, String> securityGroupMap;
	private List<String> userGroups;
	private List<SearchInput<String, String>> searchFields;
	private boolean displaySearchListFlag;
	private List<User> lastSearchResult;
	private String lastViewUser;
	private String lastSearchGroup;
	private List<String> lastMembers;
	private User loggedInUser;
	private String adminGroup;
	private boolean isAdmin;
	private boolean isLicenseManager;
	transient private ManageExecutorServiceBean taskBean;

	/**
	 * Run before the login page is rendered
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void preRender() {
		try {

			LOG.info("UserSessionBean preRender(): Prerender Login");

			ExternalContext externalContext = FacesContext.getCurrentInstance()
					.getExternalContext();

			// redirect to manageUser if user is logged in
			if (isLoggedIn) {
				LOG.info("UserSessionBean preRender(): User is logged in");

				if (getTaskBean().getSyncStatus() && !getIsAdmin()) {
					LOG.info("UserSessionBean preRender() Sync is happening and user is not admin. Proceed to logout");
					this.logout();
				}
				externalContext.redirect(externalContext
						.getRequestContextPath() + "/home.xhtml");
				return;

			}

			if (getTaskBean().getSyncStatus()) {
				returnMessage(null, FacesMessage.SEVERITY_WARN,
						"LOGIN_SYNCING_MSG", "LOGIN_SYNCING_DES");
			}

			updateProperties();

			// init
			isLoggedIn = false;

			int index = 1;

			controlledAttributeList = new ArrayList<String>();
			controlledAttributeLabel = new ArrayList<String>();
			authorizedAttributeLabel = new ArrayList<String>();
			authorizedAttributeList = new ArrayList<String>();
			attributeLabelMap = new HashMap<String, String>();
			securityGroups = new ArrayList<String>();
			securityGroupMap = new HashMap<String, String>();
			userDisplayedAttributeList = new ArrayList<String>();
			userDisplayedAttributeLabelMap = new HashMap<String, String>();
			searchFieldsAttributeList = new ArrayList<String>();
			searchFieldsAttributeLabelMap = new HashMap<String, String>();

			// get the attribute list from properties file
			while (true) {
				String attribute = PropertyLoader.bAESProperties
						.getProperty("attr" + index);
				String label = PropertyLoader.bAESProperties
						.getProperty("label" + index);
				String group = PropertyLoader.bAESProperties
						.getProperty("group" + index);
				if (attribute == null || label == null || group == null) {
					break;
				} else {
					controlledAttributeList.add(attribute.trim());
					controlledAttributeLabel.add(label.trim());
					securityGroups.add(group.trim());
					attributeLabelMap.put(attribute.trim(), label.trim());
					attributeLabelMap.put(label.trim(), attribute.trim());
					securityGroupMap.put(attribute.trim(), group.trim());
					index++;
				}
			}

			LOG.info("UserSessionBean preRender(): Found " + (index - 1)
					+ " attributes");

			adminGroup = PropertyLoader.bAESProperties
					.getProperty("admin-group");

			// get user displayed attribute list
			index = 1;
			while (true) {
				String attributeName = PropertyLoader.bAESProperties
						.getProperty("da" + index);
				String attributeLabel = PropertyLoader.bAESProperties
						.getProperty("lda" + index);
				if (attributeName == null || attributeLabel == null) {
					break;
				} else {
					userDisplayedAttributeList.add(attributeName.trim());
					userDisplayedAttributeLabelMap.put(attributeName,
							attributeLabel);
					index++;
				}
			}

			// get search field list
			index = 1;
			while (true) {
				String attributeName = PropertyLoader.bAESProperties
						.getProperty("sa" + index);
				String attributeLabel = PropertyLoader.bAESProperties
						.getProperty("lsa" + index);
				if (attributeName == null || attributeLabel == null) {
					break;
				} else {
					searchFieldsAttributeList.add(attributeName.trim());
					searchFieldsAttributeLabelMap.put(attributeName,
							attributeLabel);
					index++;
				}
			}

		} catch (Exception e) {
			LOG.error("UserSessionBean preRender(): " + e.getMessage(), e);
		}
	}

	/**
	 * On page load for manageTask.xhtml
	 */
	public void onPageLoad() {
		if (PropertyLoader.BAEADFrontEndPropertiesPath == null
				|| PropertyLoader.BAESToolConstantPath == null || !isLoggedIn()) {
			unauthorize();
			return;
		}
	}

	/**
	 * Unauthorized access to the page - redirect
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	private void unauthorize() {
		LOG.info("UserSessionBean unauthorize(): Not logged in");
		// not logged in
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (Exception e) {
			LOG.error("UserSessionBean unauthorize(): " + e.getMessage(), e);
		}
	}

	/**
	 * Check if DB can be connected
	 * 
	 * @exception Exception
	 *                SQL exception
	 */
	public void checkDB() {
		Connection connection = DBHelper.getDatabaseConnection();
		if (connection == null) {
			returnMessage("growl", FacesMessage.SEVERITY_ERROR,
					"DB_FAILED_MSG", "DB_FAILED_DES");
			return;
		} else {
			try {
				connection.close();
			} catch (SQLException e) {
				LOG.error("UserSessionBean checkDB(): Cannot close DB connection");
			}
		}
	}

	/**
	 * Update application properties
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void updateProperties() {
		try {
			// get properties file
			FacesContext ctx = FacesContext.getCurrentInstance();
			String propertiesPath = ctx.getExternalContext().getInitParameter(
					"BAESToolProperties");
			String constantPath = ctx.getExternalContext().getInitParameter(
					"BAESToolConstant");

			PropertyLoader.BAEADFrontEndPropertiesPath = propertiesPath;
			PropertyLoader.BAESToolConstantPath = constantPath;

			LOG.info("Properties file path: "
					+ PropertyLoader.BAEADFrontEndPropertiesPath);
			LOG.info("Constant file path: "
					+ PropertyLoader.BAESToolConstantPath);

			PropertyLoader.loadProperties();
			PropertyLoader.loadConstant();
		} catch (Exception e) {
			LOG.error("UserSessionBean updateProperties(): " + e.getMessage(),
					e);
		}
	}

	public UserSessionBean() {
	}

	/**
	 * Login - redirect to home page
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public String login() {
		try {
			if (userName == null || userName.trim().equals("")
					|| password == null || password.trim().equals("")) {
				LOG.info("UserSessionBean login(): Invalid input");
				returnMessage(null, FacesMessage.SEVERITY_ERROR,
						"LOGIN_FAILED_MSG", "LOGIN_FAILED_INVALID_INPUT_DES");
				return null;
			}

			// AD Authentication
			if (!ActiveDirectoryHelper.authenticate(userName, password)) {
				LOG.info("Invalid user name or password");
				returnMessage(null, FacesMessage.SEVERITY_ERROR,
						"LOGIN_FAILED_MSG",
						"LOGIN_FAILED_INVALID_CREDENTIAL_DES");
				return null;
			}

			String filter = "(userPrincipalName=" + userName + ")";

			loggedInUser = ActiveDirectoryHelper.getUser(filter, null);

			// get all groups that the logged in user is member of
			userGroups = ActiveDirectoryHelper.getAllUserGroup(filter);

			// check user access rights
			for (String key : controlledAttributeList) {
				// only get attribute that the logged in user is authorized for
				if (authorizeGroup(key)) {
					authorizedAttributeList.add(key);
					authorizedAttributeLabel.add(attributeLabelMap.get(key)
							.toString());
				}
			}

			if (userGroups.contains(adminGroup)) {
				LOG.info("UserSessionBean() login() Admin is logging in");
				isAdmin = true;
			}

			if (authorizedAttributeList.isEmpty()) {
				if (!isAdmin) {
					LOG.info("UserSessionBean login(): Unauthorized user "
							+ userName + " tried to login");
					returnMessage(null, FacesMessage.SEVERITY_ERROR,
							"UNAUTHORIZED_MSG", "UNAUTHORIZED_DES");
					return null;
				} else {
					isLicenseManager = false;
				}
			} else {
				isLicenseManager = true;
			}

			if (getTaskBean().getSyncStatus()) {
				if (!isAdmin) {
					returnMessage(null, FacesMessage.SEVERITY_WARN,
							"LOGIN_SYNCING_MSG", "LOGIN_SYNCING_DES");
					return null;
				}
			}

			LOG.info("UserSessionBean login(): Logged in user: "
					+ loggedInUser.getAttribute("distinguishedName"));

			// set login flag
			isLoggedIn = true;
			errorMessage = "";

			displaySearchListFlag = false;
			lastSearchResult = null;

		} catch (Exception e) {
			LOG.error("UserSessionBean login():" + e.getMessage(), e);
			returnUnexpectedError(null);
		}

		return "home?faces-redirect=true";
	}

	/**
	 * Log out - redirect to login page
	 */
	public String logout() {
		isLoggedIn = false;
		FacesContext.getCurrentInstance().getExternalContext()
				.invalidateSession();
		return "login?faces-redirect=true";
	}

	/**
	 * Check whether admin is allowed to manage an controlled data set
	 * 
	 * @param attributeName
	 *            Data set name
	 * 
	 * @return true or false
	 */
	public boolean authorizeGroup(String attributeName) {

		// check authorization by logged in user's membership
		try {
			for (String group : userGroups) {
				String securityGroup = securityGroupMap.get(attributeName);
				LOG.info("UserSessionBean authorizeGroup(): Checking security group for  "
						+ securityGroup);
				if (group.trim().equals(securityGroup.trim())) {
					LOG.info("UserSessionBean authorizeGroup(): Admin belongs to group "
							+ securityGroup);
					return true;
				}
			}
		} catch (Exception e) {
			LOG.error("UserSessionBean authorizeGroup():" + e.getMessage(), e);
		}
		return false;
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
	 * Get context helper from constant file
	 * 
	 * @param message
	 *            Key
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return value
	 */
	public String getContextHelper(String message) {
		try {
			if (PropertyLoader.bAESConstant == null) {
				FacesContext.getCurrentInstance().getExternalContext()
						.redirect("login.xhtml");
				return null;
			} else {
				return PropertyLoader.bAESConstant.getProperty(message);
			}
		} catch (Exception e) {
			LOG.error("UserSessionBean getContextHelper(): " + e.getMessage(),
					e);
			return null;
		}
	}

	/*
	 * getters and setters
	 */
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	public List<String> getControlledAttributeList() {
		return controlledAttributeList;
	}

	public void setControlledAttributeList(List<String> controlledAttributeList) {
		this.controlledAttributeList = controlledAttributeList;
	}

	public List<String> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(List<String> userGroups) {
		this.userGroups = userGroups;
	}

	public List<String> getControlledAttributeLabel() {
		return controlledAttributeLabel;
	}

	public void setControlledAttributeLabel(
			List<String> controlledAttributeLabel) {
		this.controlledAttributeLabel = controlledAttributeLabel;
	}

	public HashMap<String, String> getAttributeLabelMap() {
		return attributeLabelMap;
	}

	public void setAttributeLabelMap(HashMap<String, String> attributeLabelMap) {
		this.attributeLabelMap = attributeLabelMap;
	}

	public List<String> getAuthorizedAttributeList() {
		return authorizedAttributeList;
	}

	public void setAuthorizedAttributeList(List<String> authorizedAttributeList) {
		this.authorizedAttributeList = authorizedAttributeList;
	}

	public List<String> getAuthorizedAttributeLabel() {
		return authorizedAttributeLabel;
	}

	public void setAuthorizedAttributeLabel(
			List<String> authorizedAttributeLabel) {
		this.authorizedAttributeLabel = authorizedAttributeLabel;
	}

	public List<String> getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(List<String> securityGroups) {
		this.securityGroups = securityGroups;
	}

	public HashMap<String, String> getSecurityGroupMap() {
		return securityGroupMap;
	}

	public void setSecurityGroupMap(HashMap<String, String> securityGroupMap) {
		this.securityGroupMap = securityGroupMap;
	}

	public List<String> getUserDisplayedAttributeList() {
		return userDisplayedAttributeList;
	}

	public void setUserDisplayedAttributeList(
			List<String> userDisplayedAttributeList) {
		this.userDisplayedAttributeList = userDisplayedAttributeList;
	}

	public HashMap<String, String> getUserDisplayedAttributeLabelMap() {
		return userDisplayedAttributeLabelMap;
	}

	public void setUserDisplayedAttributeLabelMap(
			HashMap<String, String> userDisplayedAttributeLabelMap) {
		this.userDisplayedAttributeLabelMap = userDisplayedAttributeLabelMap;
	}

	public List<String> getSearchFieldsAttributeList() {
		return searchFieldsAttributeList;
	}

	public void setSearchFieldsAttributeList(
			List<String> searchFieldsAttributeList) {
		this.searchFieldsAttributeList = searchFieldsAttributeList;
	}

	public HashMap<String, String> getSearchFieldsAttributeLabelMap() {
		return searchFieldsAttributeLabelMap;
	}

	public void setSearchFieldsAttributeLabelMap(
			HashMap<String, String> searchFieldsAttributeLabelMap) {
		this.searchFieldsAttributeLabelMap = searchFieldsAttributeLabelMap;
	}

	public List<SearchInput<String, String>> getSearchFields() {
		return searchFields;
	}

	public void setSearchFields(List<SearchInput<String, String>> searchFields) {
		this.searchFields = searchFields;
	}

	public Boolean getDisplaySearchListFlag() {
		return displaySearchListFlag;
	}

	public void setDisplaySearchListFlag(Boolean displaySearchListFlag) {
		this.displaySearchListFlag = displaySearchListFlag;
	}

	public List<User> getLastSearchResult() {
		return lastSearchResult;
	}

	public void setLastSearchResult(List<User> lastSearchResult) {
		this.lastSearchResult = lastSearchResult;
	}

	public String getLastViewUser() {
		return lastViewUser;
	}

	public void setLastViewUser(String lastViewUser) {
		this.lastViewUser = lastViewUser;
	}

	public User getLoggedInUser() {
		return loggedInUser;
	}

	public void setLoggedInUser(User loggedInUser) {
		this.loggedInUser = loggedInUser;
	}

	public String getLastSearchGroup() {
		return lastSearchGroup;
	}

	public void setLastSearchGroup(String lastSearchGroup) {
		this.lastSearchGroup = lastSearchGroup;
	}

	public List<String> getLastMembers() {
		return lastMembers;
	}

	public void setLastMembers(List<String> lastMembers) {
		this.lastMembers = lastMembers;
	}

	public String getAdminGroup() {
		return adminGroup;
	}

	public void setAdminGroup(String adminGroup) {
		this.adminGroup = adminGroup;
	}

	public boolean getIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	public boolean getIsLicenseManager() {
		return isLicenseManager;
	}

	public void setIsLicenseManager(boolean isLicenseManager) {
		this.isLicenseManager = isLicenseManager;
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
