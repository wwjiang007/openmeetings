/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
var PRESENTER = 'presenter';
var WHITEBOARD = 'whiteBoard';
var DrawWbArea = function() {
	const self = BaseWbArea()
		, arrowImg = new Image(), delImg = new Image();
	arrowImg.src = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABwAAAAICAYAAADqSp8ZAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAygAAAMoBawMUsgAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAFsSURBVCiRrdI/SEJRFMfx37lPGxqKoGwxKJoaImhpCf8NEUFL9WgLUrPnIyEIa6reVEPQn0GeWDS4NDQETQ2JT4waojUoHBqCoJKWINB3720yIhGl+q7ncj5nuIQ6jWiaq1xmU4IwBACQ5GCAU5D8IECRAkUQzt8V++wmlSrX20e1BoFIrFdwHidIIQhH5O68sgzD/vnOF4m0QyijJGgMQIHZtJdJJ4oNg6qqNr20dKwBaOWKvZFPpZ7qXV3JH4wNSMbjJHGZ7XIlYRiiFkiBsL4CphwLwbck5E7uwMw3ClXD2iRImYYUq9lD886nLXZbyd2HL9AbXpglySOQeFVstpRJJ+5/i1UajkbbHCXahMS1ZAiS2+W1DMNmqqoqBLFMYIME1uxkvPRXDAAuTPMNhCwIGiT62eOzAQDkD+nbAjQDxudy+8mT/8C+FwjNjwuwdQnqY7b0kCesT7DC7allWVU/8D/zh3SdC/R8Aq9QhRc3h8LfAAAAAElFTkSuQmCC';
	delImg.src = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAMAAAGgrv1cAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAADNQTFRFAAAA4j094j094j094j094j094j094j094j094j094j094j094j094j094j094j094j09hIdAxgAAABB0Uk5TABAgMEBQYHCAj5+vv8/f7yMagooAAADXSURBVBgZBcEBYoQgDACw1DJETmz//9olwGn6AAAbBxoiSACTpCTtJd02smg+MPoef7UgnpPQeVM42Vg02kl+qAPeE2B19wYAgO83xi6ggRMoBfuvsUSxp+vPjag98VqwC8oI9ozC5rMnUVbw5ITID94Fo4D4umsAwN/+urvfOwDg6d8FiFUnALPnkwCs6zvg+UKcSmD3ZBWyL4hTye4J3s16AXG6J+D+uD/A7vtUAutFT9g9EacSURNX33ZPQJzKqAW8lQCIXyWAVfUM5Hz7vQAAMcZIAP9DvgiOL2K6DwAAAABJRU5ErkJggg==';
	let container, area, tabs, scroll, role = NONE, _inited = false;

	// Fabric overrides (should be kept up-to-date on fabric.js updates)
	if ('function' !== typeof(window.originalDrawControl)) {
		window.originalDrawControl = fabric.Object.prototype._drawControl;
		window.originalGetRotatedCornerCursor = fabric.Canvas.prototype._getRotatedCornerCursor;
		window.originalGetActionFromCorner = fabric.Canvas.prototype._getActionFromCorner;
		window.originalGetCornerCursor = fabric.Canvas.prototype.getCornerCursor;
		fabric.Object.prototype._drawControl = function(control, ctx, methodName, left, top, styleOverride) {
			switch (control) {
				case 'mtr':
				{
					const x = left + (this.cornerSize - arrowImg.width) / 2
						, y = top + (this.cornerSize - arrowImg.height) / 2;
					ctx.drawImage(arrowImg, x, y);
				}
					break;
				case 'tr':
				{
					if (role === PRESENTER) {
						const x = left + (this.cornerSize - delImg.width) / 2
							, y = top + (this.cornerSize - delImg.height) / 2;
						ctx.drawImage(delImg, x, y);
					} else {
						window.originalDrawControl.call(this, control, ctx, methodName, left, top, styleOverride);
					}
				}
					break;
				default:
					window.originalDrawControl.call(this, control, ctx, methodName, left, top, styleOverride);
					break;
			}
		};
		fabric.Canvas.prototype._getRotatedCornerCursor = function(corner, target, e) {
			if (role === PRESENTER && 'tr' === corner) {
				return 'pointer';
			}
			return window.originalGetRotatedCornerCursor.call(this, corner, target, e);
		};
		fabric.Canvas.prototype._getActionFromCorner = function(alreadySelected, corner, e) {
			if (role === PRESENTER && 'tr' === corner) {
				_performDelete();
				return 'none';
			}
			return window.originalGetActionFromCorner.call(this, alreadySelected, corner, e);
		};
		fabric.Canvas.prototype.getCornerCursor = function(corner, target, e) {
			if (role === PRESENTER && 'tr' === corner) {
				return 'pointer';
			}
			return window.originalGetCornerCursor.call(this, corner, target, e);
		}
	}

	function refreshTabs() {
		tabs.tabs('refresh').find('ul').removeClass('ui-corner-all').removeClass('ui-widget-header');
	}
	function getActive() {
		const idx = tabs.tabs('option', 'active');
		if (idx > -1) {
			const href = tabs.find('a')[idx];
			if (!!href) {
				return $($(href).attr('href'));
			}
		}
		return null;
	}
	function _performDelete() {
		const wb = getActive().data()
			, canvas = wb.getCanvas();
		if (role !== PRESENTER || !canvas) {
			return true;
		}
		const arr = [], objs = canvas.getActiveObjects();
		for (let i = 0; i < objs.length; ++i) {
			arr.push({
				uid: objs[i].uid
				, slide: objs[i].slide
			});
		}
		OmUtil.wbAction({action: 'deleteObj', data: {
			wbId: wb.id
			, obj: arr
		}});
		return false;
	}
	function _deleteHandler(e) {
		if ('BODY' === e.target.tagName) {
			switch (e.which) {
				case 8:  // backspace
				case 46: // delete
					e.preventDefault();
					e.stopImmediatePropagation();
					return _performDelete();
				default:
					//no-op
			}
		}
	}
	function _getWbTab(wbId) {
		return tabs.find('li[data-wb-id="' + wbId + '"]');
	}
	function _activateTab(wbId) {
		container.find('.wb-tabbar li').each(function(idx) {
			if (wbId === 1 * $(this).data('wb-id')) {
				tabs.tabs('option', 'active', idx);
				$(this)[0].scrollIntoView();
				return false;
			}
		});
	}
	function _setTabName(li, name) {
		return li.find('a').attr('title', name)
			.find('span').text(name)
	}
	function _renameTab(obj) {
		_setTabName(_getWbTab(obj.wbId), obj.name);
	}
	function _addCloseBtn(li) {
		if (role !== PRESENTER) {
			return;
		}
		li.append(OmUtil.tmpl('#wb-tab-close'));
		li.find('button').click(function() {
			OmUtil.confirmDlg('wb-confirm-remove', function() { OmUtil.wbAction({action: 'removeWb', data: {wbId: li.data().wbId}}); });
		});
	}
	function _getImage(cnv) {
		return cnv.toDataURL({
			format: 'image/png'
			, width: cnv.width
			, height: cnv.height
			, multiplier: 1. / cnv.getZoom()
			, left: 0
			, top: 0
		});
	}
	function _videoStatus(json) {
		self.getWb(json.wbId).videoStatus(json);
	}
	function _initVideos(arr) {
		for (let i = 0; i < arr.length; ++i) {
			 _videoStatus(arr[i]);
		}
	}

	self.getWbTabId = function(id) {
		return 'wb-tab-' + id;
	};
	self.getWb = function(id) {
		return $('#' + self.getWbTabId(id)).data();
	};
	self.getCanvas = function(id) {
		return self.getWb(id).getCanvas();
	};
	self.setRole = function(_role) {
		if (!_inited) {
			return;
		}
		role = _role;
		const tabsNav = tabs.find('.ui-tabs-nav');
		tabsNav.sortable(role === PRESENTER ? 'enable' : 'disable');
		const prev = tabs.find('.prev.om-icon'), next = tabs.find('.next.om-icon');
		if (role === PRESENTER) {
			if (prev.length === 0) {
				const cc = tabs.find('.wb-tabbar .scroll-container')
					, left = OmUtil.tmpl('#wb-tabbar-ctrls-left')
					, right = OmUtil.tmpl('#wb-tabbar-ctrls-right');
				cc.before(left).after(right);
				tabs.find('.add.om-icon').click(function() {
					OmUtil.wbAction({action: 'createWb'});
				});
				tabs.find('.prev.om-icon').click(function() {
					scroll.scrollLeft(scroll.scrollLeft() - 30);
				});
				tabs.find('.next.om-icon').click(function() {
					scroll.scrollLeft(scroll.scrollLeft() + 30);
				});
				tabsNav.find('li').each(function() {
					const li = $(this);
					_addCloseBtn(li);
				});
				self.addDeleteHandler();
			}
		} else {
			if (prev.length > 0) {
				prev.parent().remove();
				next.parent().remove();
				tabsNav.find('li button').remove();
			}
			self.removeDeleteHandler();
		}
		tabs.find('.ui-tabs-panel').each(function() {
			$(this).data().setRole(role);
		});
	}
	function _actionActivateWb(_wbId) {
		OmUtil.wbAction({action: 'activateWb', data: {wbId: _wbId}});
	}
	self.init = function() {
		Wicket.Event.subscribe('/websocket/message', self.wbWsHandler);
		container = $('.room-block .wb-block');
		tabs = container.find('.tabs');
		if (tabs.length === 0) {
			return;
		}
		tabs.tabs({
			beforeActivate: function(e) {
				let res = true;
				if (e.originalEvent && e.originalEvent.type === 'click') {
					res = role === PRESENTER;
				}
				return res;
			}
			, activate: function(e, ui) {
				//only send `activateWb` event if activation was initiated by user
				if (e.originalEvent && e.originalEvent.type === 'click') {
					_actionActivateWb(ui.newTab.data('wb-id'));
				}
			}
		});
		scroll = tabs.find('.scroll-container');
		area = container.find('.wb-area');
		tabs.find('.ui-tabs-nav').sortable({
			axis: 'x'
			, stop: function() {
				refreshTabs();
			}
		});
		_inited = true;
		self.setRole(role);
		$('#wb-rename-menu').menu().find('.wb-rename').click(function() {
			_getWbTab($(this).parent().data('wb-id')).find('a span').trigger('dblclick');
		});
	};
	self.destroy = function() {
		self.removeDeleteHandler();
		Wicket.Event.unsubscribe('/websocket/message', self.wbWsHandler);
	};
	self.create = function(obj) {
		if (!_inited) {
			return;
		}
		const tid = self.getWbTabId(obj.wbId)
			, wb = OmUtil.tmpl('#wb-area', tid)
			, li = OmUtil.tmpl('#wb-area-tab').data('wb-id', obj.wbId).attr('data-wb-id', obj.wbId)
				.contextmenu(function(e) {
					if (role !== PRESENTER) {
						return;
					}
					e.preventDefault();
					$('#wb-rename-menu').show().data('wb-id', obj.wbId)
						.position({my: 'left top', collision: 'none', of: _getWbTab(obj.wbId)});
				});
		li.find('a').attr('href', '#' + tid);
		_setTabName(li, obj.name)
			.dblclick(function() {
				if (role !== PRESENTER) {
					return;
				}
				const editor = $('<input name="newName" type="text" style="color: black;"/>')
					, name = $(this).hide().after(editor.val(obj.name))
					, renameWbTab = function() {
						const newName = editor.val();
						if (newName !== '') {
							OmUtil.wbAction({action: 'renameWb', data: {wbId: obj.wbId, name: newName}});
						}
						editor.remove();
						name.show();
					};
				editor.focus()
					.blur(renameWbTab)
					.keyup(function(evt) {
						if (evt.which === 13) {
							renameWbTab();
						}
					});
			});

		tabs.find('.ui-tabs-nav').append(li);
		tabs.append(wb);
		refreshTabs();
		_addCloseBtn(li);

		const wbo = Wb();
		wbo.init(obj, tid, role);
		wb.on('remove', wbo.destroy);
		wb.data(wbo);
	}
	self.createWb = function(obj) {
		if (!_inited) {
			return;
		}
		self.create(obj);
		_activateTab(obj.wbId);
		_actionActivateWb(obj.wbId);
	};
	self.activateWb = function(obj) {
		if (!_inited) {
			return;
		}
		_activateTab(obj.wbId);
	}
	self.renameWb = function(obj) {
		if (!_inited) {
			return;
		}
		_renameTab(obj);
	}
	self.removeWb = function(obj) {
		if (!_inited) {
			return;
		}
		const tabId = self.getWbTabId(obj.wbId);
		_getWbTab(obj.wbId).remove();
		$('#' + tabId).remove();
		refreshTabs();
		_actionActivateWb(getActive().data().id);
	};
	self.load = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).load(json.obj);
	};
	self.setSlide = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).setSlide(json.slide);
	};
	self.createObj = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).createObj(json.obj);
	};
	self.modifyObj = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).modifyObj(json.obj);
	};
	self.deleteObj = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).removeObj(json.obj);
	};
	self.clearAll = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).clearAll();
	};
	self.clearSlide = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).clearSlide(json.slide);
	};
	self.setSize = function(json) {
		if (!_inited) {
			return;
		}
		self.getWb(json.wbId).setSize(json);
	}
	self.download = function(fmt) {
		if (!_inited) {
			return;
		}
		const wb = getActive().data();
		if ('pdf' === fmt) {
			const arr = [];
			wb.eachCanvas(function(cnv) {
				arr.push(_getImage(cnv));
			});
			OmUtil.wbAction({action: 'downloadPdf', data: {
				slides: arr
			}});
		} else {
			const cnv = wb.getCanvas()
				, dataUri = _getImage(cnv);
			try {
				const dlg = $('#download-png');
				dlg.find('img').attr('src', dataUri);
				dlg.dialog({
					width: 350
					, appendTo: '.room-block .wb-block'
				});
			} catch (e) {
				console.error(e);
			}
		}
	}
	self.videoStatus = _videoStatus;
	self.loadVideos = function() {
		if (!_inited) {
			return;
		}
		OmUtil.wbAction({action: 'loadVideos'});
	};
	self.initVideos = _initVideos;
	self.addDeleteHandler = function() {
		if (role === PRESENTER) {
			$(window).keyup(_deleteHandler);
		}
	};
	self.removeDeleteHandler = function() {
		$(window).off('keyup', _deleteHandler);
	};
	self.updateAreaClass = function() {};
	self.doCleanAll = function() {
		if (!_inited) {
			return;
		}
		tabs.find('li').each(function() {
			const wbId = $(this).data('wb-id')
				, tabId = self.getWbTabId(wbId);
			$(this).remove();
			$('#' + tabId).remove();
		});
		refreshTabs();
	};
	return self;
};
