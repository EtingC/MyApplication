package com.lbest.rm.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口的值
 * @author YeJin
 *
 */
public class BLDevProfileInftsValueInfo {
	public static final int UNABLE_SCENE_AND_ENABLE_TRIGGER = 1;

	public static final int ENABLE_SCENE_AND_UNABLE_TRIGGER = 2;

	public static final int ENABLE_SCENE_AND_ENABLE_TRIGGER = 3;

	private int idx;
	
	private int act;

	/**
	 * -1 0 表示该功能不可触发联动，也不可加入场景
	 * 1    表示该功能可以触发联动，也不可加入场景
	 * 2    表示该功能不可触发联动，但可以加入场景
	 * 3    表示该功能可以触发联动，也可以加入场景
	 * **/
	private int ifttt = -1;

	private List<Integer> in = new ArrayList<Integer>();

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public int getAct() {
		return act;
	}

	public void setAct(int act) {
		this.act = act;
	}

	public List<Integer> getIn() {
		return in;
	}

	public void setIn(List<Integer> in) {
		this.in = in;
	}

	public int getIfttt() {
		return ifttt;
	}

	public void setIfttt(int ifttt) {
		this.ifttt = ifttt;
	}
}
