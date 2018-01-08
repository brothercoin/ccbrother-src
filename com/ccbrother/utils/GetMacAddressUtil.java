package com.hykj.ccbrother.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

public class GetMacAddressUtil {
	public static void main(String[] args) throws Exception {
		InetAddress ia = InetAddress.getLocalHost();
		System.out.println(getLocalMac(ia));
	}

	public static String getLocalMac(InetAddress ia) throws SocketException {
		byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < mac.length; i++) {
			if (i != 0) {
				buffer.append(":");
			}
			// 字节转换为整数
			int temp = mac[i] & 0xff;
			String str = Integer.toHexString(temp);
			if (str.length() == 1) {
				buffer.append("0" + str);
			} else {
				buffer.append(str);
			}
		}
		return buffer.toString();
	}
}
