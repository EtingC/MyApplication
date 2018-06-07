package com.lbest.rm.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 2017/11/2.
 */

public class DrpDescInfo {
    private int id;

//	private List<Integer> pid = new ArrayList<Integer>();

    private List<String> suid = new ArrayList<String>();

    private int version;

    private String default_lang;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

//	public List<Integer> getPid() {
//		return pid;
//	}
//
//	public void setPid(List<Integer> pid) {
//		this.pid = pid;
//	}

    public List<String> getSuid() {
        return suid;
    }

    public void setSuid(List<String> suid) {
        this.suid = suid;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDefault_lang() {
        return default_lang;
    }

    public void setDefault_lang(String default_lang) {
        this.default_lang = default_lang;
    }
}
