package com.lbest.rm.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by dell on 2017/7/28.
 */

public class BaseDeviceInfo implements Parcelable{

    private String id;    //	设备uuid
    private String uuid;    //	设备uuid
    private String snDevice;
    private String sn;        //	设备sn
    private String macDevice;
    private String mac;    //	设备mac
    private String model;        //	设备model
    private String displayName;        //	设备昵称
    private String nickName;
    private String thumbnail;    //	设备图标
    private String cid;        //	芯片id

    public BaseDeviceInfo(){}

    public int describeContents()
    {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(id);
        out.writeString(uuid);
        out.writeString(snDevice);
        out.writeString(sn);
        out.writeString(macDevice);
        out.writeString(mac);
        out.writeString(model);
        out.writeString(displayName);
        out.writeString(thumbnail);
        out.writeString(cid);
        out.writeString(nickName);
    }

    public static final Parcelable.Creator<BaseDeviceInfo> CREATOR = new Parcelable.Creator<BaseDeviceInfo>()
    {
        public BaseDeviceInfo createFromParcel(Parcel in)
        {
            return new BaseDeviceInfo(in);
        }

        public BaseDeviceInfo[] newArray(int size)
        {
            return new BaseDeviceInfo[size];
        }
    };

    public BaseDeviceInfo(Parcel in)
    {
        readFromParcel(in);
    }


    public void readFromParcel(Parcel in) {
        id = in.readString();
        uuid = in.readString();
        snDevice = in.readString();
        sn = in.readString();
        macDevice = in.readString();
        mac = in.readString();
        model = in.readString();
        displayName = in.readString();
        thumbnail = in.readString();
        cid = in.readString();
        nickName = in.readString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSnDevice() {
        return snDevice;
    }

    public void setSnDevice(String snDevice) {
        this.snDevice = snDevice;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getMacDevice() {
        return macDevice;
    }

    public void setMacDevice(String macDevice) {
        this.macDevice = macDevice;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
