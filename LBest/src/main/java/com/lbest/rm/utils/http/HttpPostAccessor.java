package com.lbest.rm.utils.http;


import java.util.Objects;

/**
 * Created by dell on 2017/10/18.
 */

public class HttpPostAccessor extends HttpAccessor{
    public HttpPostAccessor() {
        super(METHOD_POST);
    }

    @Override
    public String execute(String url, Object param) {
        return super.execute(url, param);
    }

    @Override
    public String execute(String url, Object headParam, Object param) {
        return super.execute(url, headParam, param);
    }
}
