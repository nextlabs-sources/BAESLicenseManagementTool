package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Timestamp;

import com.google.gson.annotations.Expose;

public class License implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose
	private String name;
	@Expose
	private String parties;
	@Expose
	private String category;
	@Expose
	private String label;
	@Expose
	private Timestamp effectiveDate;
	@Expose
	private Timestamp expiration;
	@Expose
	private int deactivated;
	@Expose
	private int groupEnabled;
	@Expose
	private String groupName;

	public License(String name, String parties, String category, String label,
			Timestamp effectiveDate, Timestamp expiration, int deactivated,
			int groupEnabled, String groupName) {
		this.name = name;
		this.parties = parties;
		this.category = category;
		this.label = label;
		this.effectiveDate = effectiveDate;
		this.expiration = expiration;
		this.deactivated = deactivated;
		this.groupEnabled = groupEnabled;
		this.groupName = groupName;
	}

	public License() {
		name = "";
		parties = "";
		category = "";
		label = "";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getParties() {
		return parties;
	}

	public void setParties(String parties) {
		this.parties = parties;
	}

	public Timestamp getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Timestamp effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public Timestamp getExpiration() {
		return expiration;
	}

	public void setExpiration(Timestamp expiration) {
		this.expiration = expiration;
	}

	public int getDeactivated() {
		return deactivated;
	}

	public void setDeactivated(int deactivated) {
		this.deactivated = deactivated;
	}

	public int getGroupEnabled() {
		return groupEnabled;
	}

	public void setGroupEnabled(int groupEnabled) {
		this.groupEnabled = groupEnabled;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
