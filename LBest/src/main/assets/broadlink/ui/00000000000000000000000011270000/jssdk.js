/**
 * define the common JS sdk
 */
var JSDK = (function() {
	"use strict";
	/** make setTimeout() a promise, usage:
	 *	later(1000, 'any data').then(function(data) {
	 *		// do anything
	 *		return later(2000, data); // if you want to chain
	 *	}).then(function(data) {
	 *		// do anything...
	 *	});
	 */
	function later(delay, data) {
		return new Promise(function(resolve, reject) {
			if (delay === undefined) {
				delay = 0;
			}
			window.setTimeout(function() {
				resolve(data);
			}, delay);
		});
	}

	var debug = (function() {
		var list = null;
		function log(str, prtTrace) {
			try {
				if (log.caller != null) {
					str = log.caller.name + ': ' + str;
				}
			} catch(e) {}
	
			if (list == null) {
				list = document.getElementById('debug');
				if (list == null) {
					list = document.createElement('ul');
					list.id = 'debug';
					document.getElementsByTagName('body')[0].appendChild(list);
				}
			}
			var li = document.createElement('li');
			li.innerHTML = str;
			later().then(function() {
				list.insertBefore(li, list.firstChild);
			});
			if (prtTrace) {
				trace();
			}
		}

		function trace() {
			try {
				window['3.1415926535'].x = 2.7182818284;
			} catch (e) {
				if (e.stack) {
					log('call stack: ' + e.stack);
				}
			}
		}

		return {
			log:          log,
			trace:        trace
		}
	})();

	// event manager
	var em = (function() {
		var CHECKDUP = false; // TODO: use ref counter instead
		var map = {};
		function checkParams(events, subscriber) {
			var evts = null;
			if (typeof subscriber === 'function') {
				if ('string' === typeof events) {
					evts = [events];
				} else if (Array.isArray(events)){
					evts = events;
				}
			}
			return evts;
		}

		function subscribe(events, subscriber) {
			var evts = checkParams(events, subscriber);
			if (evts === null) {
				return this;
			}

			for (var i = 0; i < evts.length; ++ i) {
				var k = evts[i];
				if (map[k] === undefined) {
					map[k] = [];
				}
				if (!Array.isArray(map[k])) {
					// TODO: add log here
					map[k] = [];
				}
				if (!CHECKDUP || map[k].indexOf(subscriber) === -1) {
					map[k].push(subscriber);
				}
			}
			return this;
		}

		function unsubscribe(events, subscriber) {
			var evts = checkParams(events, subscriber);
			if (evts === null) {
				return this;
			}

			for (var i = 0; i < evts.length; ++ i) {
				var k = evts[i];
				var m = map[k];
				if (m === undefined) {
					continue;
				}
				if (!Array.isArray(m)) {
					// TODO: log this error
					map[k] = undefined;
					continue;
				}
				if (CHECKDUP) {
					// TODO: use ref counter here
				}
				var idx = m.indexOf(subscriber);
				if (idx === -1) { // not found
					continue;
				}

				map[k] = m.slice(0, idx).concat(m.slice(idx + 1, m.length));
				if (map[k].length === 0) {
					map[k] = undefined;
				}
			}
			return this;
		}

		function fire(evt, param) {
			if (typeof evt !== 'string') {
				return false;
			}

			var m = map[evt];
			if (m === undefined || !Array.isArray(m)) {
				return false;
			}

			for (var i = 0; i < m.length; ++ i) {
				if (typeof m[i] === 'function') {
					m[i](evt, param);
				}
			}
			return true;
		}

		function shutdown() {
			map = {};
		}

		return {
			subscribe:   subscribe,
			unsubscribe: unsubscribe,
			fire:        fire,
			shutdown:    shutdown
		};
	})();

	// error handle
	var errcode = {
		ERR_OK:                 0,
		ERR_ERROR:              1, // general error
		ERR_INIT_FAILED:        2,
		ERR_JSON:               3,
		ERR_BRIDGE:             10,
		ERR_NETWORK_TIMEOUT:    1000,
		ERR_INVALID_PARAM:      2000,
	};

	// For promise
	try {
		if (window && window.Promise === undefined) {
			window.Promise = Q.promise;
		}
	} catch (e) {
		try {
			require(['Q'], function(Q) {
				window.Promise = Q.promise;
			});
		} catch (e) {
		}
	}

	// Create a promise for function func(callback) or func(onSucceed, onFailed);
	function newPromise(fn) {
		return new Promise(function(resolve, reject) {
			fn(function(data) {
				resolve(data);
			}, function(e) {
				reject(e);
			});
		});
	}

	function waitForDeviceID(evt, data) {
		if (data.changed && data.changed['deviceID']) {
			readonly.deviceID = data.changed.deviceID.value;

			window.setTimeout(function() {
				em.unsubscribe('evtDeviceInfoChanged', waitForDeviceID);

				fireReady();
			}, 0);
		}

	}

	function fireReady() {
		ready = true;
		em.subscribe('evtDeviceInfoChanged', onDeviceInfoChanged);
		em.fire('evtReady', {});
		if (readyCallback) {
			readyCallback();
			readyCallback = undefined;
		}
	}

	function onKickoff(evt, data) {
		em.unsubscribe('evtKickoff', onKickoff);

		if (readonly.deviceID === undefined) {
			em.subscribe('evtDeviceInfoChanged', waitForDeviceID);
		} else {
			fireReady();
		}
	}

	function onDeviceInfoChanged(evt, data) {
		if (data.changed && data.changed['deviceID']) {
			readonly.deviceID = data.changed.deviceID.value;
		}
	}

	function checkProfile() {
		if (!window.PROFILE) {
			alert("Profile doesn't exist");
			return false;
		}

		if (PROFILE.srvs === undefined || !Array.isArray(PROFILE.srvs) || PROFILE.srvs.length == 0) {
			alert("profile.srvs is invalid");
			return false;
		}

		return true;
	}

	// Device related
	var ready = false;
	var READY_TIMEOUT = 5000;
	function isReady() {
		return ready;
	}
	if (!checkProfile()) {
		return null;
	}
	var readonly = {
		profile: PROFILE,
		deviceID: undefined
	};
	function init() {
		ADPT.init(this);

		em.subscribe('evtKickoff', onKickoff);
		window.setTimeout(function() {
			if (!isReady()) {
				em.fire('evtSDKFailed', {});
			}
		}, READY_TIMEOUT);
	}

	function setDeviceStatus(deviceID, subDeviceID, cmd) {
		if (!isReady()) {
			debug.log('not ready');
			return null;
		}

		if (!cmdFactory.checkCmd(cmd)) {
			debug.log('cmd is invalid: ' + JSON.stringify(cmd));
			return null;
		}

		if (cmd.params.length === 0) {
			debug.log('nothing to control');
			return null; // nothing to control
		}

		return ADPT.setDeviceStatus(deviceID, subDeviceID, cmd);
	}

	function getDeviceStatus(deviceID, subDeviceID, status) {
		if (!isReady()) {
			return null;
		}

		if (status !== undefined && !Array.isArray(status)) {
			debug.log('status should be an array');
			return null;
		}

		return ADPT.getDeviceStatus(deviceID, subDeviceID, status === undefined ? [] : status);
	}

	function getIntf(k) {
		return PROFILE.suids[0].intfs[k];
	}

	function getAllParams() {
		return Object.keys(PROFILE.suids[0].intfs);
	}

	var cmdFactory = (function() {
		function checkCommand(cmd) {
			if (cmd == null) {
				return false;
			}
			var props = ['did', 'srv', 'act', 'params', 'vals'];
			for (var i = 0; i < props.length; ++ i) {
				if (!cmd.hasOwnProperty(props[i])) {
					return false;
				}
			}
	
			if (['get', 'set'].indexOf(cmd.act) === -1) {
				return false;
			}
	
			if (!Array.isArray(cmd.params) || !Array.isArray(cmd.vals)) {
				return false;
			}
			if (cmd.act !== 'get') {
				if (cmd.params.length != cmd.vals.length) {
					return false;
				}
			} else {
				if (cmd.vals.length != 0) {
					return false;
				}
			}
	
			return true;
		}

		function getValueString(value) {
			return '[{"val":' + JSON.stringify(value) + ',"idx":1}]';
		}
	
		// @return null if failed (invalid action)
		function newCommand(deviceID, srv, action, params, values) {
			if (action !== 'get' && action !== 'set') {
				debug.log('action must be "get" or "set"');
				return null;
			}
			var isget = action === 'get';

			if ((params == null) != (values == null)) {
				debug.log('params and values must both be null or not');
				return null;
			}

			if (params && !Array.isArray(params)) {
				debug.log('params must be an array');
				return null;
			}

			if (values && !Array.isArray(values)) {
				debug.log('values must be an array');
				return null;
			}

			if (params != null && values != null && (params.length != values.length)) {
				if (!isget) {
					debug.log('params and values must have the same items if not "get".');
					return null;
				}
			}
	
			var cmd = {
				did: deviceID,
				srv: srv,
				act: action,
				params: [],
				vals: []
			};
			
			if (params != null) {
				for (var i = 0; i < params.length; ++ i) {
					cmd.params.push(params[i]);
					if (!isget) {
						cmd.vals.push(JSON.parse(getValueString(values[i])));
					}
				}
			}

			return cmd;
		}
	
		function addParam(cmd, param, value) {
			if (!checkCommand(cmd)) {
				return false;
			}
	
			var p = param;
			var v = getvstring(value);
	
			var idx = -1;
			for (var i = 0; i < cmd.params.length; ++ i) {
				if (cmd.params[i] === p) {
					cmd.vals[i] = v;
					return true;
				}
			}
	
			cmd.params.push(p);
			cmd.vals.push(v);
			return true;
		}

		return {
			checkCmd: checkCommand,
			newCommand: newCommand,
			addParam: addParam
		}
	})();

	var readyCallback = null;
	function setReadyCallback(fn) {
		if (readyCallback === null) {
			if (isReady()) {
				fn();
			} else {
				readyCallback = fn;
			}
		}
	}

	var sdk = {
		// error code
		errcode:            errcode,
		
		// methods
		'init':             init,

		// device
		'isReady':          isReady,
		'ready':            setReadyCallback,
		'newCommand':       cmdFactory.newCommand,
		'addParam':         cmdFactory.addParam,
		'setDeviceStatus':  setDeviceStatus,
		'getDeviceStatus':  getDeviceStatus,
		'getIntf':          getIntf,
		'getAllParams':     getAllParams,

		// event 
		em:                 em,

		// promise
		'newPromise':       newPromise,

		// debug
		'debug':            debug,

		// utils
		'utils':            {
			later:      later
		}
	};

	// readonly properties
	['profile', 'deviceID'].forEach(function(name) {
		Object.defineProperty(sdk, name, {
			get: function() {
				return readonly[name];
			}
		});
	});

	return sdk;
})();

JSDK.init();

