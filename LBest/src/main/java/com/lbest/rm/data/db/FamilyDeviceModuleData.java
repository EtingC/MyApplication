package com.lbest.rm.data.db;

import com.alibaba.fastjson.serializer.SerializeFilter;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by dell on 2017/11/28.
 */
@DatabaseTable(tableName="moduleDevTable", daoClass = FamilyDeviceModuleDao.class)
public class FamilyDeviceModuleData implements Serializable{
    private static final long serialVersionUID = -4333316296251054416L;

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private String moduleid;
    @DatabaseField
    private String moduleName;
    @DatabaseField
    private String moduleIcon;
    @DatabaseField
    private String familyId;
    @DatabaseField
    private String roomId;
    @DatabaseField
    private String did;
    @DatabaseField
    private String sDid;
    @DatabaseField
    private String mac;
    @DatabaseField
    private String pid;
    @DatabaseField
    private String name;
    @DatabaseField
    private int password;
    @DatabaseField
    private int type;
    @DatabaseField
    private boolean lock;
    @DatabaseField
    private String aeskey;
    @DatabaseField
    private int terminalId;
    @DatabaseField
    private int subdeviceNum;
    @DatabaseField
    private String longitude;
    @DatabaseField
    private String latitude;
    @DatabaseField
    private String wifimac;
    @DatabaseField
    private String extend;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getsDid() {
        return sDid;
    }

    public void setsDid(String sDid) {
        this.sDid = sDid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPassword() {
        return password;
    }

    public void setPassword(int password) {
        this.password = password;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public String getAeskey() {
        return aeskey;
    }

    public void setAeskey(String aeskey) {
        this.aeskey = aeskey;
    }

    public int getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(int terminalId) {
        this.terminalId = terminalId;
    }

    public int getSubdeviceNum() {
        return subdeviceNum;
    }

    public void setSubdeviceNum(int subdeviceNum) {
        this.subdeviceNum = subdeviceNum;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getWifimac() {
        return wifimac;
    }

    public void setWifimac(String wifimac) {
        this.wifimac = wifimac;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    public String getModuleid() {
        return moduleid;
    }

    public void setModuleid(String moduleid) {
        this.moduleid = moduleid;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleIcon() {
        return moduleIcon;
    }

    public void setModuleIcon(String moduleIcon) {
        this.moduleIcon = moduleIcon;
    }


}
