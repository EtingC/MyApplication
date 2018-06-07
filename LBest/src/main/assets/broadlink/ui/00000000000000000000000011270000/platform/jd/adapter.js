/**
 * Adapter for JD
 *
 */

var ADPT = (function() {
	var RELEASE = false;
	var UPDATE_STATUS_INTERVAL = 5000; // in ms

	function onDocumentReady() {
		document.removeEventListener('DOMContentLoaded', onDocumentReady, false);

		try {
			if (window.JDSMART) {
				jSDK = JDSMART;
				jSDK.ready(function() {
					uSDK.em.fire('evtKickoff', {});
					//完成后
					uSDK.newPromise(jSDK.io.initDeviceData).then(function(data) {
						uSDK.newPromise(jSDK.app.getNetworkType).then(function(network) {
							if (data.device) {
								cache.deviceInfo = {
									deviceID: data.device.device_id,
									deviceName: data.device.device_name,
									deviceStatus: data.device.status == 1 ? true : false,
									networkStatus: {
										status:'available',
										more: {
											'type': network.TypeName == 'WIFI' ? 'Wi-Fi' : 'cellular'
										}
									}
								};
								var changed = {};
								['deviceID', 'deviceName', 'deviceStatus', 'networkStatus'].forEach(function(k) {
									changed[k] = {
										value: cache.deviceInfo[k]
									}
								});
								uSDK.em.fire('evtDeviceInfoChanged', {changed:changed});

								if (data.streams) {
									// uSDK.debug.log(JSON.stringify(data.streams));
									startUpdateStatus();
									parseStreams(data.streams);
								} else {
									startUpdateStatus(true);
								}
							} else {
								// TODO: report error..
							}
						});
					});
				});
			}
		} catch (e) {
			uSDK.debug.log(e);
		}
	}

	function startUpdateStatus(immediately) {
		if (immediately) {
			getDeviceStatus(cache.deviceInfo.deviceID, null, []).then(function(data) {
				startUpdateStatus();
			}).catch(function(e) {
				startUpdateStatus();
			});
		} else {
			if (window && window.setTimeout) {
				window.setTimeout(function() {
					startUpdateStatus(true);
				}, UPDATE_STATUS_INTERVAL);
			}
		}
	}

	function translateCommand(cmd) {
		var ret = {
			command: []
		};
		var command = ret.command;
		
		for (var i = 0; i < cmd.params.length; ++ i) {
			command.push({
				stream_id: cmd.params[i],
				current_value: cmd.vals[i][0].val
			});
		}

		if (window.customTranslateCommand) {
			ret = window.customTranslateCommand(ret);
		}

		return ret;
	}

	function parseStreams(streams) {
		try{
		if (streams === undefined || !Array.isArray(streams)) {
			uSDK.debug.log('result.streams must exist and be an array: ' + s);
			return;
		}

		if (!RELEASE) {
			for (var i = 0; i < streams.length; ++ i) {
				var it = streams[i];
				if (it.stream_id === undefined || it.current_value === undefined) {
					uSDK.debug.log('"stream_id" and "current_value" must exist: ' + s);
					return;
				}
				if (it.current_value === '') {
					it.current_value = '0';
				}
			}
		}

		var result = {
			'changed': {}
		};
		var changedCount = 0;
		for (var i = 0; i < streams.length; ++ i) {
			var it = streams[i];
			var p = it.stream_id;
			var v = it.current_value;
			var intf = uSDK.getIntf(p);
			if (intf === undefined || !Array.isArray(intf)) {
				continue;
			}
			intf = intf[0];
			if (intf !== undefined) {
				if (intf.in[0] === 1 || intf.in[0] === 2) {
					v = parseInt(v);
				}
			}
			if (cache.values[p] !== v) {
				++ changedCount;
				var c = {
					'oldValue': cache.values[p],
					'value': v
				};
				result.changed[p] = c;
				cache.values[p] = v;
			}
		}
		if (changedCount > 0) {
			result['status'] = JSON.parse(JSON.stringify(cache.values));
			uSDK.em.fire('evtDeviceStatusChanged', result);
		}
	}catch(e) {
		uSDK.debug.log(e);
	}
	}

	function parseOnlineStatus(status) {
		var v = status == 1 ? true : false;
		if (v != cache.deviceInfo.deviceStatus) {
			uSDK.em.fire('evtDeviceInfoChanged', { changed: {
				deviceStatus: {
					value: v,
					oldValue: cache.deviceInfo.deviceStatus
				}
			}});
			cache.deviceStatus.deviceStatus = v;
		}
	}

	function parseDeviceStatus(s) {
		var data;
		try {
			data = JSON.parse(s);
		} catch (e) {
			return null;
		}

		if (data.streams) {
			parseStreams(data.streams);
		}
		if (data.status !== undefined) {
			parseOnlineStatus(data.status);
		} else if (data.device && data.device.status !== undefined) {
			parseOnlineStatus(data.device.status);
		}

		return data;
	}

	
	var uSDK = null;
	var jSDK = null;
	var cache = {
		deviceInfo: {},
		values: {}
	};

	/**
	 * Interfaces
	 */
	function init(sdk) {
		uSDK = sdk;

		document.addEventListener('DOMContentLoaded', onDocumentReady, false);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var parsed = parseDeviceStatus(data);
				resolve(parsed);
			}

			function onFailed(e) {
				reject(e);
			}

			var command = translateCommand(cmd);
			jSDK.io.controlDevice(command, onSucceed, onFailed);
		});
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var parsed = parseDeviceStatus(data);
				resolve(parsed);
			}

			function onFailed(e) {
				reject(e);
			}

			jSDK.io.getSnapshot(onSucceed, onFailed);
		});
	}

	return {
		'init': init,
		'setDeviceStatus': setDeviceStatus,
		'getDeviceStatus': getDeviceStatus
	};
})();


