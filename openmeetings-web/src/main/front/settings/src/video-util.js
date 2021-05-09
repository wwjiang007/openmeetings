/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
const WB_AREA_SEL = '.room-block .wb-block';
const WBA_WB_SEL = '.room-block .wb-block .wb-tab-content';
const VIDWIN_SEL = '.video.user-video';
const VID_SEL = '.video-container[id!=user-video]';
const CAM_ACTIVITY = 'VIDEO';
const MIC_ACTIVITY = 'AUDIO';
const SCREEN_ACTIVITY = 'SCREEN';
const REC_ACTIVITY = 'RECORD';

function _getVid(uid) {
	return 'video' + uid;
}
function _isSharing(sd) {
	return !!sd && 'SCREEN' === sd.type && sd.activities.includes(SCREEN_ACTIVITY);
}
function _isRecording(sd) {
	return !!sd && 'SCREEN' === sd.type && sd.activities.includes(REC_ACTIVITY);
}
function _hasMic(sd) {
	return !sd || sd.activities.includes(MIC_ACTIVITY);
}
function _hasCam(sd) {
	return !sd || sd.activities.includes(CAM_ACTIVITY);
}
function _hasVideo(sd) {
	return _hasCam(sd) || _isSharing(sd) || _isRecording(sd);
}
function _getRects(sel, excl) {
	const list = [], elems = $(sel);
	for (let i = 0; i < elems.length; ++i) {
		if (excl !== $(elems[i]).attr('aria-describedby')) {
			list.push(_getRect(elems[i]));
		}
	}
	return list;
}
function _getRect(e) {
	const win = $(e), winoff = win.offset();
	return {left: winoff.left
		, top: winoff.top
		, right: winoff.left + win.width()
		, bottom: winoff.top + win.height()};
}
function _container() {
	const a = $(WB_AREA_SEL);
	const c = a.find('.wb-area .tabs .wb-tab-content');
	return c.length > 0 ? $(WBA_WB_SEL) : a;
}
function __processTopToBottom(area, rectNew, list) {
	const offsetX = 20
		, offsetY = 10;

	let minY = area.bottom, posFound;
	do {
		posFound = true;
		for (let i = 0; i < list.length; ++i) {
			const rect = list[i];
			minY = Math.min(minY, rect.bottom);

			if (rectNew.left < rect.right && rectNew.right > rect.left && rectNew.top < rect.bottom && rectNew.bottom > rect.top) {
				rectNew.left = rect.right + offsetX;
				posFound = false;
			}
			if (rectNew.right >= area.right) {
				rectNew.left = area.left;
				rectNew.top = Math.max(minY, rectNew.top) + offsetY;
				posFound = false;
			}
			if (rectNew.bottom >= area.bottom) {
				rectNew.top = area.top;
				posFound = true;
				break;
			}
		}
	} while (!posFound);
	return {left: rectNew.left, top: rectNew.top};
}
function __processEqualsBottomToTop(area, rectNew, list) {
	const offsetX = 20
		, offsetY = 10;

	rectNew.bottom = area.bottom;
	let minY = area.bottom, posFound;
	do {
		posFound = true;
		for (let i = 0; i < list.length; ++i) {
			const rect = list[i];
			minY = Math.min(minY, rect.top);

			if (rectNew.left < rect.right && rectNew.right > rect.left && rectNew.top < rect.bottom && rectNew.bottom > rect.top) {
				rectNew.left = rect.right + offsetX;
				posFound = false;
			}
			if (rectNew.right >= area.right) {
				rectNew.left = area.left;
				rectNew.bottom = Math.min(minY, rectNew.top) - offsetY;
				posFound = false;
			}
			if (rectNew.top <= area.top) {
				rectNew.top = area.top;
				posFound = true;
				break;
			}
		}
	} while (!posFound);
	return {left: rectNew.left, top: rectNew.top};
}
function _getPos(list, w, h, _processor) {
	if (Room.getOptions().interview) {
		return {left: 0, top: 0};
	}
	const wba = _container()
		, woffset = wba.offset()
		, area = {left: woffset.left, top: woffset.top, right: woffset.left + wba.width(), bottom: woffset.top + wba.height()}
		, rectNew = {
			_left: area.left
			, _top: area.top
			, _right: area.left + w
			, _bottom: area.top + h
			, get left() {
				return this._left;
			}
			, set left(l) {
				this._left = l;
				this._right = l + w;
			}
			, get right() {
				return this._right;
			}
			, get top() {
				return this._top;
			}
			, set top(t) {
				this._top = t;
				this._bottom = t + h;
			}
			, set bottom(b) {
				this._bottom = b;
				this._top = b - h;
			}
			, get bottom() {
				return this._bottom;
			}
		};
	const processor = _processor || __processTopToBottom;
	return processor(area, rectNew, list);
}
function _arrange() {
	const list = [];
	$(VIDWIN_SEL).each(function() {
		const v = $(this);
		v.css(_getPos(list, v.width(), v.height()));
		list.push(_getRect(v));
	});
}
function _arrangeResize() {
	const list = []
		, s = VideoSettings.load()
		, size = {width: 120, height: 90};
	if (s.fixed.enabled) {
		size.width = s.fixed.width;
		size.height = s.fixed.height;
	}

	function __getDialog(_v) {
		return $(_v).find('.video-container.ui-dialog-content');
	}
	$(VIDWIN_SEL).toArray().sort((v1, v2) => {
		const c1 = __getDialog(v1).data().stream()
			, c2 = __getDialog(v2).data().stream();
		return c2.level - c1.level || c1.user.displayName.localeCompare(c2.user.displayName);
	}).forEach(_v => {
		const v = $(_v);
		__getDialog(v)
			.dialog('option', 'width', size.width)
			.dialog('option', 'height', size.height);
		v.css(_getPos(list, v.width(), v.height(), __processEqualsBottomToTop));
		list.push(_getRect(v));
	});
}
function _cleanStream(stream) {
	if (!!stream) {
		stream.getTracks().forEach(track => track.stop());
	}
}
function _cleanPeer(peer) {
	if (!!peer) {
		peer.cleaned = true;
		try {
			const pc = peer.peerConnection;
			if (!!pc) {
				pc.getSenders().forEach(sender => {
					try {
						if (sender.track) {
							sender.track.stop();
						}
					} catch(e) {
						OmUtil.log('Failed to clean sender' + e);
					}
				});
				pc.getReceivers().forEach(receiver => {
					try {
						if (receiver.track) {
							receiver.track.stop();
						}
					} catch(e) {
						OmUtil.log('Failed to clean receiver' + e);
					}
				});
				pc.onconnectionstatechange = null;
				pc.ontrack = null;
				pc.onremovetrack = null;
				pc.onremovestream = null;
				pc.onicecandidate = null;
				pc.oniceconnectionstatechange = null;
				pc.onsignalingstatechange = null;
				pc.onicegatheringstatechange = null;
				pc.onnegotiationneeded = null;
			}
			peer.dispose();
			peer.removeAllListeners('icecandidate');
			delete peer.generateOffer;
			delete peer.processAnswer;
			delete peer.processOffer;
			delete peer.addIceCandidate;
		} catch(e) {
			//no-op
		}
	}
}
function _setPos(v, pos) {
	if (v.dialog('instance')) {
		v.dialog('widget').css(pos);
	}
}
function _askPermission(callback) {
	const perm = $('#ask-permission');
	if (undefined === perm.dialog('instance')) {
		perm.data('callbacks', []).dialog({
			appendTo: '.room-block .room-container'
			, dialogClass: "ask-video-play-permission"
			, autoOpen: true
			, buttons: [
				{
					text: perm.data('btn-ok')
					, click: function() {
						while (perm.data('callbacks').length > 0) {
							perm.data('callbacks').pop()();
						}
						$(this).dialog('close');
					}
				}
			]
		});
	} else if (!perm.dialog('isOpen')) {
		perm.dialog('open')
	}
	perm.data('callbacks').push(callback);
}
function _disconnect(node) {
	try {
		node.disconnect(); //this one can throw
	} catch (e) {
		//no-op
	}
}
function _sharingSupported() {
	const b = OmUtil.browser;
	return (b.name === 'Edge' && b.major > 16)
		|| b.name === 'Firefox'
		|| b.name === 'Opera'
		|| b.name === 'Yandex'
		|| (OmUtil.isSafari() && typeof(navigator.mediaDevices.getDisplayMedia) === 'function')
		|| OmUtil.isChrome()
		|| OmUtil.isEdgeChromium()
		|| (b.name === 'Mozilla' && b.major > 4);
}
function _highlight(el, clazz, count) {
	if (!el || el.length < 1 || el.hasClass('disabled') || count < 0) {
		return;
	}
	el.addClass(clazz).delay(2000).queue(function(next) {
		el.removeClass(clazz).delay(2000).queue(function(next1) {
			_highlight(el, clazz, --count);
			next1();
		});
		next();
	});
}

module.exports = {
	VIDWIN_SEL: VIDWIN_SEL
	, VID_SEL: VID_SEL

	, getVid: _getVid
	, isSharing: _isSharing
	, isRecording: _isRecording
	, hasMic: _hasMic
	, hasCam: _hasCam
	, hasVideo: _hasVideo
	, getRects: _getRects
	, getPos: _getPos
	, container: _container
	, arrange: _arrange
	, arrangeResize: _arrangeResize
	, cleanStream: _cleanStream
	, cleanPeer: _cleanPeer
	, addIceServers: function(opts, m) {
		if (m && m.iceServers && m.iceServers.length > 0) {
			opts.configuration = {iceServers: m.iceServers};
		}
		return opts;
	}
	, setPos: _setPos
	, askPermission: _askPermission
	, disconnect: _disconnect
	, sharingSupported: _sharingSupported
	, highlight: _highlight
};
