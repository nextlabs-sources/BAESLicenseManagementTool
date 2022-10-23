package com.nextlabs.bae.bean;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.nextlabs.bae.helper.PropertyLoader;

@ManagedBean(name = "manageHomeBean")
@ViewScoped
public class ManageHomeBean implements Serializable {

	private static final Log LOG = LogFactory.getLog(ManageHomeBean.class);
	private static final long serialVersionUID = 1L;
	private List<String> userDisplayedAttributeLabels;
	private List<String> userControlledAttributeLabels;
	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;

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

			userDisplayedAttributeLabels = new ArrayList<String>();
			userControlledAttributeLabels = new ArrayList<String>();

			for (String attributeLabel : userSessionBean
					.getControlledAttributeLabel()) {
				userControlledAttributeLabels.add(attributeLabel);
			}

			for (String attributeLabel : userSessionBean
					.getUserDisplayedAttributeLabelMap().values()) {
				userDisplayedAttributeLabels.add(attributeLabel);
			}
		} catch (Exception e) {
			LOG.error("ManageHomeBean init():" + e.getMessage(), e);
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
			LOG.error("ManageHomeBean unauthorzied(): " + e.getMessage(), e);
		}
	}

	/**
	 * Get controlled attribute name
	 * 
	 * @param label
	 *            Label of the attribute
	 * 
	 * @return The attribute
	 */
	public String getControlledAttributeName(String label) {
		return userSessionBean.getAttributeLabelMap().get(label);
	}

	/**
	 * Get controlled attribute security group
	 * 
	 * @param Label
	 *            of the attribute
	 * 
	 * @return The security group of the attribute
	 */
	public String getControlledAttributeSecurityGroup(String label) {
		return userSessionBean.getSecurityGroupMap().get(
				getControlledAttributeName(label));
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

	public List<String> getUserDisplayedAttributeLabels() {
		return userDisplayedAttributeLabels;
	}

	public void setUserDisplayedAttributeLabels(
			List<String> userDisplayedAttributeLabels) {
		this.userDisplayedAttributeLabels = userDisplayedAttributeLabels;
	}

	public List<String> getUserControlledAttributeLabels() {
		return userControlledAttributeLabels;
	}

	public void setUserControlledAttributeLabels(
			List<String> userControlledAttributeLabels) {
		this.userControlledAttributeLabels = userControlledAttributeLabels;
	}

}
