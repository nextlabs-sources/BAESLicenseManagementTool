package com.nextlabs.bae.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.PropertyLoader;
import com.nextlabs.bae.helper.User;

public class LazyADDataModel extends LazyDataModel<User> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] attributes;
	private String filter;
	private byte[] cookie;
	private int total;
	private String user;
	private String password;
	private Control[] masterControl;

	private static final Log LOG = LogFactory.getLog(LazyADDataModel.class);

	public LazyADDataModel(String filter, String[] attributes, String user,
			String password) {
		try {
			this.filter = filter;
			this.attributes = attributes;
			this.cookie = null;
			this.user = user;
			this.password = password;
		} catch (Exception e) {
			LOG.info(e.getMessage());
		}
	}

	@Override
	public List<User> load(int first, int pageSize, String sortField,
			SortOrder sortOrder, Map<String, Object> filters) {
		List<User> data = new ArrayList<User>();

		try {
			LdapContext context = ActiveDirectoryHelper.getLDAPContext(user,
					password);
			if (cookie != null) {
				context.setRequestControls(new Control[] { new PagedResultsControl(
						pageSize, cookie, Control.CRITICAL) });
			} else {
				context.setRequestControls(new Control[] { new PagedResultsControl(
						pageSize, Control.CRITICAL) });
			}
			SearchControls constraint = new SearchControls();
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			LOG.info("LazyADDataModel load()" + filter);
			NamingEnumeration<SearchResult> answer = null;
			String domain = PropertyLoader.bAESProperties
					.getProperty("ldap-domain-name");
			try {
				answer = context.search(domain, filter, constraint);
			} catch (NoPermissionException pe) {
				LOG.info("LazyADDataModel load(): No permission to search AD.Try to use master account instead.");
				context = ActiveDirectoryHelper.getLDAPContext(
						PropertyLoader.bAESProperties
								.getProperty("edit-account-name"),
						PropertyLoader.bAESProperties
								.getProperty("edit-account-password"));
				try {
					answer = context.search(domain, filter, constraint);
				} catch (NamingException ne) {
					LOG.error("LazyADDataModel load(): " + ne.getMessage(), ne);
				}
			} catch (NamingException ne) {
				LOG.error("LazyADDataModel load(): " + ne.getMessage(), ne);
			}

			if (answer == null) {
				LOG.info("LazyADDataModel load(): Error in search: return null");
				return data;
			}

			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				data.add(new User(sr.getAttributes()));
			}

			// Examine the paged results control response
			masterControl = context.getResponseControls();
			if (masterControl != null) {
				for (int i = 0; i < masterControl.length; i++) {
					if (masterControl[i] instanceof PagedResultsResponseControl) {
						PagedResultsResponseControl prrc = (PagedResultsResponseControl) masterControl[i];
						total = prrc.getResultSize();
						LOG.info("LazyADDataModel load(): total : " + total);
						cookie = prrc.getCookie();
					}
				}
			} else {
				LOG.info("LazyADDataModel load(): No controls were sent from the server");
			}

			context.close();
		} catch (Exception e) {
			LOG.error("LazyADDataModel load(): " + e.getMessage(), e);
		}

		this.setRowCount(70);

		return data;
	}

	public String[] getAttributes() {
		return attributes;
	}

	public void setAttributes(String[] attributes) {
		this.attributes = attributes;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public byte[] getCookie() {
		return cookie;
	}

	public void setCookie(byte[] cookie) {
		this.cookie = cookie;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Control[] getMasterControl() {
		return masterControl;
	}

	public void setMasterControl(Control[] masterControl) {
		this.masterControl = masterControl;
	}

}
