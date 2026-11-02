package com.smartdining.utils;

public class XAttr {
	
	//Request Scope
	public static void setRequest(String name, Object value) {
		XHttp.getRequest().setAttribute(name, value);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getRequest(String name) {
		return (T)XHttp.getRequest().getAttribute(name);
	}
	public static void removeRequest(String name) {
		XHttp.getRequest().removeAttribute(name);
	}
	
	//Session Scope
	public static void setSession(String name, Object value) {
		XHttp.getSession().setAttribute(name, value);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getSession(String name) {
		return (T)XHttp.getSession().getAttribute(name);
	}
	public static void removeSession(String name) {
		XHttp.getSession().removeAttribute(name);
	}
	
	//Application Scope
	public static void setContext(String name, Object value) {
		XHttp.getContext().setAttribute(name, value);
	}
	@SuppressWarnings("unchecked")
	public static <T> T getContext(String name) {
		return (T)XHttp.getContext().getAttribute(name);
	}
	public static void removeContext(String name) {
		XHttp.getContext().removeAttribute(name);
	}
}
