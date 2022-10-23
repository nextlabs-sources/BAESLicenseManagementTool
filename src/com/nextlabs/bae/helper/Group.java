package com.nextlabs.bae.helper;

import java.io.Serializable;

import javax.naming.directory.Attributes;

import com.google.gson.annotations.Expose;

public class Group implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Attributes attributes;
	@Expose
	private String groupName;

	public Group() {
	}

	public Group(Attributes attributes) {
		this.attributes = attributes;
		this.groupName = getAttribute("sAMAccountName");
	}

	public Group(String groupName) {
		this.groupName = groupName;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public void setAttributes(Attributes attributes) {
		this.attributes = attributes;
	}

	public String getAttribute(String attributeName) {
		return ActiveDirectoryHelper.getAttribute(attributes, attributeName);
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
