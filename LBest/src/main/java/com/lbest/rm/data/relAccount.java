package com.lbest.rm.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by dell on 2017/11/10.
 */

public class relAccount implements Parcelable {
    private String auid;
    private String managerFlag;
    private String name;
    private String uid;

    public relAccount(){}

    public int describeContents()
    {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(auid);
        out.writeString(managerFlag);
        out.writeString(name);
        out.writeString(uid);
    }

    public static final Parcelable.Creator<relAccount> CREATOR = new Parcelable.Creator<relAccount>()
    {
        public relAccount createFromParcel(Parcel in)
        {
            return new relAccount(in);
        }

        public relAccount[] newArray(int size)
        {
            return new relAccount[size];
        }
    };

    public relAccount(Parcel in)
    {
        readFromParcel(in);
    }


    public void readFromParcel(Parcel in) {
        auid = in.readString();
        managerFlag = in.readString();
        name = in.readString();
        uid = in.readString();
    }

    public String getAuid() {
        return auid;
    }

    public void setAuid(String auid) {
        this.auid = auid;
    }

    public String getManagerFlag() {
        return managerFlag;
    }

    public void setManagerFlag(String managerFlag) {
        this.managerFlag = managerFlag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
