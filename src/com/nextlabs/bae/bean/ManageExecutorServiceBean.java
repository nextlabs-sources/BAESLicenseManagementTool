package com.nextlabs.bae.bean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.DBHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.TaskDBHelper;
import com.nextlabs.bae.task.AsyncTask;
import com.nextlabs.bae.task.SynchronizationTask;

@ManagedBean(name = "manageExecutorServiceBean", eager = true)
@ApplicationScoped
public class ManageExecutorServiceBean {
	private static final Log LOG = LogFactory
			.getLog(ManageExecutorServiceBean.class);

	private ExecutorService executorForSpawningTask;
	private ExecutorService executorForGroup;
	private ExecutorService executorForLicense;
	private ExecutorService executorForProject;
	private LinkedBlockingDeque<Runnable> groupQueue;
	private LinkedBlockingDeque<Runnable> licenseQueue;
	private LinkedBlockingDeque<Runnable> projectQueue;
	private List<AsyncTask> groupTasks;
	private List<AsyncTask> licenseTasks;
	private List<AsyncTask> projectTasks;
	private List<AsyncTask> groupPendingTasks;
	private List<AsyncTask> licensePendingTasks;
	private List<AsyncTask> projectPendingTasks;
	private AsyncTask executingGroupTask;
	private AsyncTask executingLicenseTask;
	private AsyncTask executingProjectTask;
	private List<AsyncTask> recentCompletedGroupTasks;
	private List<AsyncTask> recentCompletedLicenseTasks;
	private List<AsyncTask> recentCompletedProjectTasks;
	private AsyncTask selectedTask;
	private SynchronizationTask syncTask;
	private StreamedContent exportFile;

