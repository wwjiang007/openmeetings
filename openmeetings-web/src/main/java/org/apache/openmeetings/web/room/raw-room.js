/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
var Room = (function() {
	const self = {}, sbSide = Settings.isRtl ? 'right' : 'left';
	let options, menuHeight, sb, dock, activities;

	function _init(_options) {
		options = _options;
		window.WbArea = options.interview ? InterviewWbArea() : DrawWbArea();
		const menu = $('.room-block .container .menu');
		activities = $('#activities');
		sb = $('.room-block .sidebar');
		_sbAddResizable();
		dock = sb.find('.btn-dock').button({
			icon: "ui-icon icon-undock"
			, showLabel: false
		}).click(function() {
			const offset = parseInt(sb.css(sbSide));
			if (offset < 0) {
				sb.removeClass('closed');
			}
			dock.button('option', 'disabled', true);
			const props = {};
			props[sbSide] = offset < 0 ? '0px' : (-sb.width() + 45) + 'px';
			sb.animate(props, 1500
				, function() {
					dock.button('option', 'disabled', false)
						.button('option', 'icon', 'ui-icon ' + (offset < 0 ? 'icon-undock' : 'icon-dock'));
					if (offset < 0) {
						dock.attr('title', dock.data('ttl-undock'));
						_sbAddResizable();
					} else {
						dock.attr('title', dock.data('ttl-dock'));
						sb.addClass('closed').resizable('destroy');
					}
					_setSize();
				});
		});
		dock.addClass(Settings.isRtl ? 'align-left' : 'align-right').attr('title', dock.data('ttl-undock'))
			.button('option', 'label', dock.data('ttl-undock'))
			.button('refresh');
		menuHeight = menu.length === 0 ? 0 : menu.height();
		VideoManager.init();
		if (typeof(Activities) !== 'undefined') {
			Activities.init();
		}
		Sharer.init();
		_setSize();
	}
	function _getSelfAudioClient() {
		const vw = $('#video' + Room.getOptions().uid);
		if (vw.length > 0) {
			const v = vw.data();
			if (VideoUtil.hasAudio(v.stream())) {
				return v;
			}
		}
		return null;
	}
	function _preventKeydown(e) {
		const base = $(e.target);
		if (e.target.isContentEditable === true || base.is('textarea, input:not([readonly]):not([type=radio]):not([type=checkbox])')) {
			return;
		}
		if (e.which === 8) { // backspace
			e.preventDefault();
			e.stopImmediatePropagation();
			return false;
		}
	}
	function __keyPressed(hotkey, e) {
		const code = OmUtil.getKeyCode(e);
		return hotkey.alt === e.altKey
			&& hotkey.ctrl === e.ctrlKey
			&& hotkey.shift === e.shiftKey
			&& hotkey.code.toUpperCase() === (code ? code.toUpperCase() : '');
	}
	function _keyHandler(e) {
		if (__keyPressed(options.keycode.arrange, e)) {
			VideoUtil.arrange();
		} else if (__keyPressed(options.keycode.muteothers, e)) {
			const v = _getSelfAudioClient();
			if (v !== null) {
				VideoManager.clickMuteOthers(Room.getOptions().uid);
			}
		} else if (__keyPressed(options.keycode.mute, e)) {
			const v = _getSelfAudioClient();
			if (v !== null) {
				v.mute(!v.isMuted());
			}
		} else if (__keyPressed(options.keycode.quickpoll, e)) {
			quickPollAction('open');
		}
		if (e.which === 27) {
			$('#wb-rename-menu').hide();
		}
	}
	function _mouseHandler(e) {
		if (e.which === 1) {
			$('#wb-rename-menu').hide();
		}
	}
	function _sbWidth() {
		if (sb === undefined) {
			sb = $('.room-block .sidebar');
		}
		return sb === undefined ? 0 : sb.width() + parseInt(sb.css(sbSide));
	}
	function _setSize() {
		const sbW = _sbWidth()
			, holder = $('.room-block');
		($('.main.room')[0]).style.setProperty('--room-sidebar-width', sbW + 'px');
		if (sbW > 285) {
			holder.addClass('big').removeClass('small');
		} else {
			holder.removeClass('big').addClass('small');
		}
	}
	function _reload() {
		if (!!options && !!options.reloadUrl) {
			window.location.href = options.reloadUrl;
		} else {
			window.location.reload();
		}
	}
	function _close() {
		_unload();
		$(".room-block").remove();
		$("#chatPanel").remove();
		const dlg = $('#disconnected-dlg');
		dlg.dialog({
			modal: true
			, close: _reload
			, buttons: [
				{
					text: dlg.data('reload')
					, icons: {primary: "ui-icon-refresh"}
					, click: function() {
						$(this).dialog("close");
					}
				}
			]
		});
	}
	function _sbAddResizable() {
		sb.resizable({
			handles: Settings.isRtl ? 'w' : 'e'
			, stop: function() {
				_setSize();
			}
		});
	}
	function _load() {
		$('body').addClass('no-header');
		Wicket.Event.subscribe("/websocket/closed", _close);
		Wicket.Event.subscribe("/websocket/error", _close);
		$(window).on('keydown.openmeetings', _preventKeydown);
		$(window).on('keyup.openmeetings', _keyHandler);
		$(document).click(_mouseHandler);
	}
	function _unload() {
		$('body').removeClass('no-header');
		Wicket.Event.unsubscribe("/websocket/closed", _close);
		Wicket.Event.unsubscribe("/websocket/error", _close);
		if (typeof(WbArea) === 'object') {
			WbArea.destroy();
			window.WbArea = undefined;
		}
		if (typeof(VideoSettings) === 'object') {
			VideoSettings.close();
		}
		if (typeof(VideoManager) === 'object') {
			VideoManager.destroy();
		}
		const _qconf = $('#quick-confirmation');
		if (_qconf.dialog('instance')) {
			_qconf.dialog('destroy');
		}
		$('.ui-dialog.user-video').remove();
		$(window).off('keyup.openmeetings');
		$(window).off('keydown.openmeetings');
		$(document).off('click', _mouseHandler);
		sb = undefined;
		Sharer.close();
	}
	function _showClipboard(txt) {
		const dlg = $('#clipboard-dialog');
		dlg.find('p .text').text(txt);
		dlg.dialog({
			resizable: false
			, height: "auto"
			, width: 400
			, modal: true
			, buttons: [
				{
					text: dlg.data('btn-ok')
					, click: function() {
						$(this).dialog('close');
					}
				}
			]
		});
	}
	function _hasRight(_rights) {
		const rights = Array.isArray(_rights) ? _rights : [];
		for (let i = 0; i < rights.length; ++i) {
			if (options.rights.includes(rights[i])) {
				return true;
			}
		}
		return false;
	}
	function _setQuickPollRights() {
		const close = $('#quick-vote .close');
		if (close.length === 1) {
			close.off();
			if (_hasRight(['superModerator', 'moderator', 'presenter'])) {
				close.show().click(function() {
					const _qconf = $('#quick-confirmation');
					_qconf.dialog({
						resizable: false
						, height: "auto"
						, width: 400
						, modal: true
						, buttons: [
							{
								text: _qconf.data('btn-ok')
								, click: function() {
									quickPollAction('close');
									$(this).dialog('close');
								}
							}
							, {
								text: _qconf.data('btn-cancel')
								, click: function() {
									$(this).dialog('close');
								}
							}
						]
					});
				});
			} else {
				close.hide();
			}
		}
	}
	function _quickPoll(obj) {
		if (obj.started) {
			let qv = $('#quick-vote');
			if (qv.length === 0) {
				const wbArea = $('.room-block .wb-block');
				qv = OmUtil.tmpl('#quick-vote-template', 'quick-vote');
				wbArea.append(qv);
			}
			const pro = qv.find('.control.pro')
				, con = qv.find('.control.con');
			if (obj.voted) {
				pro.removeClass('clickable').off();
				con.removeClass('clickable').off();
			} else {
				pro.addClass('clickable').off().click(function() {
					quickPollAction('vote', true);
				});
				con.addClass('clickable').off().click(function() {
					quickPollAction('vote', false);
				});
			}
			pro.find('.badge').text(obj.pros);
			con.find('.badge').text(obj.cons);
			_setQuickPollRights();
		} else {
			const qv = $('#quick-vote');
			if (qv.length === 1) {
				qv.remove();
			}
		}
		OmUtil.tmpl('#quick-vote-template', 'quick-vote');
	}

	self.init = _init;
	self.getMenuHeight = function() { return menuHeight; };
	self.getOptions = function() { return typeof(options) === 'object' ? JSON.parse(JSON.stringify(options)) : {}; };
	self.setRights = function(_r) {
		options.rights = _r;
		_setQuickPollRights();
	};
	self.setActivities = function(_a) {
		options.activities = _a;
	};
	self.load = _load;
	self.unload = _unload;
	self.showClipboard = _showClipboard;
	self.quickPoll = _quickPoll;
	self.hasRight = _hasRight;
	self.setCssVar = function(key, val) {
		($('.main.room')[0]).style.setProperty(key, val);
	};
	return self;
})();
function startPrivateChat(el) {
	Chat.addTab('chatTab-u' + el.parent().parent().data("userid"), el.parent().parent().find('.user.name').text());
	Chat.open();
	$('#chatMessage .wysiwyg-editor').click();
}
/***** functions required by SIP   ******/
function sipBtnClick() {
	const txt = $('.sip-number');
	txt.val(txt.val() + $(this).data('value'));
}
function sipBtnEraseClick() {
	const txt = $('.sip-number')
		, t = txt.val();
	if (!!t) {
		txt.val(t.substring(0, t.length - 1));
	}
}
function sipGetKey(evt) {
	let k = -1;
	if (evt.keyCode > 47 && evt.keyCode < 58) {
		k = evt.keyCode - 48;
	}
	if (evt.keyCode > 95 && evt.keyCode < 106) {
		k = evt.keyCode - 96;
	}
	return k;
}
function sipKeyDown(evt) {
	const k = sipGetKey(evt);
	if (k > 0) {
		$('#sip-dialer-btn-' + k).addClass('ui-state-active');
	}
}
function sipKeyUp(evt) {
	const k = sipGetKey(evt);
	if (k > 0) {
		$('#sip-dialer-btn-' + k).removeClass('ui-state-active');
	}
}
function typingActivity(uid, active) {
	const u = $('#user' + uid + ' .typing-activity.ui-icon');
	if (active) {
		u.addClass("typing");
	} else {
		u.removeClass("typing");
	}
}
