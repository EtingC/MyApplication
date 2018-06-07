var UIKIT = (function() {
	var GRID = {
		X: 5,
		Y: 8,
		size: 0,
		gap: {x: 0, y: 0}
	};
	var IDS = {
		content: 'kit-content',
		popup: 'kit-popup-layer',
		wait: 'kit-popup-wait',
		wait_ring: 'kit-popup-wait-ring',
		msgbox: 'kit-popup-msgbox',
		msgbox_text: 'kit-popup-msgbox-text',
		errbox: 'kit-popup-errbox',
		errbox_text: 'kit-popup-errbox-text'
	};

	function $(id) { // we don't use jquery :)
		return document.getElementById(id);
	}

	function gridGetX(x) {
		var ix = Math.floor(x);
		var fx = x - ix;
		return ix * (GRID.size + GRID.gap.x) + fx * GRID.size;
	}

	function gridGetY(y) {
		var iy = Math.floor(y);
		var fy = y - iy;
		return iy * (GRID.size + GRID.gap.y) + fy * GRID.size;
	}

	function testGrid() {
		window.addEventListener('resize', function() {
			document.title = window.innerWidth + 'x' + window.innerHeight;
		}, false);

		var body = document.getElementsByTagName('body')[0];
		for (var y = 0; y < GRID.Y; ++ y) {
			for (var x = 0; x < GRID.X; ++ x) {
				var cell = document.createElement('div');
				cell.className = 'kit-grid-cell';
				cell.grid = {x:x, y:y};
				cell.style.width = GRID.size + 'px';
				cell.style.height = GRID.size + 'px';
				var px = gridGetX(x);
				var py = gridGetY(y);
				cell.style.transform = 'translate(' + px + 'px, ' + py + 'px)';
				cell.style.webkitTransform = cell.style.transform;

				body.appendChild(cell);
			}
		}
	}

	function createContainers() {
		var body = document.getElementsByTagName('body')[0];
		if (body == null) {
			return false;
		}

		var data = {
			'kit-content': '',
			'kit-popup-layer': 'hidden'
		};
		for (var id in data) {
			var elem = document.createElement('div');
			elem.id = id;
			elem.className = data[id];

			// Android prior v4.4 doens't support 'vw' and 'vh', so we specify them here
			elem.style.width = window.innerWidth + 'px';
			body.appendChild(elem);
		};
		$('kit-popup-layer').style.height = window.innerHeight + 'px';

		return true;
	}

	function onLoad() {
		document.removeEventListener('DOMContentLoaded', onLoad, false);

		GRID.size = parseInt(window.innerWidth / GRID.X);
		GRID.gap.y = parseInt((window.innerHeight - (GRID.size * GRID.Y)) / (GRID.Y - 1));
		if (GRID.gap.y < 0) {
			GRID.gap.y = 0;
			GRID.size = parseInt(window.innerHeight / GRID.Y);
			GRID.gap.x = parseInt((window.innerWidth - GRID.size * GRID.X) / (GRID.X - 1));
		}

//		testGrid();
		if (!createContainers()) {
			alert('Create container failed.');
			return;
		}

		['touchstart', 'touchend', 'touchcancel', 'touchleave', 'touchmove'].forEach(function(evt) {
			document.addEventListener(evt, onTouch, false);
		});
		JSDK.em.subscribe(['evtDeviceStatusChanged'], onDeviceStatusChanged);
	}

	function onTouch(evt) {
//		evt.preventDefault();
	}

	function onDeviceStatusChanged(evt, data) {
		var changed = data.changed;
		if (changed === undefined) {
			return;
		}
	}

	var popup = (function() {
		var popupCount = 0;
		function addref() {
			++ popupCount;
			if (popupCount === 1) {
				$(IDS.popup).classList.remove('hidden');
			}
		}
		function release() {
			-- popupCount;
			if (popupCount === 0) {
				$(IDS.popup).classList.add('hidden');
			}
		}
		var waiting = (function() {
			var count = 0;
			function show() {
				var popup = $(IDS.popup);
				if (popup == null) {
					return false;
				}
				var wait = $(IDS.wait);
				if (wait == null) {
					wait = createElementFromTemplate({
						tag: 'div', id: IDS.wait, children: [{
							tag: 'div', id: IDS.wait_ring
						}]
					});

					var width = Math.floor(GRID.size * 1.5 + 0.5);
					var height = Math.floor(GRID.size * 1.5 + 0.5);
					center(wait, width, height);
					popup.appendChild(wait);
				}
				++ count;
				addref();
			}

			function close() {
				if (count == 0 || popupCount == 0) {
					JSDK.debug.log('count and popupCount is error: ' + count + ' and ' + popupCount);
				}
				release();
				-- count;
				if (count === 0) {
					var wait = $(IDS.wait);
					if (wait != null) {
						wait.parentNode.removeChild(wait);
					}
				}
			}

			return {
				show: show,
				close: close
			};
		})();

		var msgbox = (function() {
			var timer = null;
			function show(msg, timeout) {
				var popup = $(IDS.popup);
				if (popup == null) {
					return false;
				}
				if (timeout === undefined) {
					timeout = 3000;
				}
				var mb = $(IDS.msgbox);
				if (mb == null) {
					mb = createElementFromTemplate({
						tag: 'div', id: IDS.msgbox, style: { visibility: 'hidden' }, children: [{
							tag: 'div', id: IDS.msgbox_text, innerHTML: msg
						}]
					});

					popup.appendChild(mb);
				}
				JSDK.utils.later(0).then(function() {
					center(mb);
					mb.style.visibility = '';
				});

				if (timer != null) {
					window.clearTimeout(timer);
					timer = null;
				} else {
					addref();
				}
				timer = window.setTimeout(function() {
					var mb = $(IDS.msgbox);
					mb.parentNode.removeChild(mb);
					release();
					timer = null;
				}, timeout);
				return true;
			}

			return {
				show: show
			};
		})();

		var menu = (function() {
			var _m = null;
			function popup(m) {
				if (_m !== null) {
					JSDK.debug.log('There should be only on popup menu at the same time');
					return false;
				}
				_m = m;
				var layer = $(IDS.popup);
				layer.classList.add('menubg');
				m.style.position = 'absolute';
				m.style.transform = 'translate(0, ' + window.innerHeight + 'px)';
				m.style.webkitTransform = 'translate(0, ' + window.innerHeight + 'px)';
				layer.appendChild(m);
				addref();
				JSDK.utils.later(0).then(function() {
					var y = window.innerHeight - m.scrollHeight;
					m.style.transform = 'translate(0, ' + y + 'px)';
					m.style.webkitTransform = 'translate(0, ' + y + 'px)';
				});
				layer.onclick = function() {
					close();	
				}

				return true;
			}

			function close() {
				var layer = $(IDS.popup);
				layer.onclick = null;
				layer.classList.remove('menubg');
				layer.removeChild(_m);
				_m = null;
				release();
			}
			return {
				popup: popup,
				close: close
			};
		})();

		var error = (function() {
			var currid = 1;
			var map = {};
			function show(msg) {
				var popup = $(IDS.popup);
				if (popup == null) {
					return false;
				}
				popup.classList.add('errorbg');
				var mb = $(IDS.errbox);
				if (mb == null) {
					mb = createElementFromTemplate({
						tag: 'div', id: IDS.errbox, style: { visibility: 'hidden' }, children: [{
							tag: 'div', id: IDS.errbox_text, innerHTML: msg
						}]
					});

					popup.appendChild(mb);
				} else {
					$(IDS.errbox_text).innerHTML = msg;
				}
				map[currid] = msg;
				JSDK.utils.later(0).then(function() {
					center(mb);
					mb.style.visibility = '';
				});

				addref();
				return currid ++;
			}

			function close(id) {
				if (map[id] === undefined) {
					JSDK.debug.log('id: ' + id + ' does not exist');
					return false;
				}
				if (popupCount == 0) {
					JSDK.debug.log('popupCount is 0');
				}
				release();
				var popup = $(IDS.popup);
				var keys = Object.keys(map);
				if (keys.length === 1) {
					popup.classList.remove('errorbg');
					var box = $(IDS.errbox);
					if (box != null) {
						box.parentNode.removeChild(box);
					}
				} else {
					delete map[id];
					keys = Object.keys(map);
					keys.sort(function(a, b) { return b - a; }); // Largest first
					msg = map[keys[0]];

					$(IDS.errbox_text).innerHTML = msg;
					JSDK.utils.later(0).then(function() {
						center($(IDS.errbox));
					});
				}
				return true;
			}
			return {
				show: show,
				close: close
			};
		})();

		/*
		 * var el = document.getElementById('el-id');
		 * 1. just center the el use its origin size:
		 *    center(el);
		 * 2. center the el with size (in px):
		 *    center(el, 100, 100);
		 */
		function center(elem, width, height) {
			var style = elem.style;
			if (width === undefined || height === undefined) {
				width = elem.scrollWidth;
				height = elem.scrollHeight;
			} else {
				style.width = width + 'px';
				style.height = height + 'px';
			}
			style.left = Math.floor((window.innerWidth - width) / 2 + 0.5) + 'px';
			style.top = Math.floor((window.innerHeight - height) / 2 + 0.5) + 'px';
		}

		return {
			waiting: waiting,
			msgbox: msgbox,
			error: error,
			menu: menu
		}
	})();

	// controls
	var controls = (function() {
		/**
		 * @param parentNode, could be null, use #kit-content instead
		 * @param pos, the position of the control. { sx: 3, sy: 2, ex: 3.99, ey: 2.99 }
		 * @param deviceID, deviceID
		 * @param srv, the default srv
		 * @param key, the key in params, and use it as the id ('kit-' + key)
		 * @param onoff, optional, used to specify the value for on and off, default is [1, 0]
		 */
		function createOnOffButton(parentNode, pos, deviceID, srv, key, onoff) {
			if (onoff === undefined) {
				onoff = [1, 0];
			} else {
				if (!Array.isArray(onoff) && onoff.length != 2) {
					JSDK.debug.log('onoff should be [1, 0]');
					return null;
				}
			}
			var container = parentNode;
			if (container == null) {
				container = document.getElementById(IDS.content);
			}
			if (!container) {
				return null;
			}
			var id = 'kit-' + key;
			if (document.getElementById(id) != null) {
				JSDK.debug.log(id + ' exists!');
				return null;
			}

			var wrapper = createElementFromTemplate({
				tag: 'div', cls: 'kit-ctrl-container', children: [{
					tag: 'div', cls: 'kit-onoff', id: id, children: [{
						tag: 'div', cls: 'kit-onoff-thumb'
					}]
				}]
			});

			placeElement(wrapper, pos);
			container.appendChild(wrapper);

			$(id).onclick = function() {
				var val = this.classList.contains('kit-on') ? onoff[1] : onoff[0];
				var cmd = JSDK.newCommand(deviceID, srv, 'set', [key], [val]);
				var p = JSDK.setDeviceStatus(JSDK.deviceID, null, cmd);
				if (p) {
					p.then(function() {
						popup.waiting.close();
					}).catch(function(msg){
						popup.waiting.close();
						popup.msgbox.show(msg);
					});;
				}
				popup.waiting.show();
			}
			JSDK.em.subscribe(['evtDeviceStatusChanged'], function(evt, param) {
				if (param.changed) {
					var c = param.changed[key];
					if (c) {
						var button = $(id);
						if (c.value == onoff[0]) {
							button.classList.add('kit-on');
						} else if (c.value == onoff[1]) {
							button.classList.remove('kit-on');
						}
					}
				}
			});

			return wrapper;
		}

		function createPlainView(parentNode, pos, deviceID, srv, key, type, strings) {
			var container = parentNode ? parentNode : document.getElementById(IDS.content);
			if (container == null) {
				return null;
			}
			var id = 'kit-' + key;
			if (document.getElementById(id) != null) {
				JSDK.debug.log(id + ' exists!');
				return null;
			}

			var wrapper = createElementFromTemplate({
				tag: 'div', cls: 'kit-ctrl-container', children: [{
					tag: 'div', cls: 'kit-plain-view', id: id, children: [{
						tag: 'div', cls: 'kit-plain-keyname', innerHTML: strings.name
					}, {
						tag: 'div', cls: 'kit-plain-value'
					}]
				}]
			});

			container.appendChild(wrapper);
			placeElement(wrapper, pos);

			JSDK.em.subscribe(['evtDeviceStatusChanged'], function(evt, param) {
				if (param.changed) {
					var c = param.changed[key];
					if (c) {
						var value = wrapper.getElementsByClassName('kit-plain-value')[0];
						$(id).rawValue = c.value;
						if (type == 1) {
							value.innerHTML = strings.values[c.value];
						} else if (type == 2) {
							if(strings.unit)
								value.innerHTML = c.value + strings.unit;
							else
								value.innerHTML = c.value;
						} else {
							value.innerHTML = c.value;
						}
					}
				}
			});

			return wrapper;
		}

		function createEnum(parentNode, pos, deviceID, srv, key, values, strings) {
			if (values[0] !== 1) {
				return null;
			}
			var wrapper = createPlainView(parentNode, pos, deviceID, srv, key, 1, strings);
			var id = 'kit-' + key;
			var valueElem = $(id).getElementsByClassName('kit-plain-value')[0];
			valueElem.classList.add('kit-clickable');
			valueElem.onclick = function() {
				var popup = $(IDS.popup);

				function onClickLi() {
					UIKIT.popup.menu.close();
					if ($(id).rawValue === this.rawValue) {
						return;
					}

					var cmd = JSDK.newCommand(deviceID, srv, 'set', [key], [this.rawValue]);
					var p = JSDK.setDeviceStatus(JSDK.deviceID, null, cmd);
					if (p) {
						p.then(function() {
							UIKIT.popup.waiting.close();
						}).catch(function(msg){
							UIKIT.popup.waiting.close();
							UIKIT.popup.msgbox.show(msg);
						});;
					}
					UIKIT.popup.waiting.show();
				}

				var tpl = { tag: 'ul', cls: 'kit-popup-menu', children: [] };
				var currValue = $(id).rawValue;
				for (var i = 1; i < values.length; ++ i) {
					var text = strings.values[values[i]];
					tpl.children.push({
						tag: 'li', innerHTML: text,
						cls: values[i] === currValue ? 'curr' : undefined,
						rawValue: values[i], onclick: onClickLi
					});
				}
				var menu = createElementFromTemplate(tpl);
				UIKIT.popup.menu.popup(menu);
			}

			return wrapper;
		}

		function createRange(parentNode, pos, deviceID, srv, key, values, strings) {
			if (values[0] != 2 || values.length != 5) {
				return null;
			}
			var min = values[1];
			var max = values[2];
			var step = values[3];
			var scale = values[4];

			var wrapper = createPlainView(parentNode, pos, deviceID, srv, key, 2, strings);
			var id = 'kit-' + key;
			var valueElem = $(id).getElementsByClassName('kit-plain-value')[0];
			valueElem.classList.add('kit-clickable');
			valueElem.onclick = function() {
				var touchevents = ['touchstart', 'touchend', 'touchcancel', 'touchleave', 'touchmove'];
				var popup = $(IDS.popup);
				var panel = createElementFromTemplate({
					tag: 'div', cls: 'kit-slider-panel', children: [{
						tag: 'div', cls: 'kit-slider-value', innerHTML: getDisplayValue($(id).rawValue)
					}, {
						tag: 'div', cls: 'kit-slider-container', children: [{
							tag: 'div', cls: 'kit-slider-bar'
						}, {
							tag: 'div', cls: 'kit-slider-thumb', draggable: "true"
						}, {
							tag: 'div', cls: 'kit-slider-start',innerHTML:getDisplayValue(values[1]) 
						}, {
							tag: 'div', cls: 'kit-slider-end',innerHTML:getDisplayValue(values[2])
						}]
					}]
				});
				UIKIT.popup.menu.popup(panel);
				JSDK.utils.later(0).then(function() {
					setThumbPos($(id).rawValue);
				});

				var sliderValue = panel.getElementsByClassName('kit-slider-value')[0];
				var bar = panel.getElementsByClassName('kit-slider-bar')[0];
				var thumb = panel.getElementsByClassName('kit-slider-thumb')[0];

				function getDisplayValue(value) {
					var temp = '';
					if(strings.unit)
						temp = value / scale + strings.unit;
					else
					 	temp = value / scale;
					return temp;
				}

				function setThumbPos(value) {
					var left = (bar.scrollWidth - thumb.scrollWidth) * (value - min) / (max - min);
					left += bar.offsetLeft;
					thumb.style.left = left + 'px';
				}

				function getValueFromX(x) {
					var offset = x - bar.offsetLeft;
					var len = bar.scrollWidth - thumb.scrollWidth;
					var v = (max - min) * offset / len;
					v = Math.floor(v / step + 0.5) * step;
					v += min;
					if (v < min) {
						v = min;
					}
					if (v > max) {
						v = max;
					}
					return v;
				}

				var inDragging = false;
				var offsetX = 0;
				touchevents.forEach(function(evt) {
					thumb.addEventListener(evt, onTouch, false);
				});
				function onTouch(evt) {
					var touches = evt.changedTouches;
					var touch = touches[touches.length - 1];
					var x = touch.pageX;
					var y = touch.pageY;
					var v = 0;
					switch (evt.type) {
					case 'touchstart':
						if (inDragging) {
							return;
						}
						inDragging = true;
						offsetX = x - thumb.offsetLeft;
						break;
					case 'touchmove':
						v = getValueFromX(x - offsetX);
						sliderValue.innerHTML = getDisplayValue(v);
						setThumbPos(v);
						break;
					case 'touchend':
					case 'touchleave':
						v = getValueFromX(x - offsetX); {
							UIKIT.popup.menu.close();
							if ($(id).rawValue === v) {
								return;
							}

							var cmd = JSDK.newCommand(deviceID, srv, 'set', [key], [v]);
							var p = JSDK.setDeviceStatus(JSDK.deviceID, null, cmd);
							if (p) {
								p.then(function() {
									UIKIT.popup.waiting.close();
								}).catch(function(msg){
									UIKIT.popup.waiting.close();
									UIKIT.popup.msgbox.show(msg);
								});;
							}
							UIKIT.popup.waiting.show();
						}
						inDragging = false;
						break;
					case 'touchcancel':
						inDragging = false;
						break;
					}

					if (inDragging) {
						evt.preventDefault();
					}
				}
			}

			return wrapper;
		}


		return {
			createOnOffButton: createOnOffButton,
			createEnum: createEnum,
			createRange: createRange,
			createPlainView: createPlainView
		};
	})();


	/**
	 * tpl: {
	 *	tag: 'div',
	 *	id: 'kit-xx',
	 *	cls: 'kit-xx kit-yy',
	 *	innerHTML: 'anything',
	 *	style: {
	 *		display: 'none',
	 *		...
	 *	},
	 *	children: [ tpl, ...]
	 * }
	 */
	function createElementFromTemplate(tpl) {
		var elem = document.createElement(tpl.tag);
		if (elem == null) {
			return null;
		}
		for (var k in tpl) {
			if (k === 'cls') {
				elem.className = tpl[k];
			} else if (k === 'style') {
				for (var s in tpl.style) {
					elem.style[s] = tpl.style[s];
				}
			} else if (k === 'children') {
				if (!Array.isArray(tpl.children)) {
					continue;
				}
				var children = tpl.children;
				for (var i = 0; i < children.length; ++ i) {
					var child = createElementFromTemplate(children[i]);
					if (child) {
						elem.appendChild(child);
					}
				}
			} else if (k !== 'tag') {
				elem[k] = tpl[k];
			}
		}
		return elem;
	}

	function placeElement(elem, pos) {
		var st = elem.style;

		var sx = gridGetX(pos.sx);
		var sy = gridGetY(pos.sy);
		var width = gridGetX(pos.ex) - sx;
		var height = gridGetY(pos.ey) - sy;
		st.width = width + 'px';
		st.height = height + 'px';
		st.left = sx + 'px';
		st.top = sy + 'px';
	}

	function leftElement(elem, pos) {
		var st = elem.style;

		var sx = gridGetX(pos.sx);
		var sy = gridGetY(pos.sy);
		var height = gridGetY(pos.ey) - sy;
		st.left = sx + 'px';
		st.top = sy + (height - elem.scrollHeight) / 2 + 'px';
	}

	document.addEventListener('DOMContentLoaded', onLoad, false);
	return {
		// popup layer
		popup: popup,
		// controls
		controls: controls,

		// utils
		utils: {
			createElementFromTemplate: createElementFromTemplate,
			placeElement: placeElement,
			leftElement: leftElement
		}
	}
})();

