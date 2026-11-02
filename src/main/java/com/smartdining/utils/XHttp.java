package com.smartdining.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class XHttp {
	private static Map<Long, ServletRequest> requests = new ConcurrentHashMap<>();
	private static Map<Long, ServletResponse> responses = new ConcurrentHashMap<>();

	public static void  add(ServletRequest request, ServletResponse response) {
		var key = Thread.currentThread().threadId();
		requests.put( key, request);
		responses.put(key, response);
	}
	
	public static void remove() {
		var key = Thread.currentThread().threadId();
		requests.remove(key);
		responses.remove(key);
	}
	
	public static HttpServletRequest getRequest() {
		var key = Thread.currentThread().threadId();
		return (HttpServletRequest) requests.get(key);
	}
	
	public static HttpServletResponse getResponse() {
		var key = Thread.currentThread().threadId();
		return (HttpServletResponse) responses.get(key);
	}
	
	public static HttpSession getSession() {
		return getRequest().getSession();
	}
	
	public static ServletContext getContext() {
		return getRequest().getSession().getServletContext();
	}
	
	public static boolean is(String method) {
		return getRequest().getMethod().equalsIgnoreCase(method);
	}
}	
