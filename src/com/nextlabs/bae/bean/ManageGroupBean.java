package com.nextlabs.bae.bean;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.Group;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;

@ManagedBean(name = "manageGroupBean")
@ViewScoped
public class ManageGroupBean implements Serializable {

	private static final Log LOG = LogFactory.getLog(ManageGroupBean.class);
	private static final long serialVersionUID = 1L;

	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	transient private ManageExecutorServiceBean taskBean;
	private List<String> members;
	private String searchInput;
	private boolean displayResult;

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

			// get previous search if there's one
			if (userSessionBean.getLastMembers() != null
					&& !userSessionBean.getLastMembers().isEmpty()) {
				members = userSessionBean.getLastMembers();
				displayResult = true;
				searchInput = userSessionBean.getLastSearchGroup();
			} else {
				displayResult = false;
				searchInput = "";
			}
		} catch (Exception e) {
			LOG.error("ManageGroupBean init(): " + e.getMessage(), e);
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

	public ManageGroupBean() {
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
			LOG.error("ManageGroupBean unauthorize(): " + e.getMessage(), e);
		}
	}

	/**
	 * Get suggested input list when admin type into search box
	 * 
	 * @param query
	 *            User input used to generate suggestions
	 * 
	 * @exception Exception
	 *                Any exception
	 * 
	 * @return Suggested list
	 */
	public List<String> getSuggestedInput(String query) {
		List<String> result = new ArrayList<String>();
		try {
			String filter = "(&(objectClass=group)(sAMAccountName=" + query
					+ "*))";
			List<Group> groupList = ActiveDirectoryHelper.getSuggestedAdGroups(
					filter, null);
			for (Group group : groupList) {
				result.add(group.getGroupName());
			}

		} catch (Exception e) {
			LOG.error("ManageGroupBean getSuggestedInput(): " + e.getMessage(),
					e);
			returnUnexpectedError(null);
		}
		return result;
	}

	/**
	 * Get members of a license group
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void getMembership() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}
		
		try {
			// if input is empty
			if (searchInput == null || searchInput.trim().equals("")) {
				returnMessage(null, FacesMessage.SEVERITY_ERROR,
						"GROUP_EMPTY_INPUT_MSG", "GROUP_EMPTY_INPUT_DES");
				return;
			}

			LOG.info("ManageGroupBean getMembership(): Get membership of group "
					+ searchInput);
			Group group = ActiveDirectoryHelper.getAdGroup(searchInput, null);

			if (group == null) {
				FacesContext
						.getCurrentInstance()
						.addMessage(
								null,
								new FacesMessage(
										FacesMessage.SEVERITY_ERROR,
										"Group doesn't exist!",
										"The group specified doesn't exist. It might have been deleted by other session."));
				displayResult = false;
				return;
			}
			members = new ArrayList<String>();

			// remove the prefix from the query results
			List<User> memberList = ActiveDirectoryHelper.getUsers("memberOf="
					+ group.getAttribute("distinguishedName"), null, 900);
			for (User user : memberList) {
				LOG.info("ManageGropuBean getMembership(): get member: "
						+ user.getAttribute("sAMAccountName"));
				members.add(user.getAttribute("sAMAccountName"));
			}

			Collections.sort(members);
			displayResult = true;
		} catch (Exception e) {
			LOG.error("ManageGroupBean getMembership(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}

	}

	/**
	 * Redirect to user detail page
	 * 
	 * @param user
	 *            sAMAccountName of user
	 * 
	 * @return user detail page (redirected)
	 */
	public String getUserDetails(String user) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash()
				.put("selectedUser", "sAMAccountName=" + user);
		return "manageUserDetails?faces-redirect=true";
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

	public List<String> getMembers() {
		return members;
	}

	public void setMembers(List<String> members) {
		this.members = members;
	}

	public String getSearchInput() {
		return searchInput;
	}

	public void setSearchInput(String searchInput) {
		this.searchInput = searchInput;
	}

	public boolean isDisplayResult() {
		return displayResult;
	}

	public void setDisplayResult(boolean displayResult) {
		this.displayResult = displayResult;
	}

}
