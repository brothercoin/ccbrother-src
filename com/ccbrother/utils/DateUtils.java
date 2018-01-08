package com.hykj.ccbrother.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

	public final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public final static String DATE_FORMAT_NOTIME = "yyyy-MM-dd";



	/**
	 * 格式化日期
	 * 
	 * @param date
	 * @param pattern
	 * @return
	 */
	public static String getFormatTime(Date date, String pattern) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			return simpleDateFormat.format(date).toString();
		} catch (Exception e) {
			return "";
		}
	}

	public static Date getFormatDate(String date, String pattern) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		try {
			return simpleDateFormat.parse(date);
		} catch (Exception e) {
			return null;
		}
	}

	public static Date getFormatDate(String date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
		try {
			return simpleDateFormat.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * 偏移时间（分钟）
	 * 
	 * @param date
	 *            时间
	 * @param offset
	 *            偏移量，负数代表往前，正数代表往后
	 * @return
	 */
	public static Date offSetMin(Date date, int offset) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, offset);
		return cal.getTime();
	}

	/**
	 * 
	 * @param date
	 * @param field
	 * @param offset
	 * @return
	 */
	public static Date offSet(Date date, int field, int offset) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(field, offset);
		return cal.getTime();

	}

	public static String addSecond(Integer second) {
		Long currentTime = (long) (System.currentTimeMillis() + second * 1000);
		Date date = new Date(currentTime);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowTime = df.format(date);

		return nowTime;
	}

	public static String addDay(Integer days) {
		Calendar ca = Calendar.getInstance();
		ca.add(Calendar.DATE, days);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowTime = df.format(ca.getTime());

		return nowTime;
	}

	public static Long parseDateTomillion(Date date) {
		long millionSeconds = 0;
		try {
			String nowTime = getFormatTime(date, "yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			millionSeconds = format.parse(nowTime).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return millionSeconds;
	}

	public static void main(String[] args) throws ParseException {
		String f = getFormatTime(new Date(), "yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		long millionSeconds = sdf.parse(f).getTime();// 毫秒
		System.out.println(millionSeconds);

		Date date = new Date(millionSeconds);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowTime = df.format(date);
		System.out.println(nowTime);
	}


	/************************************
	 * 另一个类
	 */
	/** 时间格式(yyyy-MM-dd) */
	public final static String DATE_PATTERN = "yyyy-MM-dd";
	/** 时间格式(yyyy-MM-dd HH:mm:ss) */
	public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	public static String format(Date date) {
		return format(date, DATE_PATTERN);
	}

	public static String format(Date date, String pattern) {
		if(date != null){
			SimpleDateFormat df = new SimpleDateFormat(pattern);
			return df.format(date);
		}
		return null;
	}
}
