package com.nextlabs.bae.helper;

import java.io.Serializable;

import javax.naming.directory.Attributes;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gson.annotations.Expose;

@XmlRootElement
public class User implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Attributes attributes;
	@Expose
	private String aduser;
	@Expose
	private String displayName;
	@Expose
	private String email;

	public User() {
	}

	public User(Attributes attributes) {
		this.attributes = attributes;
		this.aduser = getAttribute("sAMAccountName");
		this.displayName = getAttribute("displayName");
		this.email = getAttribute("mail");
	}

	public User(String aduser, String displayName, String email) {
		this.aduser = aduser;
		this.displayName = displayName;
		this.email = email;
		this.attributes = null;
	}

	@XmlTransient
	public Attributes getAttributes() {
		return attributes;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public String getAttribute(String attributeName) {
		return ActiveDirectoryHelper.getAttribute(attributes, attributeName);
	}

	@XmlElement
	public String getAduser() {
		return aduser;
	}

	public void setAduser(String aduser) {
		this.aduser = aduser;
	}

	@XmlElement
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@XmlElement
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
