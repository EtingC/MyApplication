package com.lbest.rm.utils.http;

/**
 * Created by dell on 2017/10/19.
 */

public class HttpGetAccessor extends HttpAccessor {

    public HttpGetAccessor() {
        super(METHOD_GET);
    }

    public String execute(String url, Object param) {
        return super.execute(url, param);
    }


    public <T> T execute(String url, Object param, Class<T> returnType) {
        return execute(url, null, param, returnType);
    }

    @Override
    public <T> T execute(String url, Object headParam, Object param, Class<T> returnType) {
        T result = super.execute(url, headParam, param, returnType);
        return result;
    }
}
