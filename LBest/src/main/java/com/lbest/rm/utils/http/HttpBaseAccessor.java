package com.lbest.rm.utils.http;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import java.security.KeyStore;
import java.util.Properties;

/**
 * Created by dell on 2017/10/18.
 */

public class HttpBaseAccessor {
    public static final int METHOD_POST = 1;
    public static final int METHOD_GET = 2;
    public static final int METHOD_POST_MULTIPART = 3;
    /**
     * 请求
     */
    protected HttpRequestBase mHttpRequest;
    protected boolean mStoped = false;
    private static HttpClient mHttpClient;
    protected int mMethod;

    public HttpBaseAccessor(int method) {
        mMethod = method;
    }

    protected synchronized HttpClient getHttpClient() {
        if (null == mHttpClient) {
            try {
                final Properties properties = new Properties();

                HttpParams params = new BasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
                HttpProtocolParams.setUseExpectContinue(params, true);

                ConnManagerParams.setTimeout(params,
                        Integer.parseInt(properties.getProperty("poolTimeout", "1000")));
                ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRoute() {

                    @Override
                    public int getMaxForRoute(HttpRoute route) {
                        return Integer.parseInt(properties.getProperty("maxConnectionsPerRoute", "64"));
                    }
                });
                ConnManagerParams.setMaxTotalConnections(params,
                        Integer.parseInt(properties.getProperty("maxTotalConnections", "128")));
                HttpConnectionParams.setConnectionTimeout(params,
                        Integer.parseInt(properties.getProperty("connectTimeout", "15000")));
                HttpConnectionParams.setSoTimeout(params,
                        Integer.parseInt(properties.getProperty("soTimeout", "15000")));

                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                SchemeRegistry schReg = new SchemeRegistry();
                schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                schReg.register(new Scheme("https", sf, 443));

                ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
                mHttpClient = new DefaultHttpClient(conMgr, params);
            } catch (Exception e) {
                mHttpClient = new DefaultHttpClient();
            }
        }

        return mHttpClient;
    }

}
