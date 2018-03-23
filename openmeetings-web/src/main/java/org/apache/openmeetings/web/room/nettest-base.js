/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
var NetTest = (function() {
	const self = {}, LIMIT = 2000;
	let output, lbls, net, tests, testName, testLabel;

	// Based on
	// https://github.com/nesk/network.js/blob/master/example/main.js
	function _init(_lbls) {
		lbls = _lbls;
		output = $('.nettest output');
		$('.nettest button')
			.button()
			.click(function() {
				const btn = $(this);
				testLabel = btn.data('lbl');
				testName = btn.data('measure');
				tests[testName].start();
			});

		net = new Network({
			endpoint: './services/networktest/'
		});
		tests = {
			latency: {
				start: function() {
					const t = net.latency;
					t.settings({
						measures: 5
						, attempts: 3
					});
					t.start();
					t.trigger('start');
				}
			}
			, upload: {
				start: function() {
					const t = net.upload;
					t.settings({
						delay: LIMIT
						, measures: 5
						, data: {
							size: 1 * 1024 * 1024
							, multiplier: 2
						}
					});
					t.start();
				}
			}
			, download: {
				start: function() {
					const t = net.download;
					t.settings({
						delay: LIMIT
						, measures: 5
						, data: {
							size: 1 * 1024 * 1024
							, multiplier: 2
						}
					});
					t.start();
				}
			}
		};
		// progress can be added
		net.upload
			.on('start', _start)
			.on('restart', _restart)
			.on('end', _end);
		net.download
			.on('start', _start)
			.on('restart', _restart)
			.on('end', _end);
		net.latency
			.on('start', _start)
			.on('end', function(avg, _all) {
				const all = $('<span></span>').append('[');
				let delim = '';
				let max = 0, min = Number.MAX_VALUE;
				for (let i = 0; i < _all.length; ++i) {
					const v = _all[i];
					max = Math.max(max, v);
					min = Math.min(min, v);
					all.append(delim).append(_value(v, lbls['ms']));
					delim = ',';
				}
				all.append(']');
				_log(all);
				_log($('<span></span>').append(lbls['jitter.avg']).append(_value(avg, lbls['ms'])));
				_log($('<span></span>').append(lbls['jitter.min']).append(_value(min, lbls['ms'])));
				_log($('<span></span>').append(lbls['jitter.max']).append(_value(max, lbls['ms'])));
				_log($('<span></span>').append(lbls['jitter'])
						.append(':').append(_value(max - avg, lbls['ms']))
						.append(';').append(_value(min - avg, lbls['ms'])));
			});
	}
	function __start(size, newSection) {
		const msg = $('<span></span>').append(lbls['report.start']);
		if (testName === 'upload') {
			msg.append(lbls['upl.bytes']);
		} else if (testName === 'download') {
			msg.append(lbls['dwn.bytes']);
		}
		if (testName !== 'latency') {
			msg.append(_value(size / 1024 / 1024, lbls['mb']));
		}
		msg.append('...');
		_log(_delimiter(msg), newSection);
	}
	function _start(size) {
		__start(size, true);
	}
	function _restart(size) {
		__start(size, false);
	}
	function _mbps() {
		return lbls['mb'] + '/' + lbls['sec'];
	}
	function _end(avg) {
		const msg = $('<span></span>')
			.append(lbls[testName === 'upload' ? 'upl.speed' : 'dwn.speed'])
			.append(_value(avg / 1024 / 1024, _mbps()));
		_log(msg);
	}
	function _delimiter(text) {
		return $('<span class="delim"></span>').html(text);
	}
	function _log(text, newSection) {
		output.append('<br/>');
		if (newSection) {
			output.append('<br/>');
		}
		output.append($('<span class="module"></span>').text(testLabel)).append(text);
		output.find('span').last()[0].scrollIntoView(false);
	}
	function _value(value, unit) {
		if (value != null) {
			return $('<span class="value">' + value.toFixed(3) + ' ' + unit + '</span>');
		} else {
			return $('<span class="value">null</span>');
		}
	}

	self.init = _init;
	return self;
})();