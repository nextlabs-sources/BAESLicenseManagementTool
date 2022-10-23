package com.nextlabs.bae.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.nextlabs.bae.helper.Project;
import com.nextlabs.bae.helper.ProjectDBHelper;

public class LazyProjectDataModel extends LazyDataModel<Project> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int deactivated;

	public LazyProjectDataModel(int deactivated) {
		this.deactivated = deactivated;
	}

	@Override
	public List<Project> load(int first, int pageSize, String sortField,
			SortOrder sortOrder, Map<String, Object> filters) {
		List<Project> data = new ArrayList<Project>();

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
		
		data = ProjectDBHelper.getProjectListLazy(first, pageSize, sortField, sortOrderString, filters, deactivated);
		this.setRowCount(ProjectDBHelper.countAllProject(filters, deactivated));
		return data;
	}
}
