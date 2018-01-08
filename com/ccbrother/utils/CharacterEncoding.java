package com.hykj.ccbrother.utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

public class CharacterEncoding implements Filter {
	protected FilterConfig filterConfig;
	String encoding = null;

	public void destroy() {
		this.filterConfig = null;
	}

	/**
	 * 初始化
	 */
	public void init(FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
	}

	/**
	 * 将 inStr 转为 UTF-8 的编码形式
	 * 
	 * @param inStr
	 *            输入字符串
	 * @return UTF - 8 的编码形式的字符串
	 * @throws UnsupportedEncodingException
	 */
	private String toUTF(String inStr) throws UnsupportedEncodingException {
		String outStr = "";
		if (inStr != null) {
			outStr = new String(inStr.getBytes("iso-8859-1"), "UTF-8");
		}
		return outStr;
	}

	/**
	 * 中文乱码过滤处理
	 */
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// 获得请求的方式 (1.post or 2.get), 根据不同请求方式进行不同处理
		String method = request.getMethod();
		// 1. 以 post 方式提交的请求 , 直接设置编码为 UTF-8
		if (method.equalsIgnoreCase("post")) {
			try {
				request.setCharacterEncoding("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		// 2. 以 get 方式提交的请求
		else {
			// 取出客户提交的参数集
			@SuppressWarnings("unchecked")
			Enumeration<String> paramNames = request.getParameterNames();
			// 遍历参数集取出每个参数的名称及值
			while (paramNames.hasMoreElements()) {
				String name = paramNames.nextElement(); // 取出参数名称
				String values[] = request.getParameterValues(name); // 根据参数名称取出其值
				// 如果参数值集不为空
				if (values != null) {
					// 遍历参数值集
					for (int i = 0; i < values.length; i++) {
						try {
							// 回圈依次将每个值调用 toUTF(values[i]) 方法转换参数值的字元编码
							String vlustr = toUTF(values[i]);
							values[i] = vlustr;
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}
					// 将该值以属性的形式藏在 request
					request.setAttribute(name, values);
				}
			}

		}
		// 设置响应方式和支持中文的字元集
		response.setContentType("text/html;charset=UTF-8");

		// 继续执行下一个 filter, 无一下个 filter 则执行请求
		chain.doFilter(request, response);
	}
}