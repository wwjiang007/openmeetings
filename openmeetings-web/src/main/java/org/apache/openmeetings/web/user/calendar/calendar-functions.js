/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
function onOmGotoClick() {
	const gotoBtn = $('#calendar .fc-gotoBtn-button');
	let selected = null
		, gotoSpan = gotoBtn.parent().find('.goto-span');
	if (gotoSpan.length < 1) {
		gotoBtn.parent().append($('<span class="goto-span"><span/></span>'));
	}
	gotoSpan = gotoBtn.parent().find('.goto-span');
	gotoSpan.datetimepicker({
		format: 'L'
		, icons: {
			time: 'fas fa-clock'
			, date: 'fas fa-calendar'
			, up: 'fas fa-arrow-up'
			, down: 'fas fa-arrow-down'
			, previous: 'fas fa-chevron-left'
			, next: 'fas fa-chevron-right'
			, today: 'fas fa-calendar-check'
			, clear: 'fas fa-trash'
			, close: 'fas fa-times'
		}
		, buttons: {
			showToday: true
			, showClear: true
			, showClose: true
		}
	});
	gotoSpan
		.off()
		.on('hide.datetimepicker', function(e){
			$('#calendar').fullCalendar('gotoDate', e.date.format('YYYY-MM-DD'));
		})
		.datetimepicker('show');
}
