package com.lbest.rm.plugin;


public class BLJSDeviceInfo {

	private String deviceID;

	private String subDeviceID;

	private String deviceName;

	private String deviceMac;

	private String key;
	
	private int deviceStatus;
	
	private UserInfo user = new UserInfo();
	
	private BLNetworkStatusInfo networkStatus = new BLNetworkStatusInfo();

	public String getSubDeviceID() {
		return subDeviceID;
	}

	public void setSubDeviceID(String subDeviceID) {
		this.subDeviceID = subDeviceID;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public static class UserInfo{
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
	}

	public String getDeviceMac() {
		return deviceMac;
	}

	public void setDeviceMac(String deviceMac) {
		this.deviceMac = deviceMac;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}

	public UserInfo getUser() {
		return user;
	}

	public void setUser(UserInfo user) {
		this.user = user;
	}

	public BLNetworkStatusInfo getNetworkStatus() {
		return networkStatus;
	}

	public void setNetworkStatus(BLNetworkStatusInfo networkStatus) {
		this.networkStatus = networkStatus;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getDeviceStatus() {
		return deviceStatus;
	}

	public void setDeviceStatus(int deviceStatus) {
		this.deviceStatus = deviceStatus;
	}
	
	
}
