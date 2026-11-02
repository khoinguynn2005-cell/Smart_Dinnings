package com.smartdining.utils;

import java.io.IOException;

import com.smartdining.entity.Employee;
import com.smartdining.enums.EmployeeRole;

//lam viec voi User dang dang nhap 
public class XAuth {
	public static void setEmployee(Employee employee) {
		XHttp.getSession().setAttribute("employee", employee);
	}
	
	public static Employee getEmployee() {
		return (Employee) XHttp.getSession().getAttribute("user");
	}

	public static void removeUser() {
		XHttp.getSession().removeAttribute("user");
	}

	public static boolean isAuthenticated() {
		return getEmployee() != null;
	}

	public static boolean isAdmin() {
		return isAuthenticated()
				&& getEmployee().getRole() == EmployeeRole.ADMIN;
	}

	public static void saveUrl() {// luu URL dang muon truy cap
		var savedUrl = XPath.getRequestUrl().toString();
		var queryString = XPath.getQueryString();

		if (queryString != null && !queryString.isBlank()) {
			savedUrl += "?" + queryString;
		}

		XAttr.setSession("saved-url", savedUrl);
	}

	public static boolean backToSavedUrl() throws IOException {// dua ve URL da luu 
		var savedUrl = (String) XAttr.getSession("saved-url");

		if (savedUrl != null && !savedUrl.isBlank()) {
			XAttr.removeSession("saved-url");
			XHttp.getResponse().sendRedirect(savedUrl);
			return true;
		}

		return false;
	}

	public static void logoff() {// xoa User dang dang nhap va xoa URL da luu
		XAuth.removeUser();
		XAttr.removeSession("saved-url");
	}
}
