/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
const RingBuffer = require('./ring-buffer');

module.exports = class MicLevel {
	constructor() {
		let ctx, mic, analyser, vol = .0, vals = new RingBuffer(100);

		this.meterPeer = (rtcPeer, cnvs, _micActivity, _error, connectAudio) => {
			if (!rtcPeer || ('function' !== typeof(rtcPeer.getLocalStream) && 'function' !== typeof(rtcPeer.getRemoteStream))) {
				return;
			}
			const stream = rtcPeer.getLocalStream() || rtcPeer.getRemoteStream();
			if (!stream || stream.getAudioTracks().length < 1) {
				return;
			}
			try {
				const AudioCtx = window.AudioContext || window.webkitAudioContext;
				if (!AudioCtx) {
					_error("AudioContext is inaccessible");
					return;
				}
				ctx = new AudioCtx();
				analyser = ctx.createAnalyser();
				mic = ctx.createMediaStreamSource(stream);
				mic.connect(analyser);
				if (connectAudio) {
					analyser.connect(ctx.destination);
				}
				this.meter(analyser, cnvs, _micActivity, _error);
			} catch (err) {
				_error(err);
			}
		};
		this.meter = (_analyser, cnvs, _micActivity, _error) => {
			try {
				analyser = _analyser;
				analyser.minDecibels = -90;
				analyser.maxDecibels = -10;
				analyser.fftSize = 256;
				const canvas = cnvs[0]
					, color = $('body').css('--level-color')
					, canvasCtx = canvas.getContext('2d')
					, al = analyser.frequencyBinCount
					, arr = new Uint8Array(al)
					, horiz = cnvs.data('orientation') === 'horizontal';
				function update() {
					const WIDTH = canvas.width
						, HEIGHT = canvas.height;
					canvasCtx.clearRect(0, 0, WIDTH, HEIGHT);
					if (!!analyser && cnvs.length > 0) {
						if (cnvs.is(':visible')) {
							analyser.getByteFrequencyData(arr);
							let favg = 0.0;
							for (let i = 0; i < al; ++i) {
								favg += arr[i] * arr[i];
							}
							vol = Math.sqrt(favg / al);
							vals.push(vol);
							const min = vals.min();
							_micActivity(vol > min + 5); // magic number
							canvasCtx.fillStyle = color;
							if (horiz) {
								canvasCtx.fillRect(0, 0, WIDTH * vol / 100, HEIGHT);
							} else {
								const h = HEIGHT * vol / 100;
								canvasCtx.fillRect(0, HEIGHT - h, WIDTH, h);
							}
						}
						requestAnimationFrame(update);
					}
				}
				update();
			} catch (err) {
				_error(err);
			}
		};
		this.dispose = () => {
			if (!!ctx) {
				VideoUtil.cleanStream(mic.mediaStream);
				VideoUtil.disconnect(mic);
				VideoUtil.disconnect(ctx.destination);
				ctx.close();
				ctx = null;
			}
			if (!!analyser) {
				VideoUtil.disconnect(analyser);
				analyser = null;
			}
		};
	}
};
