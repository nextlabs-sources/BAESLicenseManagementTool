package com.nextlabs.bae.helper;

import java.io.Serializable;

import com.google.gson.annotations.Expose;

public class Project implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose
	private String name;
	@Expose
	private String description;
	@Expose
	private int deactivated;

	public Project() {
		name = "";
		description = "";
		deactivated = 0;
	}

	public Project(String name, String description, int deactivated) {
		this.name = name;
		this.description = description;
		this.deactivated = deactivated;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getDeactivated() {
		return deactivated;
	}

	public void setDeactivated(int deactivated) {
		this.deactivated = deactivated;
	}

}
