(function() {
function $(id) { // we don't use jquery :)
	return document.getElementById(id);
}

var panels = {
	on: null,
	off: null
};
var hasPwr = true;

var sdkerrormsg = null;
var deviceofflinemsg = null;
function onDOMContentLoaded() {
	document.removeEventListener('DOMContentLoaded', onDOMContentLoaded, false);
	JSDK.em.subscribe('evtSDKFailed', function(evt, param) {
		sdkerrormsg = UIKIT.popup.error.show(STRINGS.error.sdk);
	});

	// create on-panel and off-panel
	['on', 'off'].forEach(function(key) {
		panel = document.createElement('div');
		panel.className = 'kit-content';
		panel.id = key + 'panel';
		panel.style.visibility = 'hidden';
		$('kit-content').appendChild(panel);
		panels[key] = panel;
	});
	
	UIKIT.popup.waiting.show(); // wait for SDK ready
	onSDKReady();
}

function onSDKReady() {
	if (sdkerrormsg != null) {
		UIKIT.popup.error.close(sdkerrormsg);
		sdkerrormsg = null;
	}
	JSDK.em.subscribe('evtDeviceStatusChanged', onFirstDeviceStatusChanged);
	JSDK.em.subscribe('evtDeviceInfoChanged', onDeviceInfoChanged);

	var did = JSDK.deviceID;
	var intfs = JSDK.profile.suids[0].intfs;
	hasPwr = intfs['pwr'] === undefined ? false : true;
	var srv = JSDK.profile.srvs[0];
	var cnt = hasPwr ? 1 : 0;
	for (var k in intfs) {
		if (k === 'pwr') {
			var p = UIKIT.utils.createElementFromTemplate({tag: 'p', cls: 'kit-ctrl-container'});
			p.innerHTML = STRINGS.intfs[k].name;
			panels.off.appendChild(p);

			var pos = {sx:.1,sy:0.25,ex:3.2,ey:.65}
			UIKIT.utils.leftElement(p, pos);

			pos.sx = 3.7;
			pos.ex = 4.7;
			UIKIT.controls.createOnOffButton(panels.off, pos, did, srv, 'pwr');
		} else {
			var intf = intfs[k][0];

			var y = cnt + 1;// / 2 + 0.5
			if (intf.act == 1) {
				var pos = {sx:.1,sy:y,ex:4.7,ey:y + .4};
				UIKIT.controls.createPlainView(panels.on, pos, did, srv, k, intf.in[0], STRINGS.intfs[k]);
			} else {
				if (intf.in[0] == 1/* && intf.in.length > 3*/) {
					var pos = {sx:.1,sy:y,ex:4.7,ey:y + .4};
					UIKIT.controls.createEnum(panels.on, pos, did, srv, k, intf.in, STRINGS.intfs[k]);
				} else if (intf.in[0] == 2) {
					var pos = {sx:.1,sy:y,ex:4.7,ey:y + .4};
					UIKIT.controls.createRange(panels.on, pos, did, srv, k, intf.in, STRINGS.intfs[k]);
				} else {
					var p = UIKIT.utils.createElementFromTemplate({tag: 'p', cls: 'kit-ctrl-container'});
					p.innerHTML = STRINGS.intfs[k].name;
					panels.on.appendChild(p);
	
					var pos = {sx:.1,sy:y,ex:3.2,ey:y + .4};
					UIKIT.utils.leftElement(p, pos);
	
					if (intf.in[0] == 1 && intf.in.length == 3) {
						pos.sx = 3.7;
						pos.ex = 4.7;
						UIKIT.controls.createOnOffButton(panels.on, pos, did, srv, k);
					}
				}
			}
			++ cnt;
		}
	}

	panels.off.style.visibility = '';
	if (!hasPwr) {
		panels.on.style.visibility = '';
	}	
}
document.addEventListener('DOMContentLoaded', onDOMContentLoaded, false);
//JSDK.ready(onSDKReady);

// utils
function onDeviceInfoChanged(evt, data) {
	var changed = data.changed;
	for (var k in changed) {
		if (k === 'deviceName') {
			document.title = changed[k].value;
		} else if (k === 'deviceStatus') {
			if (changed[k].value === true) {
				if (deviceofflinemsg != null) {
					UIKIT.popup.error.close(deviceofflinemsg);
					deviceofflinemsg = null;
				}
			} else {
				if (deviceofflinemsg === null) {
					deviceofflinemsg = UIKIT.popup.error.show(STRINGS.error.device_offline);
				}
			}
		}
	}
}

function onFirstDeviceStatusChanged(evt, data) {
	UIKIT.popup.waiting.close();

/*
	panels.off.style.visibility = '';
	if (!hasPwr) {
		panels.on.style.visibility = '';
	}
*/

	JSDK.em.unsubscribe('evtDeviceStatusChanged', onFirstDeviceStatusChanged);
	JSDK.em.subscribe('evtDeviceStatusChanged', onDeviceStatusChanged);

	onDeviceStatusChanged(evt, data);
}

function onDeviceStatusChanged(evt, data) {
	var changed = data.changed;
	if (changed['pwr']) {
		if (changed['pwr'].value == 1) {
			panels.on.style.visibility = '';
		} else {
			panels.on.style.visibility = 'hidden';
		}
	}
}

})();

