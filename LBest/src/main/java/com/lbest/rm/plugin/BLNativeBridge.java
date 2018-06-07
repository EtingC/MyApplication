package com.lbest.rm.plugin;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.DNAH5Activity;
import com.lbest.rm.DevicePropertyActivity;
import com.lbest.rm.MyApplication;
import com.lbest.rm.R;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLConstants;
import com.lbest.rm.common.BLFileUtils;
import com.lbest.rm.common.BLProfileTools;
import com.lbest.rm.common.StorageUtils;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.DeviceManager;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.utils.Logutils;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import cn.com.broadlink.sdk.BLLet;

public class BLNativeBridge extends CordovaPlugin implements BLPluginInterfacer{

	private CallbackContext mNotificationCallbackContext;
	
    @Override
    public boolean execute(String action, JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "action:" + action);
    	//判断Action是否为空，为空不处理
    	if(!TextUtils.isEmpty(action)){
			switch (action){
				case DEVICEINO:
					return deviceInfo(callbackContext);
				case NOTIFICATION:
					return saveNotification(jsonArray, callbackContext);
				case DNA_CONTROL:
					return deviceControl(jsonArray, callbackContext);
				case DEVICE_AUTH:
					return toDevAuthActivity(jsonArray, callbackContext);
				case CHECK_DEVICE_AUTH:
					return queryAuth(jsonArray, callbackContext);
				case GET_USERINFO:
					return getUserInfo(callbackContext);
				case GET_FAMILYINFO:
					return getFamilyInfo(callbackContext);
				case GET_GETFAMILY_SCENELIST:
					return getSceneList(callbackContext);
				case GET_DEVICE_LIST:
					return getDeviceList(callbackContext);
				case GET_DEVICEROOM:
					return getDevRoom(jsonArray, callbackContext);
				case GET_DEV_PROFILE:
					return queryDevProfile(jsonArray, callbackContext);
				case HTTP_REQUERT:
					return httpRequerst(jsonArray, callbackContext);
				case DEVICE_STATUS_QUERY:
					return queryDevStatus(jsonArray, callbackContext);
				case GET_WIFI_INFO:
					return getWifiInfo(callbackContext);
				case GET_GETWAY_SUBDEVLIST:
					return getSubDeviceList(jsonArray, callbackContext);
				case OPEN_DEV_CRTL_PAGE:
					return openControlPage(jsonArray, callbackContext);
				case CLOSE_WEBVIEW:
					return closeWebViewActivity();
				case GPS_LOCATION:
					return gpsLocation(callbackContext);
				case GET_PRESET_DATA:
					return readPresetData(callbackContext);
				case ACCOUNT_SEND_VCODE:
					return accountSendVCode(jsonArray, callbackContext);
				case OPEN_URL:
					return openUrl(jsonArray, callbackContext);
				case OPEN_DEV_PROPERTY_PAGE:
					 openPropertyPage(jsonArray, callbackContext);
					break;
				case GET_STATUSBAR_HEIGHT:
					getStatusBarHeight(callbackContext);
					break;
                case  GET_TOOLBARBAR_HEIGHT:
                    getToolBarHeight(callbackContext);
                    break;
				case DELETE_FAMILY_DEVICE_LIST:
					deleteFamilyDeviceList(jsonArray, callbackContext);
				default:
			}
    	}

