package com.lbest.rm.common;

import com.alibaba.fastjson.JSON;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 设备Suid
 * @author YeJin
 *
 */
public class BLDevSuidInfo {

	private String suid;
	
	private org.json.JSONObject intfs;

	public String getSuid() {
		return suid;
	}

	public void setSuid(String suid) {
		this.suid = suid;
	}

	public JSONObject getIntfs() {
		return intfs;
	}

	public void setIntfs(JSONObject intfs) {
		this.intfs = intfs;
	}
	
	/**
	 * 获取设备的接口列表
	 * 
	 * @return
	 * 		List<String> 支持的接口名称列表	
	 */
	public List<String> getIntfsList(){
		if(intfs != null){
			List<String> keyList = new LinkedList<>();
			Iterator<String> it = intfs.keys();

			while (it.hasNext()){
				keyList.add(it.next());
			}
			
			return keyList;
		}
		return null;
	}
	
	/***
	 * 获取接口对应的值
	 * 
	 * @param key
	 * 
	 * @return
	 */
	public List<BLDevProfileInftsValueInfo> getIntfValue(String key){
		if(key == null || intfs.isNull(key)){
			return null;
		}
		
		return JSON.parseArray(intfs.optJSONArray(key).toString(),
				BLDevProfileInftsValueInfo.class);
	}
	
}
