package com.smartdining.utils;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.ServletException;

public class XPath {

	public static StringBuffer getRequestUrl() {
		return XHttp.getRequest().getRequestURL();
	}

	public static String getRequestUri() {
		return XHttp.getRequest().getRequestURI();
	}

	public static String getQueryString() { //la noi dung sau dau ?
		return XHttp.getRequest().getQueryString();
	}

	public static String getContextPath() {// lay ten ung dung ${ctx}
		return XHttp.getRequest().getContextPath();
	}

	public static String getServletPath() {// duong dan anh xa toi Servlet
		return XHttp.getRequest().getServletPath();
	}

	public static String getPathInfo() { // duogn dan sau servlet mapping /*
		return XHttp.getRequest().getPathInfo();
	}

	public static File getRealPath(String path) { // bien duogn dan web thanh duong dan that tren computer
		var file = new File(XHttp.getContext().getRealPath(path));
		file.getParentFile().mkdirs();
		return file;
	}
	
	public static void forward(String view)
			throws ServletException, IOException {

		var req = XHttp.getRequest();
		var resp = XHttp.getResponse();

		req.getRequestDispatcher(view).forward(req, resp);
	}
	
	
	public static void redirect(String url) throws ServletException, IOException {
		var resp = XHttp.getResponse();
		var ctx = XPath.getContextPath();
		resp.sendRedirect(url.contains(ctx) ? url : ctx + url);
	}

	public static boolean contains(String str) {
		return XPath.getServletPath().contains(str);
	}
	
	public static String getString() {
		var info = XPath.getPathInfo();
		return info != null ? info.substring(1) : null;
	}
	
	public static int getInt() {
		return Integer.parseInt(getString());
	}
	
	public static long getLong() {
		return Long.parseLong(getString());
	}
	
	public static double getDouble() {
		return Double.parseDouble(getString());
	}
	
	public static boolean getBoolean() {
		return Boolean.parseBoolean(getString());
	}


}
