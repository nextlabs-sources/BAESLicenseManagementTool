package com.nextlabs.bae.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PowerShellHelper implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(PowerShellHelper.class);

	/**
	 * Execute a Powershell command
	 * 
	 * @param command
	 *            the command
	 * @exception Exception
	 *                Any exception
	 * @return True if succeed, False otherwise
	 */
	public static boolean executeCommand(String command) {
		Boolean result = true;
		try {

			List<String> cmdarray = new ArrayList<String>();
			URL path = PowerShellHelper.class
					.getResource("/com/nextlabs/bae/resources/psscript.ps1");
			File file = new File(path.toURI());
			String finalCommand = "powershell.exe \"-File "
					+ file.getAbsolutePath() + " '" + command + "'\"";
			LOG.info("PowerShellHelper executeCommand() executing "
					+ finalCommand);
			cmdarray.add(finalCommand);
			Process powerShellProcess = Runtime.getRuntime().exec(
					cmdarray.toArray(new String[0]));
			powerShellProcess.getOutputStream().close();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					powerShellProcess.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					powerShellProcess.getErrorStream()));
			String line;
			LOG.info("PowerShellHelper executeCommand() Output :");
			while ((line = stdInput.readLine()) != null) {
				LOG.info(line);
			}
			LOG.info("PowerShellHelper executeCommand() Error :");
			while ((line = stdError.readLine()) != null) {
				LOG.error(line);
				result = false;
			}
			stdInput.close();
			stdError.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Create new license in BJ
	 * 
	 * @param licenseName
	 *            License name
	 * @param selector
	 *            Selector
	 * @return True if succeed, False otherwise
	 */
	public static boolean createLicense(String licenseName, String selector) {
		return PowerShellHelper
				.executeCommand("Invoke-Command -ComputerName 10.23.58.100 "
						+ "-ConfigurationName Microsoft.Powershell32"
						+ " -ScriptBlock {get-selectorvalue -id " + selector
						+ " -name " + licenseName + "} " + "-credential $Cred");
	}

	/**
	 * Delete a license in BJ
	 * 
	 * @param licenseName
	 *            License name
	 * @param selector
	 *            Selector
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteLicense(String licenseName, String selector) {
		return PowerShellHelper
				.executeCommand("Invoke-Command -ComputerName 10.23.58.100 "
						+ "-ConfigurationName Microsoft.Powershell32"
						+ " -ScriptBlock {remove-selectorvalue -id " + selector
						+ " -name " + licenseName + "} " + "-credential $Cred");
	}

}
