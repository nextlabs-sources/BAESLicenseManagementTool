package com.nextlabs.bae.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.nextlabs.bae.helper.User;
import com.nextlabs.bae.helper.UserLicenseDBHelper;
import com.nextlabs.bae.helper.UserProjectDBHelper;

public class LazyUserDataModel extends LazyDataModel<User> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String constraintName;
	private String constraintValue;

	public LazyUserDataModel() {
		this.constraintName = null;
	}

	public LazyUserDataModel(String constraintName, String constraintValue,
			int deactivated) {
		this.constraintName = constraintName;
		this.constraintValue = constraintValue;
	}

	@Override
	public List<User> load(int first, int pageSize, String sortField,
			SortOrder sortOrder, Map<String, Object> filters) {
		List<User> data = new ArrayList<User>();

		String sortOrderString;
		// translate sort order
		if (sortOrder == null) {
			sortOrderString = "";
		} else if (sortOrder.equals(SortOrder.ASCENDING)) {
			sortOrderString = "ASC";
		} else if (sortOrder.equals(SortOrder.DESCENDING)) {
			sortOrderString = "DESC";
		} else if (sortOrder.equals(SortOrder.UNSORTED)) {
			sortOrderString = "";
		} else {
			sortOrderString = "";
		}
		if (constraintName == null) {

		} else if (constraintName.equals("license")) {
			data = UserLicenseDBHelper.getUserListByLicenseLazy(first,
					pageSize, sortField, sortOrderString, filters,
					constraintValue);
			this.setRowCount(UserLicenseDBHelper.countUserByLicense(filters, constraintValue));
		} else if (constraintName.equals("project")) {
			data = UserProjectDBHelper.getUserListByProjectLazy(first,
					pageSize, sortField, sortOrderString, filters,
					constraintValue);
			this.setRowCount(UserProjectDBHelper.countUserByProject(filters, constraintValue));
		}
		return data;
	}

	public String getConstraintName() {
		return constraintName;
	}

	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}

	public String getConstraintValue() {
		return constraintValue;
	}

	public void setConstraintValue(String constraintValue) {
		this.constraintValue = constraintValue;
	}

}
