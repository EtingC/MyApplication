package com.lbest.rm.utils.downloadfile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.lbest.rm.utils.http.DataParseUtils;

/**
 * 从服务端读取JSON内容
 */
public class JSONAccessor extends HttpAccessor {

	private static final String TAG = JSONAccessor.class.getName();

	private static final int LOAD_BUFF_SIZE = 8192;

	private boolean mEnableJsonLog;

	private Gson mGson;

	private boolean mReturnString = false;

	/**
	 * 构造函数
	 */
	public JSONAccessor(Context context, int method) {
		super(context, method);
		initGson();
	}

	/**
	 * 连接服务端开始通信
	 * 
	 * @param url
	 *            请求URL
	 * @param param
	 *            参数
	 * @param returnType
	 *            返回类型
	 * 
	 * @return 数据结果
	 */
	public <T> T execute(String url, Object param, Class<T> returnType) {
		try {
			return access(url, param, returnType);
		} catch (Exception e) {
			onException(e);
		}
		return null;
	}

	/**
	 * 连接服务端开始通信
	 * 
	 * @param url
	 *            请求URL
	 * @param param
	 *            参数
	 * @param returnType
	 *            返回类型
	 * 
	 * @return 数据结果
	 */
	protected <T> T access(String url, Object param, Class<T> returnType) throws Exception {
		BufferedInputStream bis = null;
		ByteArrayOutputStream baos = null;
		try {
			if (mMethod == METHOD_POST) {
				mHttpRequest = new HttpPost();
			} else {
				mHttpRequest = new HttpGet();
			}

			if (param != null) {
				List<Field> fields = DataParseUtils.getFields(param.getClass(), Object.class);

				switch (mMethod) {
				case METHOD_POST:
					List<NameValuePair> params = new ArrayList<NameValuePair>();

					for (Field field : fields) {
						field.setAccessible(true);
						if (field.get(param) != null) {
							params.add(new BasicNameValuePair(field.getName(), String.valueOf(field.get(param))));
						}
					}

					UrlEncodedFormEntity formEntiry = new UrlEncodedFormEntity(params, HTTP.UTF_8);

					((HttpPost) mHttpRequest).setEntity(formEntiry);

					break;

				case METHOD_POST_MULTIPART:
					MultipartEntity multipartEntity = new MultipartEntity();

					for (Field field : fields) {
						field.setAccessible(true);
						if (field.get(param) != null) {
							if (field.getType().equals(File.class)) {
								multipartEntity.addPart(field.getName(), new FileBody((File) field.get(param)));
							} else {
								multipartEntity.addPart(field.getName(), new StringBody(String.valueOf(field.get(param)), Charset.forName(HTTP.UTF_8)));
							}
						}
					}

					((HttpPost) mHttpRequest).setEntity(multipartEntity);

					break;

				case METHOD_GET:
					StringBuilder sbUrl = new StringBuilder();

					for (Field field : fields) {
						field.setAccessible(true);
						if (field.get(param) != null) {
							sbUrl.append('&');
							sbUrl.append(field.getName());
							sbUrl.append('=');
							sbUrl.append(String.valueOf(field.get(param)));
						}
					}

					if (sbUrl.length() > 0) {
						sbUrl.replace(0, 1, "?");
						url += sbUrl.toString();
					}

					break;

				case METHOD_GET_ADD:
					StringBuilder sbUrl2 = new StringBuilder();

					for (Field field : fields) {
						field.setAccessible(true);
						if (field.get(param) != null) {
							sbUrl2.append('&');
							sbUrl2.append(field.getName());
							sbUrl2.append('=');
							sbUrl2.append(String.valueOf(field.get(param)));
						}
					}
					url += sbUrl2.toString();
					break;
					default:
				}
			}

			mHttpRequest.setURI(new URI(url));

			HttpClient httpClient = getHttpClient();
			HttpResponse response = httpClient.execute(mHttpRequest);

			if (mStoped)
				return null;

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				bis = new BufferedInputStream(response.getEntity().getContent());
				baos = new ByteArrayOutputStream();

				int size;
				byte[] temp = new byte[LOAD_BUFF_SIZE];
				while ((size = bis.read(temp, 0, temp.length)) != -1 && !mStoped) {
					baos.write(temp, 0, size);
				}

				if (mStoped)
					return null;

				String json = baos.toString();

				if (mEnableJsonLog)
					Log.d(TAG, json);

				if (json != null && json.length() > 0) {
					if (mReturnString)
						return (T) json;

					T result = null;

					if (returnType != null)
						result = mGson.fromJson(json, returnType);

					if (mStoped)
						return null;
					else
						return result;
				}

			} else {
				throw new SocketException("Status Code : " + response.getStatusLine().getStatusCode());
			}

		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				bis = null;
			}
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				baos = null;
			}

			mHttpRequest.abort();
		}
		
		return null;
	}

	protected void onException(Exception e) {
		Log.e(TAG, e.getMessage(), e);
	}

	protected void initGson() {
		mGson = new Gson();
	}

	public void enableJsonLog(boolean enable) {
		mEnableJsonLog = enable;
	}

	public void setReturnString(boolean enable) {
		mReturnString = enable;
	}
}