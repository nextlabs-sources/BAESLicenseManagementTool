package com.nextlabs.bae.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.nextlabs.bae.helper.BAELog;
import com.nextlabs.bae.helper.LogDBHelper;

public class LazyLogDataModel extends LazyDataModel<BAELog> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LazyLogDataModel() {
	}

	@Override
	public List<BAELog> load(int first, int pageSize, String sortField,
			SortOrder sortOrder, Map<String, Object> filters) {
		List<BAELog> data = new ArrayList<BAELog>();

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
		
		data = LogDBHelper.getLogListLazy(first, pageSize, sortField, sortOrderString, filters);
		this.setRowCount(LogDBHelper.countAllLog(filters));
		return data;
	}
}
