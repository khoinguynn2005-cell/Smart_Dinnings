package com.smartdining.utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

public class XParam {
	//doc 1 Param dang String
	public static String getString(String name) {
		return XHttp.getRequest().getParameter(name);
		
	}
	//doc nhieu Param co cung ten
	public static List<String> getValues(String name) {
		var values = XHttp.getRequest().getParameterValues(name);
		return values != null ? List.of(values) : List.of();
	}
	//doc nhieu Param va chuyen than Integer
	public static List<Integer> getIntValues(String name) {
		return getValues(name).stream().map(s -> Integer.valueOf(s)).toList();
	}
	//doc Param dang int
	public static int getInt(String name) {
		var value = XParam.getString(name);
		return Integer.parseInt(value);
	}
	//doc Param dang long(doc ID)
	public static long getLong(String name) {
		var value = XParam.getString(name);
		return Long.parseLong(value);
	}
	//doc Param dang double
	public static double getDouble(String name) {
		var value = XParam.getString(name);
		return Double.parseDouble(value);
	}
	//doc Param dang boolean(co check box)
	public static boolean getBoolean(String name) {
		var value = XParam.getString(name);
		return Boolean.parseBoolean(value == null ? "false" : value);
	}
	//doc Param diu dang yyy-MM-dd
	public static Date getDate(String name) {
		return XParam.getDate(name, "yyyy-MM-dd");
	}
	//doc Date theo dinh dang truyen vao
	public static Date getDate(String name, String pattern) {
		var value = XParam.getString(name);
		try {
			return new SimpleDateFormat(pattern).parse(value);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	//doc so tien
	public static BigDecimal getBigDecimal(String name) {
        var value = XParam.getString(name);

        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	//doc LocalDate theo dung dang yyy-MM-dd type"date"
	public static LocalDate getLocalDate(String name) {
        var value = XParam.getString(name);

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }
	// doc LocalTime type"time"
	public static LocalTime getLocalTime(String name) {
        var value = XParam.getString(name);

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }
	//doc LocalDateTime
	public static LocalDateTime getLocalDateTime(
            String name) {

        var value = XParam.getString(name);

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }
	//doc file upload
//	public static File getFile(String name, String folder) throws IOException, ServletException {
//		var part = XHttp.getRequest().getPart(name);
//		if(part.getSize() > 0) {
//			var filename = part.getSubmittedFileName();
//			filename = XStr.key() + filename.substring(filename.lastIndexOf("."));
//			var file = XPath.getRealPath(folder + "/" + filename);
//			part.write(file.getAbsolutePath());
//			return file;
//		}
//		return null;
//	}
}
