package com.lbest.rm.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by dell on 2017/7/27.
 */

public class productInfo implements Parcelable{
    private String id;//产品id
    private String model;//产品model
    private String deviceName;//产品名称
    private String icon;//产品图片

    private String price;
    private String description;
    private String detailUrl;
    private String buyUrl;
    private String displayName;
    private String brandName;
    private String shortModel;
    private String netType;
    private String initAction;


    public productInfo(){}

    public int describeContents()
    {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(id);
        out.writeString(model);
        out.writeString(deviceName);
        out.writeString(icon);
        out.writeString(price);
        out.writeString(description);
        out.writeString(detailUrl);
        out.writeString(buyUrl);
        out.writeString(displayName);
        out.writeString(brandName);
        out.writeString(netType);
        out.writeString(initAction);
        out.writeString(shortModel);
    }

    public static final Parcelable.Creator<productInfo> CREATOR = new Parcelable.Creator<productInfo>()
    {
        public productInfo createFromParcel(Parcel in)
        {
            return new productInfo(in);
        }

        public productInfo[] newArray(int size)
        {
            return new productInfo[size];
        }
    };

    private productInfo(Parcel in)
    {
        id = in.readString();
        model = in.readString();
        deviceName = in.readString();
        icon = in.readString();
        price = in.readString();
        description = in.readString();
        detailUrl = in.readString();
        buyUrl = in.readString();
        displayName = in.readString();
        brandName = in.readString();
        netType = in.readString();
        initAction = in.readString();
        shortModel = in.readString();
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public String getBuyUrl() {
        return buyUrl;
    }

    public void setBuyUrl(String buyUrl) {
        this.buyUrl = buyUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getNetType() {
        return netType;
    }

    public void setNetType(String netType) {
        this.netType = netType;
    }

    public String getInitAction() {
        return initAction;
    }

    public void setInitAction(String initAction) {
        this.initAction = initAction;
    }

    public String getShortModel() {
        return shortModel;
    }

    public void setShortModel(String shortModel) {
        this.shortModel = shortModel;
    }
}
