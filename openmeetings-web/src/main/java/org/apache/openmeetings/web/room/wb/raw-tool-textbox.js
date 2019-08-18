/* Licensed under the Apache License, Version 2.0 (the "License") http://www.apache.org/licenses/LICENSE-2.0 */
var Textbox = function(wb, s, sBtn) {
	const text = Text(wb, s, sBtn);
	text.omType = 'textbox';

	text.createTextObj = function(canvas, pointer) {
		return new fabric.Textbox('', {
			left: pointer.x
			, top: pointer.y
			, padding: 7
			, fill: text.fill.enabled ? text.fill.color : 'rgba(0,0,0,0)'
			, stroke: text.stroke.enabled ? text.stroke.color : 'rgba(0,0,0,0)'
			//, strokeWidth: text.stroke.width
			, fontSize: text.stroke.width
			, omType: text.omType
			, fontFamily: text.fontFamily
			, opacity: text.opacity
			, breakWords: true
			, width: canvas.width / 4
			, lockScalingX: false
			, lockScalingY: true
		});
	};
	text.doubleClick = function(e) {
		const ao = e.target;
		if (!!ao && text.omType === ao.omType) {
			text.obj = ao;
			text.obj.enterEditing();
			WbArea.removeDeleteHandler();
		}
	};
	text._onMouseDown = function() {
		WbArea.addDeleteHandler();
	};
	text._onActivate = function() {
		WbArea.addDeleteHandler();
	};
	text._onDeactivate = function() {
		WbArea.addDeleteHandler();
	};
	return text;
};