		return false;
    }

	public void getStatusBarHeight(CallbackContext callbackContext) {
		final DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
		int result = 0;
		int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = activity.getResources().getDimensionPixelSize(resourceId);
		}
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("statusbar_height", result);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		callbackContext.success(jsonObject.toString());
	}

    public void getToolBarHeight(CallbackContext callbackContext) {
        final DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
        float result = 0;
        result = activity.getResources().getDimension(R.dimen.toolbar_height);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("toolbar_height", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callbackContext.success(jsonObject.toString());
    }

	@Override
	public Boolean shouldAllowNavigation(String url) {
		return true;
	}

	@Override
	public Boolean shouldAllowRequest(String url) {
		return true;
	}


	//获取WIFI信息
	public boolean getWifiInfo(CallbackContext callbackContext){
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("ssid", CommonUtils.getWIFISSID(cordova.getActivity()));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		callbackContext.success(jsonObject.toString());
		return true;
	}

	//获取用户信息
	public boolean getUserInfo(CallbackContext callbackContext){
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("userId", BLAcountToAli.getInstance().getBlUserInfo().getBl_userid());
			jsonObject.put("nickName", BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());
			jsonObject.put("userName", BLAcountToAli.getInstance().getBlUserInfo().getBl_userid());
			jsonObject.put("userIcon", BLAcountToAli.getInstance().getBlUserInfo().getBl_icon());
			jsonObject.put("loginSession", BLAcountToAli.getInstance().getBlUserInfo().getBl_loginsession());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		callbackContext.success(jsonObject.toString());
		return true;
	}

	//获取家庭信息
	public boolean getFamilyInfo(CallbackContext callbackContext){
//		JSONObject jsonObject = new JSONObject();
//		try {
//			jsonObject.put("familyId", HomePageActivity.mBlFamilyInfo.getFamilyId());
//			jsonObject.put("familyName", HomePageActivity.mBlFamilyInfo.getName());
//			jsonObject.put("familyIcon", HomePageActivity.mBlFamilyInfo.getIconPath());
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//		callbackContext.success(jsonObject.toString());
		return true;
	}

	public boolean gpsLocation(CallbackContext callbackContext){
//		JSONObject jsonObject = new JSONObject();
//		try {
//			jsonObject.put("city", EControlApplication.mGPSCity);
//			jsonObject.put("address", EControlApplication.mGPSAddress);
//			jsonObject.put("longitude", EControlApplication.mLongitude);
//			jsonObject.put("latitude", EControlApplication.mLatitude);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//		callbackContext.success(jsonObject.toString());
		return true;
	}

	public boolean openControlPage(JSONArray jsonArray, CallbackContext callbackContext){
		if(jsonArray.length() > 0){
			final DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();

			String devDid = null, param = null;
			try {
				String didJSONStr = jsonArray.getString(0);
				JSONObject jsonObject = new JSONObject(didJSONStr);
				String did = jsonObject.optString("did");
				String sdid = jsonObject.optString("sdid");
				param = jsonObject.optString("extend");
				String data = jsonObject.optString("data");

				//如果data数据不为空 将数据保存文件，供下次H5调用,数据大小约1M
				writeH5CacheFileData(data);

				devDid = TextUtils.isEmpty(sdid) ? did : sdid;
			} catch (JSONException e) {
				e.printStackTrace();
			}


			FamilyDeviceModuleData familyDeviceModuleData = null;
			if(!TextUtils.isEmpty(devDid)){
				familyDeviceModuleData=DeviceManager.getInstance().getDevice(devDid);
			}

			if(familyDeviceModuleData != null){
				Intent intent = new Intent();
				intent.setClass(activity, DNAH5Activity.class);
				intent.putExtra(BLConstants.INTENT_DEVICE, familyDeviceModuleData);
				intent.putExtra(BLConstants.INTENT_PARAM, param);
				activity.startActivity(intent);
			}else{
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(activity,activity.getResources().getString(R.string.str_main_free_device),Toast.LENGTH_LONG).show();
					}
				});
			}
		}
		return true;
	}

	private boolean readPresetData(CallbackContext callbackContext){
		String data = readH5CacheFileData();
		JSONObject jsonObject = new JSONObject();
		try {
			 if(data != null) jsonObject.put("data", data);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		callbackContext.success(jsonObject.toString());
		return true;
	}

	//获取设备列表
	public boolean getDeviceList(CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		List<BLDeviceInfo> devList = ((EControlApplication) activity.getApplicationContext()).mBLDeviceManager.getDevList();
//		JSONObject jsonObject = new JSONObject();
//
//		JSONArray jsonArray = new JSONArray();
//		for(BLDeviceInfo deviceInfo : devList){
//			String did = TextUtils.isEmpty(deviceInfo.getPdid()) ?  deviceInfo.getDid() : deviceInfo.getPdid();
//			String sdid = TextUtils.isEmpty(deviceInfo.getPdid()) ?  null : deviceInfo.getDid();
//			BLModuleInfo moduleInfo = getDidModuleInfo(did, sdid);
//			JSONObject devObject = new JSONObject();
//			devObject.put("did", deviceInfo.getDid());
//			devObject.put("pdid", deviceInfo.getPdid());
//			devObject.put("mac", deviceInfo.getMac());
//			devObject.put("pid", deviceInfo.getPid());
//			devObject.put("name",moduleInfo == null ? deviceInfo.getName() : moduleInfo.getName());
//			devObject.put("lock", deviceInfo.isLock());
//			devObject.put("password", deviceInfo.getPassword());
//			jsonArray.put(devObject);
//		}
//
//		jsonObject.put("deviceList", jsonArray);
//		callbackContext.success(jsonObject.toString());
		return true;
	}

	//获取子设备列表
	public boolean getSubDeviceList(JSONArray paramJsonArray, CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		BLDeviceManager deviceManager = ((EControlApplication) activity.getApplicationContext()).mBLDeviceManager;
//		try {
//			BLModuleInfoDao dao = new BLModuleInfoDao(activity.getHelper());
//			FamilyDeviceRelationDao familyDeviceRelationDao = new FamilyDeviceRelationDao(activity.getHelper());
//
//			String getWayDid = activity.mBlDeviceInfo.getDid();
//			if(paramJsonArray != null && paramJsonArray.length() >= 1){
//				String didJSONStr = paramJsonArray.getString(0);
//				JSONObject jsonObject = new JSONObject(didJSONStr);
//				String did = jsonObject.optString("did");
//				if(!TextUtils.isEmpty(did)){
//					getWayDid = did;
//				}
//			}
//
//			List<BLModuleInfo> devList = dao.queryGetwaySubDevModuleList(HomePageActivity.mBlFamilyInfo.getFamilyId(), getWayDid);
//			JSONObject jsonObject = new JSONObject();
//			JSONArray jsonArray = new JSONArray();
//
//			for(BLModuleInfo moduleInfo : devList){
//				BLDeviceInfo deviceInfo = deviceManager.queryDeivceFromCache(moduleInfo.getSubDevId());
//				JSONObject devObject = new JSONObject();
//				devObject.put("did", moduleInfo.getSubDevId());
//				devObject.put("icon", moduleInfo.getIconPath());
//				if(deviceInfo != null) devObject.put("pid", deviceInfo.getPid());
//				devObject.put("name", moduleInfo.getName());
//
//				BLRoomInfo roomInfo = familyDeviceRelationDao.queryDeviceRoom(moduleInfo.getSubDevId());
//				if(roomInfo != null){
//					devObject.put("roomId", roomInfo.getRoomId());
//					devObject.put("roomName", roomInfo.getName());
//				}
//
//				jsonArray.put(devObject);
//			}
//
//			jsonObject.put("deviceList", jsonArray);
//
//			callbackContext.success(jsonObject.toString());
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		return true;
	}

	//获取场景列表
	private boolean getSceneList(CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		JSONObject jsonObject = new JSONObject();
//
//		try {
//			BLModuleInfoDao dao = new BLModuleInfoDao(activity.getHelper());
//			List<BLModuleInfo> sceneList = dao.queryFamilyAllModuleList(
//					HomePageActivity.mBlFamilyInfo.getFamilyId(), BLModuleType.CUSTOM_SCENE);
//			JSONArray jsonArray = new JSONArray();
//			for(BLModuleInfo scene : sceneList){
//				JSONObject devObject = new JSONObject();
//				devObject.put("id", scene.getModuleId());
//				devObject.put("name", scene.getName());
//				devObject.put("icon", scene.getIconPath());
//				jsonArray.put(devObject);
//			}
//
//			jsonObject.put("scenes", jsonArray);
//
//			callbackContext.success(jsonObject.toString());
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		callbackContext.success(jsonObject.toString());
		return true;
	}

	//获取设备所在的房间
	private boolean getDevRoom(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		if(jsonArray.length() > 0){
//			String did = null;
//			String didJSONStr = jsonArray.getString(0);
//			JSONObject jsonObject = new JSONObject(didJSONStr);
//			did = jsonObject.optString("did");
//
//			if(did != null){
//				try {
//					FamilyDeviceRelationDao dao = new FamilyDeviceRelationDao(activity.getHelper());
//					BLRoomInfo roomInfo = dao.queryDeviceRoom(did);
//					if(roomInfo != null){
//						JSONObject resultJsonObject = new JSONObject();
//						resultJsonObject.put("id", roomInfo.getRoomId());
//						resultJsonObject.put("name", roomInfo.getName());
//						callbackContext.success(resultJsonObject.toString());
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
		return true;
	}

	//获取设备的profile
	private boolean queryDevProfile(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
		if(jsonArray.length() > 0){
			String pid = null;
			String didJSONStr = jsonArray.getString(0);
			JSONObject jsonObject = new JSONObject(didJSONStr);
			pid = jsonObject.optString("pid");

			if(pid != null){
				String profile = BLProfileTools.queryProfileStrByPid(pid);
				JSONObject resultJsonObject = new JSONObject();
				JSONObject profileJson = new JSONObject(profile);
				resultJsonObject.put("profile", profileJson);
				callbackContext.success(resultJsonObject.toString());
			}
		}
		return true;
	}

	//设备认证
	private boolean toDevAuthActivity(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		if(jsonArray.length() > 0){
//			activity.toDevAuthActivity(jsonArray.getString(0), callbackContext);
//		}
		return true;
	}

	//check设备授权
	private boolean queryAuth(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//		if(jsonArray.length() > 0){
//			JSONObject jsonObject = new JSONObject(jsonArray.getString(0));
//
//			String ticket = jsonObject.optString("ticket");
//			new QueryAuthDevListTask(activity, ticket , callbackContext).executeOnExecutor(EControlApplication.FULL_TASK_EXECUTOR);
//		}
		return true;
	}

    /**保存当前推送JS的回调**/
    private boolean saveNotification(JSONArray jsonArray, CallbackContext callbackContext){
    	mNotificationCallbackContext = callbackContext;
    	return true;
    }

	private boolean accountSendVCode(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		if(jsonArray.length() > 0){
//			JSONObject jsonObject = new JSONObject(jsonArray.getString(0));
//			String account = jsonObject.optString("account");
//			String countrycode = jsonObject.optString("countrycode");
//			if(account != null){
//				new AccountSendVCodeTask(callbackContext).executeOnExecutor(EControlApplication.FULL_TASK_EXECUTOR, account, countrycode);
//			}else{
//				return false;
//			}
//		}
		return true;
	}

	private boolean openUrl(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
		if(jsonArray.length() > 0){
			DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();

			JSONObject jsonObject = new JSONObject(jsonArray.getString(0));
			String openUrl = jsonObject.optString("url");
			String platform = jsonObject.optString("platform");

			if(openUrl != null){
				if(!openUrl.startsWith("http")){
					openUrl = StorageUtils.languageFolder(activity.mBlDeviceInfo.getPid())  + File.separator + openUrl;
				}

				if(platform == null || platform.equals("app")){
					Intent intent = new Intent();
					intent.setClass(activity, DNAH5Activity.class);
					intent.putExtra(BLConstants.INTENT_DEVICE, activity.mBlDeviceInfo);
					intent.putExtra(BLConstants.INTENT_URL, openUrl);
					activity.startActivity(intent);
				}else{
					Intent intent = new Intent();
					intent.setAction("android.intent.action.VIEW");
					Uri content_url = Uri.parse(openUrl);
					intent.setData(content_url);
					activity.startActivity(intent);
				}

				callbackContext.success();
			}else{
				return false;
			}
		}
		return true;
	}

	private boolean deleteFamilyDeviceList(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		if(jsonArray.length() > 0){
//			DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//			JSONObject jsonObject = new JSONObject(jsonArray.getString(0));
//			JSONArray didsArray = jsonObject.optJSONArray("dids");
//			JSONArray sdidsArray = jsonObject.optJSONArray("sdids");
//
//			List<String> sdidsList = null; List<String> didsList = null;
//			if(didsArray != null){
//				didsList = JSON.parseArray(didsArray.toString(), String.class);
//			}
//			if(sdidsArray != null){
//				sdidsList = JSON.parseArray(sdidsArray.toString(), String.class);
//			}
//
//			new DeleteFamilyDeviceListTask(activity, didsList, sdidsList, callbackContext).execute();
//		}
		return true;
	}

    /**
     * 获取设备信息
     * 
     * @param callbackContext
     * 			回调
     * @return
     */
    private boolean deviceInfo(CallbackContext callbackContext){
    	DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
    	if(activity != null && activity.mBlDeviceInfo != null){
			FamilyDeviceModuleData deviceInfo = activity.mBlDeviceInfo;
			String did = deviceInfo.getDid();
			String sdid = deviceInfo.getsDid();
    		BLJSDeviceInfo startUpInfo = new BLJSDeviceInfo();
    		startUpInfo.setDeviceStatus(BLLet.Controller.queryDeviceState(did));
			startUpInfo.setDeviceID(did);
			startUpInfo.setSubDeviceID(sdid);
			startUpInfo.setDeviceName(deviceInfo.getName());
		    startUpInfo.setDeviceMac(deviceInfo.getMac());
			startUpInfo.setKey(deviceInfo.getAeskey());
    		startUpInfo.getNetworkStatus().setStatus(CommonUtils.checkNetwork(activity)
					? BLPluginInterfacer.NETWORK_AVAILAVLE : BLPluginInterfacer.NETWORK_UNAVAILAVLE);
    		startUpInfo.getUser().setName(BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());
    		callbackContext.success(JSON.toJSONString(startUpInfo));
    	}

		return true;
    }

	public void openPropertyPage(JSONArray jsonArray, CallbackContext callbackContext){
		Logutils.log_d("openPropertyPage:"+JSON.toJSONString(jsonArray));
		try {
			JSONObject jobj = jsonArray.getJSONObject(0);
			if(jobj!=null){
				String did=jobj.getString("did");
				if(!TextUtils.isEmpty(did)){
					DeviceManager mDeviceManager=DeviceManager.getInstance();
					FamilyDeviceModuleData mBlDeviceInfo=mDeviceManager.getDevice(did);
					if(mBlDeviceInfo!=null){
						Intent intent = new Intent();
						intent.putExtra(BLConstants.INTENT_DEVICE, mBlDeviceInfo);
						intent.setClass(cordova.getActivity(), DevicePropertyActivity.class);
						cordova.getActivity().startActivityForResult(intent, 5);
					}else{
						Toast.makeText(cordova.getActivity(),cordova.getActivity().getResources().getString(R.string.deviceproperty_nodevice),Toast.LENGTH_LONG).show();
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
    /**
     * 设备控制
     * @param jsonArray
     * @param callbackContext
     * @return
     */
    private boolean deviceControl(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
		String deviceMac = jsonArray.getString(0);
		String subDeviceID = jsonArray.getString(1);
		String cmd = jsonArray.getString(2);
		String method = jsonArray.getString(3);

		String extendStr = null;
		if(jsonArray.length() >= 5){
			extendStr = jsonArray.getString(4);
		}

		//判断mac地址是否为空，以及method是否为空
		if(TextUtils.isEmpty(deviceMac) || TextUtils.isEmpty(method)){
			BLJsBaseResult pluginBaseResult = new BLJsBaseResult();
			pluginBaseResult.setCode(ERRCODE_PARAM);
			callbackContext.error(JSON.toJSONString(pluginBaseResult));
		}else{
			Logutils.log_d("device control:"+cmd);
			new ControlTask(cordova.getActivity(), extendStr, callbackContext).executeOnExecutor(MyApplication.FULL_TASK_EXECUTOR,
					deviceMac, subDeviceID, cmd, method);
//			new QueryTimerTask(cordova.getActivity(), extendStr, callbackContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deviceMac, subDeviceID);
		}
    	return true;
    }

    /**
     * 推送消息给JS
     * 
     * @param param
     *			JsonStr
     */
    public void pushJSNotification(String param){
    	if(mNotificationCallbackContext != null){
    		CallbackContext ctx = mNotificationCallbackContext;
    		mNotificationCallbackContext = null;
    		ctx.success(param);
    	}
    }

	private boolean httpRequerst(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, jsonArray.toString());
		String cmdJsonStr = jsonArray.getString(0);
		new HttpRequestTask(cordova.getActivity(), callbackContext).execute(cmdJsonStr);
		return true;
	}

	private boolean queryDevStatus(JSONArray jsonArray, CallbackContext callbackContext) throws JSONException {
//		String cmdJsonStr = jsonArray.getString(0);
//		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
//
//		new QueryDeviceStatusTask(cordova.getActivity(), activity.mBlDeviceInfo, callbackContext).execute(cmdJsonStr);
		return true;
	}

	@Override
	public Boolean shouldAllowBridgeAccess(String url) {
		return true;
	}

	private boolean closeWebViewActivity(){
		clearH5CacheFileData();
		DNAH5Activity activity = (DNAH5Activity) cordova.getActivity();
		activity.finish();
		return true;
	}

	private void clearH5CacheFileData(){
		String path = StorageUtils.CACHE_FILE_PATH + File.separator + "webwiewCache.data";
		File file = new File(path);
		if(new File(path).exists()){
			file.delete();
		}
	}

	private void writeH5CacheFileData(String data){
		if(!TextUtils.isEmpty(data)){
			String path = StorageUtils.CACHE_FILE_PATH + File.separator + "webwiewCache.data";
			BLFileUtils.saveStringToFile(data, path);
		}
	}

	private String readH5CacheFileData(){
		String path = StorageUtils.CACHE_FILE_PATH + File.separator + "webwiewCache.data";
		return BLFileUtils.readTextFileContent(path);
	}
}
