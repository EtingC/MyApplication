/**
 * Fake Adapter, for debug
 *
 */
var ADPT = (function() {
	var UPDATE_INFO_INTERVAL = 3000; // in ms
	var UPDATE_STATUS_INTERVAL = 3000; // in ms
	var API_URL = '/v1/device/control';

	var SDK = null;
	var values = {};
	var deviceInfo = {};
	/*
	var user = {
		'user': { 'name': 'dehou' }
	};
	*/
	var docCookies = { // from: https://developer.mozilla.org/en-US/docs/Web/API/Document/cookie
		getItem: function (sKey) {
			if (!sKey) { return null; }
			return decodeURIComponent(document.cookie.replace(new RegExp("(?:(?:^|.*;)\\s*" + encodeURIComponent(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=\\s*([^;]*).*$)|^.*$"), "$1")) || null;
		},
		setItem: function (sKey, sValue, vEnd, sPath, sDomain, bSecure) {
			if (!sKey || /^(?:expires|max\-age|path|domain|secure)$/i.test(sKey)) {
				return false;
			}
			var sExpires = "";
			if (vEnd) {
				switch (vEnd.constructor) {
					case Number:
						sExpires = vEnd === Infinity ? "; expires=Fri, 31 Dec 9999 23:59:59 GMT" : "; max-age=" + vEnd;
						break;
					case String:
						sExpires = "; expires=" + vEnd;
						break;
					case Date:
						sExpires = "; expires=" + vEnd.toUTCString();
						break;
				}
			}
			document.cookie = encodeURIComponent(sKey) + "=" + encodeURIComponent(sValue) + sExpires + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "") + (bSecure ? "; secure" : "");
			return true;
		},
		removeItem: function (sKey, sPath, sDomain) {
			if (!this.hasItem(sKey)) { return false; }
			document.cookie = encodeURIComponent(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT" + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "");
			return true;
		},
		hasItem: function (sKey) {
			if (!sKey) { return false; }
			return (new RegExp("(?:^|;\\s*)" + encodeURIComponent(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=")).test(document.cookie);
		},
		keys: function () {
			var aKeys = document.cookie.replace(/((?:^|\s*;)[^\=]+)(?=;|$)|^\s*|\s*(?:\=[^;]*)?(?:\1|$)/g, "").split(/\s*(?:\=[^;]*)?;\s*/);
			for (var nLen = aKeys.length, nIdx = 0; nIdx < nLen; nIdx++) { aKeys[nIdx] = decodeURIComponent(aKeys[nIdx]); }
			return aKeys;
		}
	};

	// when the document is ready, load cordova dynamically
	function onDocumentReady() {
		document.removeEventListener('DOMContentLoaded', onDocumentReady, false);
		SDK.utils.later(0).then(function() {
			SDK.em.fire('evtKickoff', {});
			function waitingDeviceID(evt, param) {
				if (deviceInfo.deviceID !== undefined) {
					SDK.em.unsubscribe('evtDeviceInfoChanged', waitingDeviceID);
					startUpdateStatus(true);
				}
			}
			SDK.em.subscribe('evtDeviceInfoChanged', waitingDeviceID);
			startUpdateInfo(true);
		});
	}

	function getQueryVariable(variable) {
		var query = window.location.search.substring(1);
		var vars = query.split('&');
		for (var i = 0; i < vars.length; i++) {
			var pair = vars[i].split('=');
			if (decodeURIComponent(pair[0]) == variable) {
				return decodeURIComponent(pair[1]);
			}
		}
		return null;
	}

	function startUpdateInfo(immediately) {
		if (immediately) {
			var deviceID = null;
			var devlist = docCookies.getItem('devlist');
			if (devlist == null) {
				SDK.debug.log('devlist is missing');
				return;
			}
			devlist = devlist.split('&');
			var index = getQueryVariable('index');
			if (index === null) {
				SDK.debug.log('index is missing');
				return;
			}
			if (index >= devlist.length) {
				SDK.debug.log('index is out of range(' + index + ' of ' + devlist.length + ')');
				return;
			}

			var devstr = devlist[index];
			devstr = devstr.split('_');
			if (devstr.length !== 5) {
				SDK.debug.log('format of devstr is invalid: ' + devstr);
				return;
			}

			deviceID = devstr[2];
			var signature = docCookies.getItem('signature');
			if (signature === null) {
				SDK.debug.log('Signature is missing');
				return;
			}
			deviceInfo.signature = signature;
			var info = {
				'deviceID':      deviceID,
				'networkStatus': {'status': 'available'}
			};

			var changed = {};
			var changedCount = 0;
			for (var k in info) {
				if (k === 'networkStatus') {
					var ns = deviceInfo.networkStatus;
					if (ns === undefined || info.networkStatus.status !== ns) {
						changed[k] = {
							oldValue: deviceInfo[k],
							value: info[k]
						};
						++ changedCount;
						deviceInfo[k] = info[k];
					}
				} else if (deviceInfo[k] !== info[k]) {
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

			startUpdateInfo();
		/*
		} else {
			SDK.utils.later(UPDATE_INFO_INTERVAL).then(function() {
				startUpdateInfo(true);
			});
		*/
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
			if (UPDATE_STATUS_INTERVAL > 0) {
				SDK.utils.later(UPDATE_STATUS_INTERVAL).then(function() {
					startUpdateStatus(true);
				});
			}
		}
	}

	function parseDeviceStatus(response) {
		if (response.errcode && response.errcode !== 0) {
			SDK.debug.log('status != 0: ' + s);
			return null;
		}

		data = response;//.data;
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

	function handleResponseErrcode(errcode) {
		var deviceStatus = errcode === -3 ? false : true;
		if (deviceInfo.deviceStatus !== deviceStatus) {
			var changed = { deviceStatus: {
				value: deviceStatus,
				oldValue: deviceInfo.deviceStatus
			}};
			deviceInfo.deviceStatus = deviceStatus;
			SDK.em.fire('evtDeviceInfoChanged', {changed: changed});
		}
	}
	
	/**
	 * Interfaces
	 */
	function init(sdk) {
		SDK = sdk;

		document.addEventListener('DOMContentLoaded', onDocumentReady, false);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		JSDK.debug.log(cmd);
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				handleResponseErrcode(data.errcode);
				if (response.errcode !== 0) {
					reject(STRINGS.error.cmd + '(' + response.errcode + ')');
				} else {
					resolve(parseDeviceStatus(response));
				}
			}

			function onFailed(e) {
				reject(e);
			}

			handleCommand(cmd, onSucceed, onFailed);
		});
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		return new Promise(function(resolve, reject) {
			function onSucceed(data) {
				var response = JSON.parse(data);
				handleResponseErrcode(data.errcode);
				if (response.errcode !== 0) {
					reject(STRINGS.error.cmd + '(' + response.errcode + ')');
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
				cmd = SDK.newCommand(deviceID, SDK.profile.srvs[0], 'get', SDK.getAllParams(), []);
			}
			handleCommand(cmd, onSucceed, onFailed);
		});
	}

	function handleCommand(cmd, onSuccess, onFailed) {
		cmd.pid = SDK.profile.desc.pid; // cloud needs pid to looking for the parse script.
		var http = new XMLHttpRequest(); 
		cmd = JSON.stringify(cmd);
		http.open('POST', API_URL, true);
		http.setRequestHeader("Content-length", cmd.length);
		http.setRequestHeader("Connection", "close");
		http.onreadystatechange = function() {
			if(http.readyState == 4) {
				if (http.status == 200) {
					onSuccess(http.responseText);
				} else {
					onFailed(STRINGS.error.cmd + '(' + http.status + ')');
				}
			}
		}
		try {
			http.responseType = 'text';
			http.withCredentials = true;
		} catch (e) {}
		http.send(cmd);
	}

	return {
		'init': init,
		'setDeviceStatus': setDeviceStatus,
		'getDeviceStatus': getDeviceStatus
	};
})();

