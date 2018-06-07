/**
 * Fake Adapter, for debug
 *
 */
var ADPT = (function() {
	var uSDK = null;
	var deviceInfo = null;
	var values = {};
	var deviceInfo = {};
	var UPDATE_STATUS_INTERVAL = 5000; // in ms
	/*
	var user = {
		'user': { 'name': 'dehou' }
	};
	*/

	// when the document is ready, load cordova dynamically
	function onReady() {
		if (window.DA.uuid === undefined) {
			window.setTimeout(onReady, 1000);
			return;
		}

		try {
			uSDK.em.fire('evtKickoff', {});
			startUpdateInfo(true);
			startUpdateStatus(true);
		} catch (e) {
//			uSDK.debug.trace();
			uSDK.debug.log(e);
		}
	}

	function startUpdateInfo(immediately) {
		if (immediately) {
			var changed = {};
			var changedCount = 0;
			var map = {
				'uuid': 'deviceID',
				'networkIsAvailable': 'networkStatus'
			};
			for (var ak in map) {
				var bk = map[ak];
				var av = DA[ak];
				if (bk == 'networkStatus') {
					av = av ? 'available' : 'unavailable';
				}
				if (deviceInfo[bk] !== av) {
					changed[bk] = {
						oldValue: deviceInfo[bk],
						value: av 
					};
					++ changedCount;
					deviceInfo[bk] = av;
				}
			}
			if (changedCount > 0) {
				uSDK.em.fire('evtDeviceInfoChanged', {changed: changed});
			}

//			startUpdateInfo();
		} else {
			window.setTimeout(function() {
				startUpdateInfo(true);
			}, UPDATE_INFO_INTERVAL);
		}
	}

	var registeredItems = {};
	var registeredID = 0;
	function registerResponse(item) {
		var id = registeredID;
		registeredItems[id] = item;

		++ registeredID;

		window.setTimeout(function() {
			if (registeredItems[id] !== undefined) {
				try {
					registeredItems[id].reject(STRINGS.error.cmd + ' (timeout)');
					delete registeredItems[id];
				} catch (e) {
					uSDK.debug.log(e);
				}
			}
		}, 10 * 1000);
	}

	function onStatusChange(data) {
		parseDeviceStatus(data);
		for (var id in registeredItems) {
			var item = registeredItems[id];
			var cmd = item.cmd;
			for (var k in data) {
				if (cmd[k] !== undefined) {
					delete cmd[k];
				}
			}
			if (Object.keys(cmd).length === 0) {
				item.resolve(data);
				delete registeredItems[id];
			}
		}
	}

	function startUpdateStatus(immediately) {
		if (immediately) {
			getDeviceStatus(deviceInfo.deviceID, null, []).then(function(data) {
				DA.bindPushData({
					'deviceStatusChange': onStatusChange,
					'netWorkStatusChange': function() {
						// TODO:
					}
				});
			}).catch(function(e) {
				startUpdateStatus();
			});
		} else {
			uSDK.utils.later(UPDATE_STATUS_INTERVAL).then(function() {
				startUpdateStatus(true);
			});
		}
	}

	function translateResponse(data) {
		var aks = Object.keys(data);
		var params = [];
		var values = [];
		// uSDK.debug.log(JSON.stringify(data));
		for (var i = 0; i < aks.length; ++ i) {
			var ak = aks[i];
			var av = data[ak].value;
			if (ak == 'onlineState') {
				var online = av == 'on' ? true : false;
				if (deviceInfo['deviceStatus'] !== online) {
					uSDK.em.fire('evtDeviceInfoChanged', {
						changed: {
							'deviceStatus': {
								value: online,
								oldValue: deviceInfo['deviceStatus']
							}
						}
					});
					deviceInfo['deviceStatus'] = online;
				}
				continue;
			}
			//这里会提示uuid不存在
			var bk = CMDMAP.a2b[ak];
			if (bk === undefined) {
				//uSDK.debug.log(ak + ' doesnot exist in map');
				bk = ak;
			}
			var intf = uSDK.getIntf(bk);
			if (intf === undefined) {
				//uSDK.debug.log(bk + " doesn't exist in profile.");
				continue;
			}

			params.push(bk);
			values.push(av);
		}


		return {
			params: params,
			vals: values
		};
	}

	function translateCommand(cmd) {
		var ps = cmd.params;
		var vs = cmd.vals;
		if (ps.length !== vs.length) {
			uSDK.debug.log('params and vals have different size: ' + ps.length + ' vs ' + vs.length);
			return null;
		}
		if (ps.length === 0) {
			uSDK.debug.log('set nothing');
			return null;
		}

		var ret = {};
		for (var i = 0; i < ps.length; ++ i) {
			var p = ps[i];
			var v = vs[i][0].val;
			if (CMDMAP.b2a[p] !== undefined) {
				p = CMDMAP.b2a[p];
			}

			ret[p] = { 'value': v };
		}

		return ret;
	}

	function parseDeviceStatus(response) {
		var data = translateResponse(response);
		var ps = data.params;
		var vs = data.vals;
		var result = {
			'changed': {} 
		};
		var changedCount = 0;
		for (var i = 0; i < ps.length; ++ i) {
			var p = ps[i];
			var v = vs[i];
			if (values[p] !== v) {
				++ changedCount;
				var c = {
					'oldValue': values[p],
					'value': v
				};
				result.changed[p] = c;
				values[p] = v;
			}
		}
		if (changedCount > 0) {
			result['status'] = JSON.parse(JSON.stringify(values));
			uSDK.em.fire('evtDeviceStatusChanged', result);
		}
		return data;
	}

	/**
	 * Interfaces
	 */
	function init(sdk) {
		uSDK = sdk;
		document.addEventListener('DOMContentLoaded', onReady);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		return new Promise(function(resolve, reject) {
			var acmd = translateCommand(cmd);
			if (acmd === null) {
				uSDK.utils.later(0).then(function() {
					reject('command is invalid: ' + JSON.stringify(cmd));
				});
			} else {
				DA.setDeviceStatus(deviceID, acmd);
				registerResponse({
					resolve: resolve,
					reject: reject,
					cmd: acmd
				});
			}
		});
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		return new Promise(function(resolve, reject) {
			function onData(data) {
				var response = data;
				/*
				try {
					response = JSON.parse(data);
				} catch (e) {
					response = data;
				}
				*/

				resolve(parseDeviceStatus(response));
			}

			if (subDeviceID === null) {
				DA.getDeviceStatus(deviceID, onData);
			} else {
				reject('SubDevice currently is not supported');
			}
		});
	}

	return {
		'init': init,
		'setDeviceStatus': setDeviceStatus,
		'getDeviceStatus': getDeviceStatus
	};
})();

