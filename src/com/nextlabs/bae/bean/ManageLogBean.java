package com.nextlabs.bae.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.event.data.FilterEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.nextlabs.bae.helper.LogDBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.model.LazyLogDataModel;

@ManagedBean(name = "manageLogBean")
@ViewScoped
public class ManageLogBean implements Serializable {

	private static final Log LOG = LogFactory.getLog(ManageLogBean.class);
	private static final long serialVersionUID = 1L;
	transient private ManageExecutorServiceBean taskBean;
	private LazyLogDataModel allLogs;
	private StreamedContent exportFile;
	private List<String> columnNames;
	private List<String> selectedColumns;
	private String exportFileType;
	@ManagedProperty(value = "#{userSessionBean}")
	private UserSessionBean userSessionBean;
	private Map<String, Object> filteredMap;

	/**
	 * Executed after the bean is constructed
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	@PostConstruct
	public void init() {
		try {
			if (PropertyLoader.BAEADFrontEndPropertiesPath == null
					|| PropertyLoader.BAESToolConstantPath == null
					|| !userSessionBean.isLoggedIn()) {
				unauthorize();
				return;
			}

			if (userSessionBean.getIsAdmin()) {
				if (!userSessionBean.getIsLicenseManager()) {
					FacesContext.getCurrentInstance().getExternalContext()
							.redirect("synchronize.xhtml");
					return;
				} else {
					if (getTaskBean().getSyncStatus()) {
						FacesContext.getCurrentInstance().getExternalContext()
								.redirect("synchronize.xhtml");
						return;
					}
				}
			} else {
				if (getTaskBean().getSyncStatus()) {
					logoutOnSync();
					return;
				}
			}

			columnNames = LogDBHelper.getColumnsName();
			selectedColumns = new ArrayList<String>();
			exportFileType = "csv";
			allLogs = new LazyLogDataModel();
			filteredMap = null;
		} catch (Exception e) {
			LOG.error("ManageLogBean init(): " + e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Unauthorized access to the page - redirect
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	private void unauthorize() {
		LOG.info("Not logged in");
		// not logged in
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (Exception e) {
			LOG.error("ManageLogBean unauthorize(): " + e.getMessage(), e);
		}
	}

	private void logoutOnSync() {
		try {
			FacesContext.getCurrentInstance().getExternalContext()
					.redirect("login.xhtml");
		} catch (IOException e) {
			LOG.error("ManageGroupBean init(): " + e.getMessage(), e);
		}
	}

	/**
	 * Export logs
	 * 
	 * @exception Exception
	 *                Any exception
	 */
	public void handleExport() {
		if (getTaskBean().getSyncStatus()) {
			logoutOnSync();
			return;
		}

		try {
			LOG.info("ManageLogBean handleExport(): Export file");
			InputStream exportStream = LogDBHelper.exportLog(selectedColumns,
					filteredMap, exportFileType);
			if (exportStream == null) {
				returnUnexpectedError(null);
				return;
			}
			exportFile = new DefaultStreamedContent(exportStream,
					"application/" + exportFileType, "logs." + exportFileType);

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	public void returnUnexpectedError(String id) {
		returnMessage(id, FacesMessage.SEVERITY_ERROR, "UNEXPECTED_ERROR_MSG",
				"UNEXPECTED_ERROR_DES");
	}

	public void returnMessage(String id, Severity level, String sum, String des) {
		FacesContext.getCurrentInstance().addMessage(
				id,
				new FacesMessage(level, PropertyLoader.bAESConstant
						.getProperty(sum), PropertyLoader.bAESConstant
						.getProperty(des)));
	}

	/**
	 * Executed when log table is filtered
	 * 
	 * @param event
	 *            Filter event
	 */
	public void onLogFilter(FilterEvent event) {
		filteredMap = event.getFilters();
	}

	/*
	 * getters and setters
	 */
	public UserSessionBean getUserSessionBean() {
		return userSessionBean;
	}

	public void setUserSessionBean(UserSessionBean userSessionBean) {
		this.userSessionBean = userSessionBean;
	}

	public LazyLogDataModel getAllLogs() {
		return allLogs;
	}

	public void setAllLogs(LazyLogDataModel allLogs) {
		this.allLogs = allLogs;
	}

	public StreamedContent getExportFile() {
		return exportFile;
	}

	public void setExportFile(StreamedContent exportFile) {
		this.exportFile = exportFile;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	public List<String> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public String getExportFileType() {
		return exportFileType;
	}

	public void setExportFileType(String exportFileType) {
		this.exportFileType = exportFileType;
	}

	public ManageExecutorServiceBean getTaskBean() {
		if (taskBean == null) {
			FacesContext context = FacesContext.getCurrentInstance();
			taskBean = context.getApplication().evaluateExpressionGet(context,
					"#{manageExecutorServiceBean}",
					ManageExecutorServiceBean.class);
		}

		return taskBean;
	}

}
