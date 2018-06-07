/**
 * Adapter for BroadLink
 *
 */
var ADPT = (function() {
	var BRIDGE = "BLNativeBridge";
	var UPDATE_INFO_INTERVAL = 3000; // in ms
	var UPDATE_STATUS_INTERVAL = 3000; // in ms

	// when the document is ready, load cordova dynamically
	function onDocumentReady() {
		document.removeEventListener('DOMContentLoaded', onDocumentReady, false);

		try {
			if (!window.cordova) {
				var script = document.createElement('script');
				script.src = '../../cordova.js';
				document.getElementsByTagName('head')[0].appendChild(script);
			}
			document.addEventListener('deviceready', onCordovaReady, false);
		} catch (e) {
			SDK.debug.log(e);
		}
	}

	// when cordova is ready, call 'startup' method
	function onCordovaReady() {
		document.removeEventListener('deviceready', onCordovaReady, false);
		SDK.newPromise(getDeviceInfo).then(function(data) {
			SDK.em.fire('evtKickoff', {});
			startUpdateDeviceInfo(true);
			startUpdateStatus(true);
		}).catch(function(e) {
			// alert('Cordova error: ' + e);
			SDK.debug.trace();
			SDK.debug.log(e);
		});
	}

	function getDeviceInfo(onSuccess, onError) {
		cordova.exec(onSuccess, onError, BRIDGE, 'deviceinfo', []);
	}

	function startUpdateDeviceInfo(immediately) {
		if (immediately) {
			SDK.newPromise(getDeviceInfo).then(function(data) {
				var info = JSON.parse(data);
				var changed = {};
				var changedCount = 0;
				for (var k in info) {
					if (k === 'deviceStatus') {
						info[k] = (info[k] == 0 || info[k] == 3) ? false : true;
					}
					if (deviceInfo[k] !== info[k]) {
						changed[k] = {
							oldValue: deviceInfo[k],
							value: info[k]
						};
						++ changedCount;
						deviceInfo[k] = info[k];
					}
				}
				if (changedCount > 0) {
					SDK.em.fire('evtDeviceInfoChanged', {changed: changed});
				}
				startUpdateDeviceInfo();
			}).catch(function(e) {
				SDK.debug.log(e);
				startUpdateDeviceInfo();
			});
		} else {
			window.setTimeout(function() {
				startUpdateDeviceInfo(true);
			}, UPDATE_INFO_INTERVAL);
		}
	}

	function startUpdateStatus(immediately) {
		if (immediately) {
			if (deviceInfo.deviceID === undefined) {
				window.setTimeout(function() {
					startUpdateStatus(true);
				}, 200);
				return;
			}
			getDeviceStatus(deviceInfo.deviceID, null, []).then(function(data) {
				startUpdateStatus();
			}).catch(function(e) {
				startUpdateStatus();
			});
		} else {
			window.setTimeout(function() {
				startUpdateStatus(true);
			}, UPDATE_STATUS_INTERVAL);
		}
	}
	function parseDeviceStatus(response) {
		if (response.status && response.status !== 0) {
			SDK.debug.log('status != 0: ' + s);
			return null;
		}

		data = response.data;
		var ps = data.params;
		var vs = data.vals;
		if (ps === undefined || vs === undefined) {
			SDK.debug.log('"params" and "vals" must exist: ' + s);
			return null;
		}

		if (!Array.isArray(ps) || !Array.isArray(vs) || ps.length != vs.length) {
			SDK.debug.log('ps and vs should be both array and have the same length: ' + s);
			return null;
		}

		// get the real value (ignore "idx")
		for (var i = 0; i < vs.length; ++ i) {
			var v = vs[i][0];
			if (v.val === undefined) {
				SDK.debug.log('vs[' + i + '][0] doesn\'t have "val": ' + JSON.stringify(v));
				return null;
			}
			vs[i] = v.val;
		}

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
			SDK.em.fire('evtDeviceStatusChanged', result);
		}
		return data;
	}

	
	var SDK = null;
	var deviceInfo = {};
	var values = {};

	/**
	 * Interfaces
	 */
	function init(sdk) {
		SDK = sdk;

		document.addEventListener('DOMContentLoaded', onDocumentReady, false);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				if (response.status !== 0) {
					reject(STRINGS.error.cmd + '(' + response.status + ')');
				} else {
					resolve(parseDeviceStatus(response));
				}
			}

			function onFailed(e) {
				reject(e);
			}

			var command = [deviceID, subDeviceID, cmd, 'dev_ctrl']; // TODO
			cordova.exec(onSucceed, onFailed, BRIDGE, 'devicecontrol', command);
		});
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				if (response.status !== 0) {
					reject(STRINGS.error.cmd + '(' + response.status + ')');
				} else {
					resolve(parseDeviceStatus(response));
				}
			}

			function onFailed(e) {
				reject(e);
			}

			var cmd;
			if (status.length > 0) {
				cmd = SDK.newCommand(deviceID, SDK.profile.srvs[0], 'get', status, []);
			} else {
				cmd = SDK.newCommand(deviceID, SDK.profile.srvs[0], 'get', SDK.getAllParams(), []);
			}
			var command = [deviceID, subDeviceID, cmd, 'dev_ctrl'];
			cordova.exec(onSucceed, onFailed, BRIDGE, 'devicecontrol', command);
		});
	}

	function registerNotification() {
		function onSucceed(data) { // TODO
			registerNotification();

			data = JSON.parse(data);
		}

		function onFailed(e) {
			registerNotification();
		}

		cordova.exec(onSucceed, onFailed, BRIDGE, 'notification', []);
	}

	return {
		'init': init,
		'setDeviceStatus': setDeviceStatus,
		'getDeviceStatus': getDeviceStatus
	};
})();

