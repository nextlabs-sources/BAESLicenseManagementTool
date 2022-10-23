package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Timestamp;

public class BAELog implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Timestamp time;
	private String admin;
	private String targetUser;
	private String action;
	private String attribute;
	private String oldValue;
	private String newValue;
	private String trigger;

	public BAELog(Timestamp time, String admin, String targetUser,
			String action, String attribute, String oldValue, String newValue,
			String trigger) {
		this.time = time;
		this.admin = admin;
		this.targetUser = targetUser;
		this.action = action;
		this.attribute = attribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.trigger = trigger;
	}

	public Timestamp getTime() {
		return time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getTargetUser() {
		return targetUser;
	}

	public void setTargetUser(String targetUser) {
		this.targetUser = targetUser;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}

}
