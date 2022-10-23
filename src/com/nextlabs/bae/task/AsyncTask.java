package com.nextlabs.bae.task;

import java.sql.Timestamp;

import com.google.gson.annotations.Expose;
import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.User;

public abstract class AsyncTask implements Runnable {
	@Expose
	protected String admin;
	@Expose
	protected String password;
	@Expose
	protected String taskName;
	@Expose
	protected int progress;
	@Expose
	protected String message;
	@Expose
	protected String trigger;
	@Expose
	protected Boolean error;
	@Expose
	protected Timestamp timeStart;
	@Expose
	protected int totalProgress;
	@Expose
	protected int tempProgress;
	@Expose
	protected int numberOfUsers;
	@Expose
	protected int numberOfObjects;
	@Expose
	protected int numberOfSuccess;
	@Expose
	protected int numberOfFailure;

	public User getAdminUser() {
		User user = ActiveDirectoryHelper.getUser("userPrincipalName=" + admin,
				null);
		return user;
	}

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}

	public Boolean getError() {
		return error;
	}

	public void setError(Boolean error) {
		this.error = error;
	}

	public Timestamp getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(Timestamp timeStart) {
		this.timeStart = timeStart;
	}

	public int getTotalProgress() {
		return totalProgress;
	}

	public void setTotalProgress(int totalProgress) {
		this.totalProgress = totalProgress;
	}

	public int getTempProgress() {
		return tempProgress;
	}

	public void setTempProgress(int tempProgress) {
		this.tempProgress = tempProgress;
	}

	public int getNumberOfUsers() {
		return numberOfUsers;
	}

	public void setNumberOfUsers(int numberOfUsers) {
		this.numberOfUsers = numberOfUsers;
	}

	public int getNumberOfObjects() {
		return numberOfObjects;
	}

	public void setNumberOfObjects(int numberOfObjects) {
		this.numberOfObjects = numberOfObjects;
	}

	public int getNumberOfSuccess() {
		return numberOfSuccess;
	}

	public void setNumberOfSuccess(int numberOfSuccess) {
		this.numberOfSuccess = numberOfSuccess;
	}

	public int getNumberOfFailure() {
		return numberOfFailure;
	}

	public void setNumberOfFailure(int numberOfFailure) {
		this.numberOfFailure = numberOfFailure;
	}

}
