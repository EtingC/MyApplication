/**
 * Fake Adapter, for debug
 *
 */
var ADPT = (function() {
	var UPDATE_INFO_INTERVAL = 3000; // in ms
	var UPDATE_STATUS_INTERVAL = 3000; // in ms

	var ID = '7654321';

	var SDK = null;
	var deviceInfo = null;
	var values = {};
	var deviceInfo = {};
	/*
	var user = {
		'user': { 'name': 'dehou' }
	};
	*/

	// when the document is ready, load cordova dynamically
	function onDocumentReady() {
		document.removeEventListener('DOMContentLoaded', onDocumentReady, false);

		function kickoff(sInfo) {
			try {
				SDK.em.fire('evtKickoff', {});
				startUpdateInfo(true);
				startUpdateStatus(true);
			} catch (e) {
				SDK.debug.trace();
				SDK.debug.log(e);
			}
		}
		SDK.utils.later(1000).then(kickoff);

	}

	var sui = 0;
	function startUpdateInfo(immediately) {
		if (immediately) {
			++ sui;
			var info = {
				'deviceID':      ID,
				'deviceName':    "dehou's machine",
				'deviceStatus':  true,
				'networkStatus': {'status': 'available', 'more': {'type': 'Wi-Fi'}}
			};
			// test offline >
			if (sui > 1 && sui < 3) {
				info.deviceStatus = false;
			}
			// < test offline

			var changed = {};
			var changedCount = 0;
			for (var k in info) {
				if (deviceInfo[k] !== info[k]) {
					changed[k] = {
						oldValue: deviceInfo[k],
						value: info[k]
					};
					if (k === 'deviceStatus') {
						++ changedCount;
					}
					++ changedCount;
					deviceInfo[k] = info[k];
				}
			}
			if (changedCount > 0) {
				SDK.em.fire('evtDeviceInfoChanged', {changed: changed});
			}

			startUpdateInfo();
		} else {
			SDK.utils.later(UPDATE_INFO_INTERVAL).then(function() {
				startUpdateInfo(true);
			});
		}
	}

	function startUpdateStatus(immediately) {
		if (immediately) {
			var p = getDeviceStatus(deviceInfo.deviceID, null, []);
			p.then(function(data) {
				startUpdateStatus();
			}).catch(function(e) {
				startUpdateStatus();
			});
		} else {
			SDK.utils.later(UPDATE_STATUS_INTERVAL).then(function() {
				startUpdateStatus(true);
			});
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

	var device = (function() {
		var states;
		function initIntfs(intfs) {
			states = JSON.parse(JSON.stringify(intfs));
			for (var k in states) {
				var v = states[k][0];
				switch(v.in[0]) {
				case 1:
				case 2:
					v.val = v.in[1];
					break;
				case 3:
					v.val = 'unknown';
					break;
				}
			}
		}

		function checkValueValid(state, val) {
			if (state.in[0] == 1) {
				for (var i = 1; i < state.in.length; ++ i) {
					if (val === state.in[i]) {
						return true;
					}
				}
			} else if (state.in[0] == 2) {
				if (val >= state.in[1] && val <= state.in[2]) {
					return true;
				}
			} else {
				return true;
			}
			return false;
		}

		var handleCount = 0;
		function handleCommand(cmd, onSuccess, onFailed) {
			if (cmd.did !== ID) {
				SDK.debug.log('DeviceID does not match');
			}
			document.title = handleCount + ' times';
			SDK.utils.later(200 + Math.random() * 1000).then(function() {
				var response = {
					status: 0,
					data: {
						params: [ ],
						vals: [ ]
					}
				};
				++ handleCount;
				if (handleCount % 5 == 0) {
					response.status = -1000;
					onSuccess(JSON.stringify(response));
					return;
				}
				var params = response.data.params;
				var vals = response.data.vals;
				if (cmd.act == 'get' || cmd.act == 'set') {
					var set = cmd.act == 'set';
					if (!set && cmd.params.length == 0) {
						for (var k in states) {
							cmd.params.push(k);
						}
					}
					for (var i = 0; i < cmd.params.length; ++ i) {
						var p = cmd.params[i];
						var state = states[p][0];
						if (state !== undefined) {
							params.push(p);
							if (set) {
								var val = cmd.vals[i][0].val;
								if (!checkValueValid(state, val)) {
									onFailed({
										code: SDK.errcode.ERR_INVALID_PARAM,
										error: 'params: ' + p + ' has invalid value: ' + val
									});
									return;
								}
								state.val = val;
							}
							vals.push([{"idx": 1, "val": state.val}]);
						}
					}
					onSuccess(JSON.stringify(response));
				} else {
					onFailed(JSON.stringify({
						code: SDK.errcode.ERR_INVALID_PARAM,
						error: 'act must be "get" or "set"'
					}));
				}
			});
		}

		return {
			init: initIntfs,
			handleCommand: handleCommand
		}
	})();
	
	/**
	 * Interfaces
	 */
	function init(sdk) {
		SDK = sdk;
		device.init(SDK.profile.suids[0].intfs);

		document.addEventListener('DOMContentLoaded', onDocumentReady, false);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				if (response.status !== 0) {
					reject(STRINGS.error.cmd);
				} else {
					resolve(parseDeviceStatus(response));
				}
			}

			function onFailed(e) {
				reject(e);
			}

			device.handleCommand(cmd, onSucceed, onFailed);
		});
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				if (response.status !== 0) {
					reject(STRINGS.error.cmd);
				} else {
					resolve(parseDeviceStatus(response));
				}
			}

			function onFailed(e) {
				reject(e);
			}

			var cmd;
			if (status.length > 0) {
				cmd = SDK.newCommand(deviceID, JSDK.profile.srvs[0], 'get', status, []);
			} else {
				cmd = SDK.newCommand(deviceID, JSDK.profile.srvs[0], 'get');
			}
			device.handleCommand(cmd, onSucceed, onFailed);
		});
	}

	return {
		'init': init,
		'setDeviceStatus': setDeviceStatus,
		'getDeviceStatus': getDeviceStatus
	};
})();

