package com.nextlabs.bae.service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import com.nextlabs.bae.helper.ActiveDirectoryHelper;
import com.nextlabs.bae.helper.User;

@WebService
public class ActiveDirectoryService {

	@WebMethod
	public User getUser(@WebParam(name = "filter") String filter,
			@WebParam(name = "username") String userName,
			@WebParam(name = "password") String password) {
		if (filter == null) {
			return null;
		}
		String[] attributes = { "sAMAccountName", "mail", "displayName" };
		return ActiveDirectoryHelper.getUser(filter, attributes);
	}
}
