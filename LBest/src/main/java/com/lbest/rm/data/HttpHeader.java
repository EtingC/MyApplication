package com.lbest.rm.data;

import com.lbest.rm.utils.EncryptUtils;

import java.io.File;

/**
 * Created by dell on 2017/11/30.
 */

public class HttpHeader extends UserHeadParam {
    private RequestTimestampResult timestampResult;
    private String FileMd5;
    private String ContentSHA1;

    public void setEncrypt(File file){
        FileMd5 = EncryptUtils.fileMD5(file);
        ContentSHA1 = EncryptUtils.fileSHA1(file);
    }

    public String getFileMd5() {
        return FileMd5;
    }

    public void setFileMd5(String fileMd5) {
        FileMd5 = fileMd5;
    }

    public String getContentSHA1() {
        return ContentSHA1;
    }

    public void setContentSHA1(String contentSHA1) {
        ContentSHA1 = contentSHA1;
    }

    public RequestTimestampResult getTimestampResult() {
        return timestampResult;
    }

    public void setTimestampResult(RequestTimestampResult timestampResult) {
        this.timestampResult = timestampResult;
    }
}
