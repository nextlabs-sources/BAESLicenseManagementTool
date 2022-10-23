package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.SortControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ActiveDirectoryHelper implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory
			.getLog(ActiveDirectoryHelper.class);

	private static final int ADS_GROUP_TYPE_GLOBAL_GROUP = 0x0002;
	/*
	 * private static final int ADS_GROUP_TYPE_DOMAIN_LOCAL_GROUP = 0x0004;
	 * private static final int ADS_GROUP_TYPE_LOCAL_GROUP = 0x0004; private
	 * static final int ADS_GROUP_TYPE_UNIVERSAL_GROUP = 0x0008;
	 */

	private static final int ADS_GROUP_TYPE_SECURITY_ENABLED = 0x80000000;

	/**
	 * Return LdapContext
	 * 
	 * @param userName
	 *            ldap user name
	 * 
	 * @param password
	 *            ldap password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return LdapContext
	 */
	public static LdapContext getLDAPContextFromPool(String userName,
			String password) {
		long lCurrentTime = System.nanoTime();
		LdapContext ctx = null;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(LdapContext.CONTROL_FACTORIES,
					"com.sun.jndi.ldap.ControlFactory");
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			if (PropertyLoader.bAESProperties.getProperty("ssl-authentication")
					.equals("true")) {
				LOG.info("LDAP Authenticated with SSL");
				env.put(Context.SECURITY_PROTOCOL, "ssl");
			}
			env.put(Context.SECURITY_PRINCIPAL, userName);
			env.put(Context.SECURITY_CREDENTIALS, password);
			env.put(Context.PROVIDER_URL,
					"ldap://"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-server")
							+ ":"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-port") + "/");
			env.put(Context.STATE_FACTORIES, "PersonStateFactory");
			env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
			env.put("com.sun.jndi.ldap.connect.pool", "true");
			ctx = new InitialLdapContext(env, null);

		} catch (Exception ex) {
			LOG.error(
					"ActiveDirectoryHelper getLDAPContextFromPool(): "
							+ ex.getMessage(), ex);
		}
		LOG.debug("ActiveDirectoryHelper getLDAPContextFromPool() completed. Time spent: "
				+ ((System.nanoTime() - lCurrentTime) / 1000000.00) + "ms");
		return ctx;
	}

	public static LdapContext getLDAPContext(String userName, String password) {
		long lCurrentTime = System.nanoTime();
		LdapContext ctx = null;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(LdapContext.CONTROL_FACTORIES,
					"com.sun.jndi.ldap.ControlFactory");
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			if (PropertyLoader.bAESProperties.getProperty("ssl-authentication")
					.equals("true")) {
				LOG.info("LDAP Authenticated with SSL");
				env.put(Context.SECURITY_PROTOCOL, "ssl");
			}
			env.put(Context.SECURITY_PRINCIPAL, userName);
			env.put(Context.SECURITY_CREDENTIALS, password);
			env.put(Context.PROVIDER_URL,
					"ldap://"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-server")
							+ ":"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-port") + "/");
			env.put(Context.STATE_FACTORIES, "PersonStateFactory");
			env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
			env.put("com.sun.jndi.ldap.connect.pool", "false");
			ctx = new InitialLdapContext(env, null);

		} catch (Exception ex) {
			LOG.error(
					"ActiveDirectoryHelper getLDAPContext(): "
							+ ex.getMessage(), ex);
		}
		LOG.debug("ActiveDirectoryHelper getLdapContext() completed. Time spent: "
				+ ((System.nanoTime() - lCurrentTime) / 1000000.00) + "ms");
		return ctx;
	}

	public static boolean authenticate(String userName, String password) {
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			env.put(LdapContext.CONTROL_FACTORIES,
					"com.sun.jndi.ldap.ControlFactory");
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			if (PropertyLoader.bAESProperties.getProperty("ssl-authentication")
					.equals("true")) {
				LOG.info("LDAP Authenticated with SSL");
				env.put(Context.SECURITY_PROTOCOL, "ssl");
			}
			env.put(Context.SECURITY_PRINCIPAL, userName);
			env.put(Context.SECURITY_CREDENTIALS, password);
			env.put(Context.PROVIDER_URL,
					"ldap://"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-server")
							+ ":"
							+ PropertyLoader.bAESProperties
									.getProperty("ad-port") + "/");
			env.put(Context.STATE_FACTORIES, "PersonStateFactory");
			env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
			env.put("com.sun.jndi.ldap.connect.pool", "false");
			LdapContext ctx = new InitialLdapContext(env, null);
			ctx.close();
			return true;

		} catch (Exception ex) {
			LOG.error(
					"ActiveDirectoryHelper authenticate(): " + ex.getMessage(),
					ex);
			return false;
		}
	}

	public static boolean testLdap(String userName, String password) {
		LdapContext context = ActiveDirectoryHelper.getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));

		if (context == null) {
			return false;
		} else {
			try {
				context.close();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			return true;
		}
	}

	/**
	 * Get user list from AD with selected attributes based on filter
	 * 
	 * @param filter
	 *            Search filter
	 * 
	 * @param attributes
	 *            Array of attribute to get
	 * 
	 * @param pageSize
	 *            size of page
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of searched users
	 */
	public static List<User> getUsers(String filter, String[] attributes,
			int pageSize) {
		List<User> returnUsers = new ArrayList<User>();
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {

			// user cookie and page size to handle cases when result set has
			// more than 1000 entries
			byte[] cookie = null;
			ctx.setRequestControls(new Control[] { new PagedResultsControl(
					pageSize, Control.NONCRITICAL) });
			int total;
			do {
				SearchControls constraint = new SearchControls();
				if (attributes != null) {
					constraint.setReturningAttributes(attributes);
				}
				constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
				constraint.setReturningObjFlag(false);

				LOG.info("ActiveDirectoryHelper getUsers(): Filter = " + filter);
				NamingEnumeration<SearchResult> answer = null;
				String domain = PropertyLoader.bAESProperties
						.getProperty("ldap-domain-name");
				try {
					answer = ctx.search(domain, filter, constraint);
				} catch (NamingException ne) {
					LOG.error(
							"ActiveDirectoryHelper searchAD(): "
									+ ne.getMessage(), ne);
				}

				if (answer == null) {
					LOG.info("ActiveDirectoryHelper getUsers(): Error in search: return null");
					return returnUsers;
				}

				while (answer.hasMoreElements()) {
					SearchResult sr = (SearchResult) answer.next();
					returnUsers.add(new User(sr.getAttributes()));
				}

				// Examine the paged results control response
				Control[] controls = ctx.getResponseControls();
				if (controls != null) {
					for (int i = 0; i < controls.length; i++) {
						if (controls[i] instanceof PagedResultsResponseControl) {
							PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
							total = prrc.getResultSize();
							LOG.info("total : " + total);
							cookie = prrc.getCookie();
						}
					}
				} else {
					LOG.info("ActiveDirectoryHelper getUsers(): No controls were sent from the server");
				}
				// Re-activate paged results
				ctx.setRequestControls(new Control[] { new PagedResultsControl(
						pageSize, cookie, Control.CRITICAL) });
			} while (cookie != null && cookie.length != 0);
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getUsers(): " + e.getMessage(), e);
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper getUsers(): " + e.getMessage(),
						e);
			}
		}
		return returnUsers;
	}

	/**
	 * Get concatenated string representing user list from AD with selected
	 * attributes based on filter
	 * 
	 * @param filter
	 *            Search filter
	 * 
	 * @param attributes
	 *            Array of attribute to get
	 * 
	 * @param pageSize
	 *            size of page
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of searched users
	 */
	public static String getUsersAsString(String filter, String identifier,
			String[] attributes, int pageSize) {
		if (attributes != null
				&& !Arrays.asList(attributes).contains(identifier)) {
			LOG.error("getUsersAsString() Invalid attributes "
					+ attributes.toString() + " and identifier " + identifier);
			return null;
		}

		StringBuilder result = new StringBuilder();
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {

			// user cookie and page size to handle cases when result set has
			// more than 1000 entries
			byte[] cookie = null;
			ctx.setRequestControls(new Control[] {
					new PagedResultsControl(pageSize, Control.NONCRITICAL),
					new SortControl("sAMAccountName", Control.CRITICAL) });
			int total;
			do {
				SearchControls constraint = new SearchControls();
				if (attributes != null) {
					constraint.setReturningAttributes(attributes);
				}
				constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
				constraint.setReturningObjFlag(false);

				LOG.info("ActiveDirectoryHelper getUsers(): Filter = " + filter);
				NamingEnumeration<SearchResult> answer = null;
				String domain = PropertyLoader.bAESProperties
						.getProperty("ldap-domain-name");
				try {
					answer = ctx.search(domain, filter, constraint);
				} catch (NamingException ne) {
					LOG.error(
							"ActiveDirectoryHelper searchAD(): "
									+ ne.getMessage(), ne);
				}

				if (answer == null) {
					LOG.info("ActiveDirectoryHelper getUsers(): Error in search: return null");
					return null;
				}

				while (answer.hasMoreElements()) {
					SearchResult sr = (SearchResult) answer.next();
					result.append(getAttribute(sr.getAttributes(), identifier)
							+ ", ");
				}

				// Examine the paged results control response
				Control[] controls = ctx.getResponseControls();
				if (controls != null) {
					for (int i = 0; i < controls.length; i++) {
						if (controls[i] instanceof PagedResultsResponseControl) {
							PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
							total = prrc.getResultSize();
							LOG.info("total : " + total);
							cookie = prrc.getCookie();
						}
					}
				} else {
					LOG.info("ActiveDirectoryHelper getUsers(): No controls were sent from the server");
				}
				// Re-activate paged results
				ctx.setRequestControls(new Control[] { new PagedResultsControl(
						pageSize, cookie, Control.CRITICAL) });
			} while (cookie != null && cookie.length != 0);
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getUsers(): " + e.getMessage(), e);
			return null;
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper getUsers(): " + e.getMessage(),
						e);
			}
		}

		if (result.length() > 0) {
			return result.substring(0, result.length() - 2);
		} else {
			return result.toString();
		}
	}

	/**
	 * Get users based on an existing user list which might be a list of string
	 * or a list of user with insufficient attributes
	 * 
	 * @param users
	 *            User list
	 * @param attributesToFetch
	 *            Attributes to fetch
	 * @return List of user
	 */
	public static List<User> getUsers(Object[] users, String[] attributesToFetch) {
		List<User> returnUsers = new ArrayList<User>();
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {
			String filter = "(|";
			for (Object user : users) {
				if (user instanceof String) {
					filter += "(sAMAccountName=" + user.toString() + ")";
				} else if (user instanceof User) {
					filter += "(sAMAccountName=" + ((User) user).getAduser()
							+ ")";
				}
			}
			filter += ")";
			LOG.debug("getUsers() filter = " + filter);
			NamingEnumeration<SearchResult> answer = null;
			SearchControls constraint = new SearchControls();
			if (attributesToFetch != null) {
				constraint.setReturningAttributes(attributesToFetch);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			constraint.setReturningObjFlag(false);
			String domain = PropertyLoader.bAESProperties
					.getProperty("ldap-domain-name");

			answer = ctx.search(domain, filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper getUsers(): Error in search: return null");
				return returnUsers;
			}

			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				returnUsers.add(new User(sr.getAttributes()));
			}

		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getUsers(): " + e.getMessage(), e);
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper getUsers(): " + e.getMessage(),
						e);
			}
		}
		return returnUsers;
	}

	/**
	 * Get suggested users with limit
	 * 
	 * @param filter
	 *            Search filter
	 * @param attributes
	 *            Returned Attributes
	 * @param limit
	 *            Limit
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 *            LdapContext
	 * @exception Exception
	 *                Any exception
	 * @return List of users
	 */
	public static List<User> getSuggestedUsers(String filter,
			String[] attributes, int limit) {
		LOG.info("ActiveDirectoryHelper getSuggestedUsers(): filter = "
				+ filter);
		List<User> results = new ArrayList<User>();
		try {

			SearchControls constraint = new SearchControls();
			constraint.setCountLimit(limit);
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				results.add(new User(sr.getAttributes()));
			}
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper getSuggestedUsers(): "
							+ e.getMessage(), e);
		}

		return results;
	}

	/**
	 * Return one user with selected attributes based on filter
	 * 
	 * @param filter
	 *            Search filter
	 * 
	 * @param attributes
	 *            Array of selected attributes
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return Searched user
	 */
	public static User getUser(String filter, String[] attributes) {
		User returnUser = null;
		try {

			LOG.info("ActiveDirectoryHelper getUser(): filter = " + filter);
			SearchControls constraint = new SearchControls();
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.error("ActiveDirectoryHelper getUser(): Error in search: return null");
				return returnUser;
			}

			if (!answer.hasMoreElements()) {
				LOG.info("ActiveDirectoryHelper getUser(): No user found!");
				return returnUser;
			}

			if (answer.hasMoreElements()) {
				LOG.info("ActiveDirectoryHelper getUser(): User found!");
				SearchResult sr = (SearchResult) answer.next();
				returnUser = new User(sr.getAttributes());
			}

			answer.close();
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getUser(): " + e.getMessage(), e);
		}
		return returnUser;
	}

	/**
	 * Perform general AD search and return search result. Get ldap context by
	 * itself
	 * 
	 * @param domain
	 *            Ldap domain
	 * 
	 * @param filter
	 *            Search filter
	 * 
	 * @param constraint
	 *            Search constraint
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception NopermissionException
	 *                Raised when given context does not have permission to
	 *                search AD. Will attempt to use master account to create a
	 *                new context
	 * 
	 * @exception NamingException
	 *                Any NamingException
	 * 
	 * @return Search result
	 */
	public static NamingEnumeration<SearchResult> searchAD(String domain,
			String filter, SearchControls constraint) {
		NamingEnumeration<SearchResult> answer = null;
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		constraint.setReturningObjFlag(false);
		try {
			answer = ctx.search(domain, filter, constraint);
		} catch (NamingException ne) {
			LOG.error("ActiveDirectoryHelper searchAD(): " + ne.getMessage(),
					ne);
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper getUsers(): " + e.getMessage(),
						e);
			}
		}
		return answer;
	}

	/**
	 * 
	 * @param domain
	 * @param filter
	 * @param constraint
	 * @param ctx
	 * @return
	 */
	/*
	 * public static NamingEnumeration<SearchResult> searchAD(String domain,
	 * String filter, SearchControls constraint, LdapContext ctx) {
	 * NamingEnumeration<SearchResult> answer = null; try { answer =
	 * ctx.search(domain, filter, constraint); } catch (NoPermissionException
	 * pe) { LOG.info(
	 * "ActiveDirectoryHelper searchAD(): No permission to search AD.Try to use master account instead."
	 * ); ctx = getLDAPContext( PropertyLoader.bAESProperties
	 * .getProperty("edit-account-name"), PropertyLoader.bAESProperties
	 * .getProperty("edit-account-password")); try { answer = ctx.search(domain,
	 * filter, constraint); } catch (NamingException ne) { LOG.error(
	 * "ActiveDirectoryHelper searchAD(): " + ne.getMessage(), ne); } } catch
	 * (NamingException ne) { LOG.error("ActiveDirectoryHelper searchAD(): " +
	 * ne.getMessage(), ne); } return answer; }
	 */

	/**
	 * Modify attribute of a group object in ldap
	 * 
	 * @param mods
	 *            Array of modifications
	 * 
	 * @param groupName
	 *            Name of the group
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception NoPermissionException
	 *                Raised when given context doesn't have permission to
	 *                modify AD - will use master account instead
	 * 
	 * @exception Any
	 *                other exception
	 * 
	 * @return String with OK value if succeed, otherwise the error if fail
	 */
	public static String modifyGroupAttribute(ModificationItem[] mods,
			String groupName) {
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {
			Attributes attributes = getAdGroup(groupName, null).getAttributes();
			if (attributes == null) {
				LOG.error("ActivityDirectoryHelper modifyGroupAttributes() Cannot find the specified group.");
				return PropertyLoader.bAESConstant
						.getProperty("TASK_INDIVIDUAL_ERROR");
			}
			String identifier = attributes.get("distinguishedName").toString();
			identifier = identifier.split(":")[1].trim();
			ctx.modifyAttributes(identifier, mods);

			return "OK";
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper modifyGroupAttribute(): "
							+ e.getMessage(), e);
			return PropertyLoader.bAESConstant
					.getProperty("TASK_INDIVIDUAL_ERROR");
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper modifyGroupAttribute(): "
								+ e.getMessage(), e);
			}
		}
	}

	/**
	 * Modify attribute of a user object in ldap and log the action to database
	 * 
	 * @param mods
	 *            Array of modifications
	 * 
	 * @param filter
	 *            Search filter of the user
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @param admin
	 *            The admin that perform the modification - used for logging
	 * 
	 * @param trigger
	 *            The trigger of the modification - used for logging
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return String with OK value if succeed, otherwise the error if fail
	 */
	public static String modifyUserAttribute(ModificationItem[] mods,
			String filter, String admin, String trigger) {
		LdapContext ctx = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {
			Attributes attributes = getUser(filter, null).getAttributes();

			if (attributes == null) {
				LOG.error("ActiveDirectoryHelper modifyUserAttribute() Cannot find the specified user");
				return PropertyLoader.bAESConstant
						.getProperty("TASK_INDIVIDUAL_ERROR");
			}

			String identifier = attributes.get("distinguishedName").toString();
			identifier = identifier.split(":")[1].trim();

			ctx.modifyAttributes(identifier, mods);

			// log the modification
			List<BAELog> logs = new ArrayList<BAELog>();
			for (int i = 0; i < mods.length; i++) {
				if (mods[i] == null) {
					continue;
				}
				Attribute attribute = mods[i].getAttribute();
				String attributeName = attribute.toString().split(":")[0]
						.trim();
				String attributeValue = attribute.toString().split(":")[1]
						.trim();
				String targetUser = getAttribute(attributes, "sAMAccountName");
				String action = "UNDEFINED";
				switch (mods[i].getModificationOp()) {
				case 1:
					action = "ADD";
					break;
				case 2:
					action = "REPLACE";
					break;
				case 3:
					action = "REMOVE";
					break;
				default:
					action = "UNDEFINED";
					break;
				}
				logs.add(new BAELog(new Timestamp(new Date().getTime()), admin,
						targetUser, action, attributeName, "", attributeValue,
						trigger));
			}

			LogDBHelper.createLogs(logs);

			return "OK";
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper modifyUserAttribute(): "
							+ e.getMessage(), e);
			return PropertyLoader.bAESConstant
					.getProperty("TASK_INDIVIDUAL_ERROR");
		} finally {
			try {
				ctx.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper modifyUserAttribute(): "
								+ e.getMessage(), e);
			}
		}
	}

	/**
	 * Get all groups of a user
	 * 
	 * @param filter
	 *            Search filter of user
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of String with all user groups
	 */
	public static List<String> getAllUserGroup(String filter) {
		List<String> result = new ArrayList<String>();
		try {
			String[] attributeID = new String[] { "memberOf" };
			Attributes attributes = getUser(filter, attributeID)
					.getAttributes();
			Attribute attr = attributes.get("memberOf");
			NamingEnumeration<?> nenum = attr.getAll();
			LOG.info("ActiveDirectoryHelper getAllUserGroup(): Found "
					+ attr.size() + " groups");
			while (nenum.hasMore()) {
				String value = (String) nenum.next();
				String[] tValue = value.split(",");
				tValue = tValue[0].split("=");
				value = tValue[1].trim();
				LOG.info("ActiveDirectoryHelper getAllUserGroup(): Member of: "
						+ value);
				result.add(value);
			}
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper getAllUserGroup(): "
							+ e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Initialize license list in DB at application start up based on existing
	 * data in AD
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return The license list built
	 */
	public static List<License> buildLicenseList() {
		Set<String> licenseSet = new HashSet<String>();
		List<License> licenseList = new ArrayList<License>();
		HashMap<String, String> attributeCategory = new HashMap<String, String>();
		HashMap<String, String> attributeLabel = new HashMap<String, String>();
		try {
			SearchControls constraint = new SearchControls();
			String filter = "(objectClass=user)";
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// get all users
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper buildLicenseList(): Error in search: return null");
				return licenseList;
			}

			// loop through all users
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();
				int index = 1;

				// loop through all user attributes
				while (true) {
					String attributeName = PropertyLoader.bAESProperties
							.getProperty("attr" + index);
					String label = PropertyLoader.bAESProperties
							.getProperty("label" + index);
					if (attributeName == null || label == null
							|| attributeName.trim().equals("")) {
						break;
					}

					// add value to set to prevent duplicate
					Attribute attribute = attributes.get(attributeName);
					if (attribute != null) {
						String value = attribute.toString().split(":")[1];
						String[] tLicenseArray = value.split(", ");
						for (String tLicense : tLicenseArray) {
							licenseSet.add(tLicense.trim());
							attributeCategory.put(tLicense.trim(),
									attributeName);
							attributeLabel.put(tLicense.trim(), label);
						}
					}
					index++;
				}
			}

			// add back to a list
			String[] tLicenseArray = licenseSet.toArray(new String[0]);
			for (String tLicense : tLicenseArray) {
				licenseList.add(new License(tLicense, "", attributeCategory
						.get(tLicense), attributeLabel.get(tLicense), null,
						null, 0, 0, null));
				LicenseDBHelper.createLicense(tLicense.trim(), "",
						attributeCategory.get(tLicense),
						attributeLabel.get(tLicense), null, null, 0, null);
			}

		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper buildLicenseList(): "
							+ e.getMessage(), e);
		}
		return licenseList;
	}

	/**
	 * Initialize User_License table in database at application start up based
	 * on data in AD
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True or false based on the success
	 */
	public static boolean buildUserLicense() {
		try {
			SearchControls constraint = new SearchControls();
			String filter = "(objectClass=user)";
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// get all users
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper buildUserList(): Error in search: return false");
				return false;
			}

			// loop through all users
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();
				int index = 1;

				// loop through all user attributes
				while (true) {
					String attributeName = PropertyLoader.bAESProperties
							.getProperty("attr" + index);
					String label = PropertyLoader.bAESProperties
							.getProperty("label" + index);
					if (attributeName == null || label == null
							|| attributeName.trim().equals("")) {
						break;
					}

					// add value to set to prevent duplicate
					Attribute attribute = attributes.get(attributeName);
					if (attribute != null) {
						String value = attribute.toString().split(":")[1];
						String[] tLicenseArray = value.split(", ");

						// update user_license table
						List<License> licenses = new ArrayList<License>();
						for (String l : tLicenseArray) {
							licenses.add(new License(l.trim(), null, null,
									null, null, null, 0, 0, null));
						}
						List<User> users = new ArrayList<User>();
						users.add(new User(attributes));

						UserLicenseDBHelper.assignLicensesToUsers(licenses,
								users);
					}
					index++;
				}
			}

		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper buildUserList(): " + e.getMessage(),
					e);
			return false;
		}
		return true;
	}

	/**
	 * Initialize project table in database at application start up based on
	 * data in AD
	 * 
	 * @param userName
	 *            Authorized user
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of built projects
	 */
	public static List<Project> buildProjectList() {
		Set<String> projectSet = new HashSet<String>();
		List<Project> projectList = new ArrayList<Project>();
		try {
			SearchControls constraint = new SearchControls();
			String filter = "(objectClass=user)";
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// get all users
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper buildProjectList(): Error in search: return null");
				return projectList;
			}

			// loop through all users
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();

				// loop through all user attributes
				String attributeName = PropertyLoader.bAESProperties
						.getProperty("pa");

				// add value to set to prevent duplicate
				Attribute attribute = attributes.get(attributeName);
				if (attribute != null) {
					String value = attribute.toString().split(":")[1];
					String[] tProjectArray = value.split(", ");
					for (String tProject : tProjectArray) {
						projectSet.add(tProject.trim());
					}
				}
			}

			// add back to a list
			String[] tProjectArray = projectSet.toArray(new String[0]);
			for (String tProject : tProjectArray) {
				projectList.add(new Project(tProject, "", 0));
				ProjectDBHelper.createNewProject(tProject.trim(), "");
			}

		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper buildProjectList(): "
							+ e.getMessage(), e);
		}
		return projectList;
	}

	/**
	 * Initialize User_Project table in database at application start up based
	 * on data in AD
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean buildUserProject() {
		try {
			SearchControls constraint = new SearchControls();
			String filter = "(objectClass=user)";
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// get all users
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper buildUserProject(): Error in search: return false");
				return false;
			}

			// loop through all users
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();

				// loop through all user attributes
				String attributeName = PropertyLoader.bAESProperties
						.getProperty("pa");

				// add value to set to prevent duplicate
				Attribute attribute = attributes.get(attributeName);
				if (attribute != null) {
					String value = attribute.toString().split(":")[1];
					String[] tProjectArray = value.split(", ");

					// update user_project table
					List<Project> projects = new ArrayList<Project>();
					for (String l : tProjectArray) {
						projects.add(new Project(l.trim(), "", 0));
					}
					List<User> users = new ArrayList<User>();
					users.add(new User(attributes));

					UserProjectDBHelper.assignProjectsToUsers(projects, users);
				}
			}

		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper buildUserProject(): "
							+ e.getMessage(), e);
		}
		return true;
	}

	/**
	 * Get all users who has a particular license
	 * 
	 * @param license
	 *            The license
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of users
	 */
	public static List<User> getUsersByLicense(License license,
			String[] attributesToFetch) {
		List<User> returnUsers = new ArrayList<User>();
		try {
			SearchControls constraint = new SearchControls();
			if (attributesToFetch != null) {
				constraint.setReturningAttributes(attributesToFetch);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String filter = "(&(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")(" + license.getCategory().trim() + "="
					+ license.getName().trim() + "))";
			LOG.info("ActiveDirectoryHelper getUsersByLicense(): Get user by license using filter "
					+ filter);
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper getUsersByLicense(): Error in search: return null");
				return returnUsers;
			}
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attributes = sr.getAttributes();
				returnUsers.add(new User(attributes));

			}
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper getUsersByLicense(): "
							+ e.getMessage(), e);
		}
		return returnUsers;
	}

	/**
	 * Get all users who are members of a particular project
	 * 
	 * @param project
	 *            The project name
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of users
	 */
	public static List<User> getUsersByProject(String project,
			String[] attributesToFetch) {
		List<User> returnUsers = new ArrayList<User>();
		try {
			SearchControls constraint = new SearchControls();
			if (attributesToFetch != null) {
				constraint.setReturningAttributes(attributesToFetch);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String filter = "(&(objectClass=user)(mail=*"
					+ PropertyLoader.bAESProperties.getProperty("email-filter")
					+ ")(" + PropertyLoader.bAESProperties.getProperty("pa")
					+ "=" + project.trim() + "))";
			LOG.info("ActiveDirectoryHelper getUsersByProject(): Get user by project using filter "
					+ filter);
			returnUsers = getUsers(filter, null, 1000);
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper getUsersByProject(): "
							+ e.getMessage(), e);
		}
		return returnUsers;
	}

	/**
	 * Delete the existence of a license value in AD (in all users)
	 * 
	 * @param license
	 *            The license
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @param admin
	 *            The admin who performed this action
	 * 
	 * @param trigger
	 *            The trigger of the action
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteLicenseFromAD(License license, String admin,
			String trigger) {
		try {
			SearchControls constraint = new SearchControls();
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String filter = "(" + license.getCategory().trim() + "="
					+ license.getName().trim() + ")";
			LOG.info("ActiveDirectoryHelper deleteLicenseFromAD(): Get user by license using filter "
					+ filter);
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper deleteLicenseFromAD(): Error in search: return null");
				return false;
			}

			// loop through all users that have this license
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attributes = sr.getAttributes();
				String account = getAttribute(attributes, "sAMAccountName");

				// get the old license value
				String oldLicense = getAttribute(attributes,
						license.getCategory());

				// delete the license value form the string
				String newLicense = oldLicense.replace(license.getName(), "");

				// when the license value is at the middle of the string
				newLicense = newLicense.replace(";;", ";");

				if (newLicense.endsWith(";")) {
					newLicense = newLicense.substring(0,
							newLicense.length() - 1);
				}

				if (newLicense.trim().length() == 0) {
					// clear the value
					newLicense = null;
				}

				// modify the ad
				ModificationItem[] mods = new ModificationItem[1];
				mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
						new BasicAttribute(license.getCategory(),
								license.getName()));
				filter = "(sAMAccountName=" + account + ")";
				modifyUserAttribute(mods, filter, admin, trigger);
			}
			return true;
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper deleteLicenseFromAD(): "
							+ e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Delete the existence of a project value in AD (all users)
	 * 
	 * @param project
	 *            The project
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @param admin
	 *            The admin who performed the action
	 * 
	 * @param trigger
	 *            The trigger of the action
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteProjectFromAD(Project project, String admin,
			String trigger) {
		try {
			SearchControls constraint = new SearchControls();
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String filter = "("
					+ PropertyLoader.bAESProperties.getProperty("pa") + "=*"
					+ project.getName().trim() + "*)";
			LOG.info("ActiveDirectoryHelper deleteProjectFromAD(): Get user by project using filter "
					+ filter);
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper deleteProjectFromAD(): Error in search: return null");
				return false;
			}

			// loop through all users that have this license
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attributes = sr.getAttributes();
				String account = getAttribute(attributes, "sAMAccountName");

				// modify the ad
				ModificationItem[] mods = new ModificationItem[1];
				mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
						new BasicAttribute(PropertyLoader.bAESProperties
								.getProperty("pa"), project.getName()));
				filter = "(sAMAccountName=" + account + ")";
				modifyUserAttribute(mods, filter, admin, trigger);
			}
			return true;
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper deleteProjectFromAD(): "
							+ e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Create a group in AD (for license group)
	 * 
	 * @param groupName
	 *            Name of the gorup
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean createADGroup(String groupName) throws NameAlreadyBoundException{
		LdapContext context = getLDAPContext(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));

		Context result = null;
		try {
			groupName = groupName.replaceAll("[^a-zA-Z0-9 -]", "_");

			String group = "CN="
					+ groupName
					+ ","
					+ PropertyLoader.bAESProperties
							.getProperty("group-domain-name");
			Attributes attrs = new BasicAttributes(true);
			attrs.put("objectClass", "group");
			attrs.put("sAMAccountName", groupName);
			attrs.put("cn", groupName);
			attrs.put("description", "Security Group for license " + groupName
					+ ".");

			attrs.put(
					"groupType",
					Integer.toString(ADS_GROUP_TYPE_GLOBAL_GROUP
							+ ADS_GROUP_TYPE_SECURITY_ENABLED));

			result = context.createSubcontext(group, attrs);

			return true;
		} catch (NameAlreadyBoundException ex) {
			throw ex;
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper createADGroup(): " + e.getMessage(),
					e);
			return false;
		} finally {
			try {
				context.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper createADGroup(): "
								+ e.getMessage(), e);
			}

			if (result != null) {
				try {
					result.close();
				} catch (Exception e) {
					LOG.error(
							"ActiveDirectoryHelper createADGroup(): "
									+ e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Delete a group in AD (license group)
	 * 
	 * @param groupName
	 *            Name of the group
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean deleteADGroup(String groupName, String userName,
			String password) {
		LdapContext context = getLDAPContextFromPool(
				PropertyLoader.bAESProperties.getProperty("edit-account-name"),
				PropertyLoader.bAESProperties
						.getProperty("edit-account-password"));
		try {
			groupName = groupName.replaceAll("[^a-zA-Z0-9 -]", "_");
			String group = "CN="
					+ groupName
					+ ","
					+ PropertyLoader.bAESProperties
							.getProperty("group-domain-name");

			// delete the context
			context.destroySubcontext(group);

			return true;
		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper deleteADGroup(): " + e.getMessage(),
					e);
			return false;
		} finally {
			try {
				context.close();
			} catch (Exception e) {
				LOG.error(
						"ActiveDirectoryHelper getUsers(): " + e.getMessage(),
						e);
			}
		}
	}

	/**
	 * Get a group in AD
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @param groupName
	 *            Name of the group
	 * 
	 * @param attributes
	 *            Selected attributes to be returned
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return The group
	 */
	public static Group getAdGroup(String groupName, String[] attributes) {
		Group returnGroup = null;
		groupName = groupName.replaceAll("[^a-zA-Z0-9 -]", "_");
		String filter = "sAMAccountName=" + groupName;
		try {
			SearchControls constraint = new SearchControls();
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("group-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper getAdGroup(): Error in search: return null");
				return returnGroup;
			}
			if (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				returnGroup = new Group(sr.getAttributes());
			}
			answer.close();
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getAdGroup(): " + e.getMessage(),
					e);
		}
		return returnGroup;
	}

	/**
	 * Get a list of group in AD based on search filter
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @param filter
	 *            Search filter
	 * 
	 * @param attributes
	 *            Selected attributes to be returned
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return List of groups
	 */
	public static List<Group> getAdGroups(String filter, String[] attributes) {
		List<Group> returnList = new ArrayList<Group>();
		try {
			SearchControls constraint = new SearchControls();
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("group-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper getAdGroups(): Error in search: return empty list");
				return returnList;
			}
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Group group = new Group(sr.getAttributes());
				returnList.add(group);
			}
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getAdGroups(): " + e.getMessage(),
					e);
		}
		return returnList;
	}

	/**
	 * Get ad groups based for suggestion, limit to top 20 results
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password LdapContext
	 * @param filter
	 *            Search filter
	 * @param attributes
	 *            Attributes to be returned
	 * @exception Exception
	 *                Any exception
	 * @return List of groups
	 */
	public static List<Group> getSuggestedAdGroups(String filter,
			String[] attributes) {
		List<Group> returnList = new ArrayList<Group>();
		try {
			SearchControls constraint = new SearchControls();
			if (attributes != null) {
				constraint.setReturningAttributes(attributes);
			}
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);
			constraint.setCountLimit(20);

			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("group-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper getAdGroups(): Error in search: return empty list");
				return returnList;
			}
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Group group = new Group(sr.getAttributes());
				returnList.add(group);
			}
		} catch (Exception e) {
			LOG.error("ActiveDirectoryHelper getAdGroups(): " + e.getMessage(),
					e);
		}
		return returnList;
	}

	/**
	 * Initialize license groups based on licenses from database
	 * 
	 * @param userName
	 *            Authorized user
	 * 
	 * @param password
	 *            Password
	 * 
	 * @exception Any
	 *                exception
	 * 
	 * @return True if succeed, False otherwise
	 */
	public static boolean buildAdGroups() {
		try {
			SearchControls constraint = new SearchControls();
			String filter = "(objectClass=user)";
			constraint.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// create all groups
			List<License> licenses = LicenseDBHelper.getAllLicense();

			for (License license : licenses) {
				String groupName = license.getName();
				if (getAdGroup(groupName, null) == null) {
					createADGroup(groupName);
				}
			}

			// get all users
			NamingEnumeration<SearchResult> answer = searchAD(
					PropertyLoader.bAESProperties
							.getProperty("ldap-domain-name"),
					filter, constraint);

			if (answer == null) {
				LOG.info("ActiveDirectoryHelper buildAdGroups(): Error in search: return false");
				return false;
			}

			// loop through all users
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();
				int index = 1;

				// loop through all user attributes
				while (true) {
					String attributeName = PropertyLoader.bAESProperties
							.getProperty("attr" + index);
					String label = PropertyLoader.bAESProperties
							.getProperty("label" + index);
					if (attributeName == null || label == null
							|| attributeName.trim().equals("")) {
						break;
					}

					// add value to set to prevent duplicate
					Attribute attribute = attributes.get(attributeName);
					if (attribute != null) {
						String value = attribute.toString().split(":")[1];
						String[] tLicenseArray = value.split(", ");

						for (String license : tLicenseArray) {
							List<ModificationItem> mods = new ArrayList<ModificationItem>();
							String groupName = license.trim();
							Group adGroup = getAdGroup(groupName, null);

							// in case there are still inconsistencies between
							// AD
							// and DB
							if (adGroup == null) {
								createADGroup(groupName);
								adGroup = getAdGroup(groupName, null);
							}

							String members = adGroup.getAttribute("member");
							if (members == null
									|| !members.contains(getAttribute(
											attributes, "distinguishedName"))) {
								mods.add(new ModificationItem(
										DirContext.ADD_ATTRIBUTE,
										new BasicAttribute("member",
												getAttribute(attributes,
														"distinguishedName"))));
							}

							modifyUserAttribute(
									mods.toArray(new ModificationItem[0]),
									"sAMAccountName=" + adGroup.getGroupName(),
									"SYSTEM", "INITIAL SYNCHRONIZATION");
						}

					}
					index++;
				}
			}

		} catch (Exception e) {
			LOG.error(
					"ActiveDirectoryHelper buildAdGroups(): " + e.getMessage(),
					e);
			return false;
		}
		return true;
	}

	/**
	 * Get the value of an attribute of an object
	 * 
	 * @param attributes
	 *            Attributes of the object
	 * 
	 * @param attributeName
	 *            Name of the attribute to be query
	 * 
	 * @return Value of the attribute
	 */
	public static String getAttribute(Attributes attributes,
			String attributeName) {
		Attribute attrGet = attributes.get(attributeName);
		if (attrGet == null || attrGet.toString().trim().equals("")) {
			return "";
		} else {
			String temp = attrGet.toString();
			String[] tList = temp.split(":");
			return tList[1].trim();
		}
	}
}