	/**
	 * Executed after the been is constructed, setting up all resources used by
	 * the bean Check database and add data if needed from Active Directory Get
	 * corrupted tasks and restart them
	 */
	@PostConstruct
	public void init() {
		// setup properties of the application
		setupProperties();

		LOG.info("ManageExecutorServiceBean init(): Initializing Executor service...");

		// executor service for tasks that prepare data on each update and spawn
		// necessary working tasks
		executorForSpawningTask = Executors.newSingleThreadExecutor();

		// variables for license group task
		executingGroupTask = null;
		groupQueue = new LinkedBlockingDeque<Runnable>();
		executorForGroup = new ThreadPoolExecutor(1, 1, 0L,
				TimeUnit.MILLISECONDS, groupQueue);
		groupPendingTasks = new ArrayList<AsyncTask>();
		groupTasks = new ArrayList<AsyncTask>();

		// variables for license task
		licenseQueue = new LinkedBlockingDeque<Runnable>();
		executorForLicense = new ThreadPoolExecutor(1, 1, 0L,
				TimeUnit.MILLISECONDS, licenseQueue);
		licensePendingTasks = new ArrayList<AsyncTask>();
		licenseTasks = new ArrayList<AsyncTask>();
		executingLicenseTask = null;

		// variables for project task
		projectQueue = new LinkedBlockingDeque<Runnable>();
		executorForProject = new ThreadPoolExecutor(1, 1, 0L,
				TimeUnit.MILLISECONDS, projectQueue);
		projectPendingTasks = new ArrayList<AsyncTask>();
		projectTasks = new ArrayList<AsyncTask>();
		executingProjectTask = null;

		LOG.info("ManageExecutorServiceBean init(): Initialize DB and AD if necessary..");

		// checking connection before getting tasks
		if (!ActiveDirectoryHelper.testLdap(PropertyLoader.bAESProperties
				.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"))) {
			LOG.error("ManageExectorServiceBean init(): Cannot connect to AD");
			return;
		}

		if (!DBHelper.testDB()) {
			LOG.error("ManageExecutorServiceBean init(): Cananot connect to DB");
			return;
		}

		recentCompletedGroupTasks = TaskDBHelper
				.getRecentCompletedTask("group");

		// get interrupted license group tasks from the last application up time
		List<AsyncTask> interruptedGroupTask = TaskDBHelper
				.getIncompletedTask("group");
		for (int i = 0; i < interruptedGroupTask.size(); i++) {
			AsyncTask interruptedTask = interruptedGroupTask.get(i);
			TaskDBHelper.deleteTask(interruptedTask.getTimeStart(),
					interruptedTask.getAdminUser().getAduser(), "group");
			executeGroupTask(interruptedTask);
		}

		recentCompletedLicenseTasks = TaskDBHelper
				.getRecentCompletedTask("license");

		// get interrupted license tasks from the last application up time
		List<AsyncTask> interruptedLicenseTask = TaskDBHelper
				.getIncompletedTask("license");
		for (int i = 0; i < interruptedLicenseTask.size(); i++) {
			AsyncTask interruptedTask = interruptedLicenseTask.get(i);
			TaskDBHelper.deleteTask(interruptedTask.getTimeStart(),
					interruptedTask.getAdminUser().getAduser(), "license");
			executeLicenseTask(interruptedTask);
		}

		recentCompletedProjectTasks = TaskDBHelper
				.getRecentCompletedTask("project");

		// get interrupted project tasks from the last application up time
		List<AsyncTask> interruptedProjectTask = TaskDBHelper
				.getIncompletedTask("project");
		for (int i = 0; i < interruptedProjectTask.size(); i++) {
			AsyncTask interruptedTask = interruptedProjectTask.get(i);
			TaskDBHelper.deleteTask(interruptedTask.getTimeStart(),
					interruptedTask.getAdminUser().getAduser(), "project");
			executeProjectTask(interruptedTask);
		}

		LOG.info("ManageExecutorServiceBean init(): Finished initializing Executor service");
	}

	/**
	 * Task synchronization
	 */
	public void sync() {
		syncTask = new SynchronizationTask();
		executeSpawningTask(syncTask);
	}

	public void executeSpawningTask(Runnable r) {
		executorForSpawningTask.execute(r);
	}

	/**
	 * Check if a synchronization is happening
	 * 
	 * @return
	 */
	public boolean getSyncStatus() {
		if (syncTask == null || syncTask.getProgress() == 100) {
			return false;
		} else {
			return true;
		}
	}

	public void clearSyncTask() {
		syncTask = null;
	}

	public void exportSynchronization() {
		try {
			LOG.info("ManageExecutorServiceBean exportSynchronization(): Export file");
			InputStream exportStream = syncTask.exportTask();
			if (exportStream == null) {
				returnUnexpectedError(null);
				return;
			}
			exportFile = new DefaultStreamedContent(exportStream,
					"application/pdf", "synchronization.pdf");

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			returnUnexpectedError(null);
		}
	}

	/**
	 * Executing a new license group task
	 * 
	 * @param r
	 *            Async task to be executed
	 */
	public void executeGroupTask(AsyncTask r) {
		LOG.info("Executing group task ... ");
		if (executorForGroup == null) {
			LOG.info("Executor is null");
			executorForGroup = new ThreadPoolExecutor(1, 1, 0L,
					TimeUnit.MILLISECONDS, groupQueue);
		}
		TaskDBHelper.createTask((AsyncTask) r, "group");
		executorForGroup.execute(r);
		groupTasks.add(r);
	}

	/**
	 * Executing a new license task
	 * 
	 * @param r
	 *            Async task to be executed
	 */
	public void executeLicenseTask(AsyncTask r) {
		LOG.info("Executing license task ... ");
		if (executorForLicense == null) {
			LOG.info("Executor is null");
			executorForLicense = new ThreadPoolExecutor(1, 1, 0L,
					TimeUnit.MILLISECONDS, licenseQueue);
		}
		TaskDBHelper.createTask((AsyncTask) r, "license");
		executorForLicense.execute(r);
		licenseTasks.add(r);
	}

	/**
	 * Executing a new project task
	 * 
	 * @param r
	 *            Async task to be executed
	 */
	public void executeProjectTask(AsyncTask r) {
		LOG.info("Executing project task ... ");
		if (executorForProject == null) {
			LOG.info("Executor is null");
			executorForProject = new ThreadPoolExecutor(1, 1, 0L,
					TimeUnit.MILLISECONDS, projectQueue);
		}
		TaskDBHelper.createTask((AsyncTask) r, "project");
		executorForProject.execute(r);
		projectTasks.add(r);
	}

	/*
	 * Executed before the bean is destroyed Shut down all executor services
	 */
	@PreDestroy
	public void destroy() {
		LOG.info("ManageExecutorServiceBean destroy(): Destroying Executor service ...");
		executorForGroup.shutdownNow();
		executorForLicense.shutdownNow();
		executorForProject.shutdownNow();
		executorForSpawningTask.shutdownNow();
		LOG.info("ManageExecutorServiceBean destroy(): Finished destroying Executor service");
	}

	/**
	 * Update group tasks progress
	 */
	public void updateGroupProgress() {
		List<AsyncTask> toRemoveGroupTask = new ArrayList<AsyncTask>();

		// check for completed tasks
		for (AsyncTask r : groupTasks) {
			if (r.getProgress() >= 100) {
				toRemoveGroupTask.add(r);
			}
		}

		// remove completed tasks from the main task list
		for (AsyncTask r : toRemoveGroupTask) {
			groupTasks.remove(r);
		}

		// get executing task
		if (groupTasks.size() > 0) {
			executingGroupTask = groupTasks.get(0);
		} else {
			executingGroupTask = null;
		}

		// get pending tasks from the queue
		groupPendingTasks.clear();
		AsyncTask[] temp = groupQueue.toArray(new AsyncTask[0]);
		for (AsyncTask t : temp) {
			groupPendingTasks.add(t);
		}

		// get recent completed tasks
		recentCompletedGroupTasks = TaskDBHelper
				.getRecentCompletedTask("group");

	}

	/**
	 * Update license tasks progress
	 */
	public void updateLicenseProgress() {
		List<AsyncTask> toRemoveLicenseTask = new ArrayList<AsyncTask>();

		// check for completed tasks
		for (AsyncTask r : licenseTasks) {
			if (r.getProgress() >= 100) {
				toRemoveLicenseTask.add(r);
			}
		}

		// remove completed tasks
		for (AsyncTask r : toRemoveLicenseTask) {
			licenseTasks.remove(r);
		}

		// get executing task
		if (licenseTasks.size() > 0) {
			executingLicenseTask = licenseTasks.get(0);
		} else {
			executingLicenseTask = null;
		}

		// get pending tasks from the queue
		licensePendingTasks.clear();
		AsyncTask[] temp = licenseQueue.toArray(new AsyncTask[0]);
		for (AsyncTask t : temp) {
			licensePendingTasks.add(t);
		}

		// get recent completed tasks
		recentCompletedLicenseTasks = TaskDBHelper
				.getRecentCompletedTask("license");
	}

	/**
	 * Update project task progress
	 */
	public void updateProjectProgress() {
		List<AsyncTask> toRemoveProjectTask = new ArrayList<AsyncTask>();

		// check for completed tasks
		for (AsyncTask r : projectTasks) {
			if (r.getProgress() >= 100) {
				toRemoveProjectTask.add(r);
			}
		}

		// remove completed tasks
		for (AsyncTask r : toRemoveProjectTask) {
			projectTasks.remove(r);
		}

		// get executing task
		if (projectTasks.size() > 0) {
			executingProjectTask = projectTasks.get(0);
		} else {
			executingProjectTask = null;
		}

		// get pending tasks from the queue
		projectPendingTasks.clear();
		AsyncTask[] temp = projectQueue.toArray(new AsyncTask[0]);
		for (AsyncTask t : temp) {
			projectPendingTasks.add(t);
		}

		// get recent completed tasks
		recentCompletedProjectTasks = TaskDBHelper
				.getRecentCompletedTask("project");
	}

	/**
	 * Setup application properties
	 */
	public void setupProperties() {
		// get properties file
		FacesContext ctx = FacesContext.getCurrentInstance();
		String propertiesPath = ctx.getExternalContext().getInitParameter(
				"BAESToolProperties");
		String constantPath = ctx.getExternalContext().getInitParameter(
				"BAESToolConstant");

		// set properties paths
		PropertyLoader.BAEADFrontEndPropertiesPath = propertiesPath;
		PropertyLoader.BAESToolConstantPath = constantPath;

		LOG.info("Properties file path: "
				+ PropertyLoader.BAEADFrontEndPropertiesPath);
		LOG.info("Constant file path: " + PropertyLoader.BAESToolConstantPath);

		// load properties
		PropertyLoader.loadProperties();
		PropertyLoader.loadConstant();
	}

	public void selectTask() {
		LOG.info("Viewing information of " + selectedTask.getTaskName());
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

	/* Getters and Setters */

	public ExecutorService getExecutorForGroup() {
		return executorForGroup;
	}

	public void setExecutorForGroup(ExecutorService executorForGroup) {
		this.executorForGroup = executorForGroup;
	}

	public ExecutorService getExecutorForLicense() {
		return executorForLicense;
	}

	public void setExecutorForLicense(ExecutorService executorForLicense) {
		this.executorForLicense = executorForLicense;
	}

	public ExecutorService getExecutorForProject() {
		return executorForProject;
	}

	public void setExecutorForProject(ExecutorService executorForProject) {
		this.executorForProject = executorForProject;
	}

	public LinkedBlockingDeque<Runnable> getGroupQueue() {
		return groupQueue;
	}

	public void setGroupQueue(LinkedBlockingDeque<Runnable> groupQueue) {
		this.groupQueue = groupQueue;
	}

	public LinkedBlockingDeque<Runnable> getLicenseQueue() {
		return licenseQueue;
	}

	public void setLicenseQueue(LinkedBlockingDeque<Runnable> licenseQueue) {
		this.licenseQueue = licenseQueue;
	}

	public LinkedBlockingDeque<Runnable> getProjectQueue() {
		return projectQueue;
	}

	public void setProjectQueue(LinkedBlockingDeque<Runnable> projectQueue) {
		this.projectQueue = projectQueue;
	}

	public List<AsyncTask> getGroupTasks() {
		return groupTasks;
	}

	public void setGroupTasks(List<AsyncTask> groupTasks) {
		this.groupTasks = groupTasks;
	}

	public List<AsyncTask> getLicenseTasks() {
		return licenseTasks;
	}

	public void setLicenseTasks(List<AsyncTask> licenseTasks) {
		this.licenseTasks = licenseTasks;
	}

	public List<AsyncTask> getProjectTasks() {
		return projectTasks;
	}

	public void setProjectTasks(List<AsyncTask> projectTasks) {
		this.projectTasks = projectTasks;
	}

	public List<AsyncTask> getGroupPendingTasks() {
		return groupPendingTasks;
	}

	public void setGroupPendingTasks(List<AsyncTask> groupPendingTasks) {
		this.groupPendingTasks = groupPendingTasks;
	}

	public List<AsyncTask> getLicensePendingTasks() {
		return licensePendingTasks;
	}

	public void setLicensePendingTasks(List<AsyncTask> licensePendingTasks) {
		this.licensePendingTasks = licensePendingTasks;
	}

	public List<AsyncTask> getProjectPendingTasks() {
		return projectPendingTasks;
	}

	public void setProjectPendingTasks(List<AsyncTask> projectPendingTasks) {
		this.projectPendingTasks = projectPendingTasks;
	}

	public AsyncTask getExecutingGroupTask() {
		return executingGroupTask;
	}

	public void setExecutingGroupTask(AsyncTask executingGroupTask) {
		this.executingGroupTask = executingGroupTask;
	}

	public AsyncTask getExecutingLicenseTask() {
		return executingLicenseTask;
	}

	public void setExecutingLicenseTask(AsyncTask executingLicenseTask) {
		this.executingLicenseTask = executingLicenseTask;
	}

	public AsyncTask getExecutingProjectTask() {
		return executingProjectTask;
	}

	public void setExecutingProjectTask(AsyncTask executingProjectTask) {
		this.executingProjectTask = executingProjectTask;
	}

	public List<AsyncTask> getRecentCompletedGroupTasks() {
		return recentCompletedGroupTasks;
	}

	public void setRecentCompletedGroupTasks(
			List<AsyncTask> recentCompletedGroupTasks) {
		this.recentCompletedGroupTasks = recentCompletedGroupTasks;
	}

	public List<AsyncTask> getRecentCompletedLicenseTasks() {
		return recentCompletedLicenseTasks;
	}

	public void setRecentCompletedLicenseTasks(
			List<AsyncTask> recentCompletedLicenseTasks) {
		this.recentCompletedLicenseTasks = recentCompletedLicenseTasks;
	}

	public List<AsyncTask> getRecentCompletedProjectTasks() {
		return recentCompletedProjectTasks;
	}

	public void setRecentCompletedProjectTasks(
			List<AsyncTask> recentCompletedProjectTasks) {
		this.recentCompletedProjectTasks = recentCompletedProjectTasks;
	}

	public AsyncTask getSelectedTask() {
		return selectedTask;
	}

	public void setSelectedTask(AsyncTask selectedTask) {
		this.selectedTask = selectedTask;
	}

	public ExecutorService getExecutorForSpawningTask() {
		return executorForSpawningTask;
	}

	public void setExecutorForSpawningTask(
			ExecutorService executorForSpawningTask) {
		this.executorForSpawningTask = executorForSpawningTask;
	}

	public SynchronizationTask getSyncTask() {
		return syncTask;
	}

	public void setSyncTask(SynchronizationTask syncTask) {
		this.syncTask = syncTask;
	}

	public StreamedContent getExportFile() {
		return exportFile;
	}

	public void setExportFile(StreamedContent exportFile) {
		this.exportFile = exportFile;
	}

}
