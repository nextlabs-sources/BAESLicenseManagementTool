package com.nextlabs.bae.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.nextlabs.bae.helper.License;
import com.nextlabs.bae.helper.LicenseDBHelper;
import com.nextlabs.bae.helper.LicenseProjectDBHelper;

public class LazyLicenseDataModel extends LazyDataModel<License> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int deactivated;
	private String constraintName;
	private String constraintValue;

	public LazyLicenseDataModel(int deactivated) {
		this.deactivated = deactivated;
		this.constraintName = null;
	}

	public LazyLicenseDataModel(String constraintName, String constraintValue,
			int deactivated) {
		this.constraintName = constraintName;
		this.constraintValue = constraintValue;
		this.deactivated = deactivated;
	}

	@Override
	public List<License> load(int first, int pageSize, String sortField,
			SortOrder sortOrder, Map<String, Object> filters) {
		List<License> data = new ArrayList<License>();

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
			data = LicenseDBHelper.getLicensesListLazy(first, pageSize,
					sortField, sortOrderString, filters, deactivated);
			this.setRowCount(LicenseDBHelper.countAllLicense(filters,
					deactivated));
		} else {
			if (constraintName.equals("project")) {
				data = LicenseProjectDBHelper.getLicensesListLazyByProject(
						first, pageSize, sortField, sortOrderString, filters,
						constraintValue, deactivated);
				this.setRowCount(LicenseProjectDBHelper.countLicenseProject(
						filters, deactivated, constraintValue));
			}
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
