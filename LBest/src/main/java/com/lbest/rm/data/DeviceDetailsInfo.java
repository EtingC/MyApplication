package com.lbest.rm.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 2017/11/10.
 */

public class DeviceDetailsInfo extends BaseDeviceInfo{
    private String auid;
    private List<relAccount> relAccounts;

    public DeviceDetailsInfo(){
        super();
    }

    public DeviceDetailsInfo(Parcel in){
        super(in);
    }

    public static final Parcelable.Creator<DeviceDetailsInfo> CREATOR = new Parcelable.Creator<DeviceDetailsInfo>()
    {
        public DeviceDetailsInfo createFromParcel(Parcel in)
        {
            return new DeviceDetailsInfo(in);
        }

        public DeviceDetailsInfo[] newArray(int size)
        {
            return new DeviceDetailsInfo[size];
        }
    };

    public void readFromParcel(Parcel in) {
        super.readFromParcel(in);

        if(relAccounts ==null){
            relAccounts = new ArrayList<relAccount>();
        }
        in.readTypedList(relAccounts, relAccount.CREATOR);
        auid=in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeTypedList(relAccounts);
        out.writeString(auid);
    }

    public List<relAccount> getRelAccounts() {
        return relAccounts;
    }

    public String getAuid() {
        return auid;
    }

    public void setAuid(String auid) {
        this.auid = auid;
    }

    public void setRelAccounts(List<relAccount> relAccounts) {
        this.relAccounts = relAccounts;
    }
}
