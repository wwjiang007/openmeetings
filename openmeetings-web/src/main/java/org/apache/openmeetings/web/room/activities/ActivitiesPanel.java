/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.room.activities;

import static org.apache.openmeetings.core.util.WebSocketHelper.sendRoom;
import static org.apache.openmeetings.web.app.WebSession.getUserId;
import static org.apache.openmeetings.web.util.CallbackFunctionHelper.getNamedFunction;
import static org.apache.wicket.ajax.attributes.CallbackParameter.explicit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.openmeetings.db.entity.basic.Client;
import org.apache.openmeetings.db.entity.room.Room.Right;
import org.apache.openmeetings.db.entity.room.Room.RoomElement;
import org.apache.openmeetings.db.util.ws.RoomMessage;
import org.apache.openmeetings.db.util.ws.TextRoomMessage;
import org.apache.openmeetings.web.app.ClientManager;
import org.apache.openmeetings.web.pages.BasePage;
import org.apache.openmeetings.web.room.RoomPanel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

public class ActivitiesPanel extends Panel {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(ActivitiesPanel.class);
	private static final String PARAM_ID = "id";
	private static final String ACTION = "action";
	private static final String PARAM_ROOM_ID = "roomid";
	private static final String ACTIVITY_FMT = "%s %s [%s]";
	private static final String ACTIVITY_FMT_RTL = "%3$s %2$s [%1$s]";
	private enum Action {
		accept, decline, close
	};
	private static final FastDateFormat df = FastDateFormat.getInstance("HH:mm:ss");
	private final Map<String, Activity> activities = new LinkedHashMap<>();
	private final RoomPanel room;
	private final AbstractDefaultAjaxBehavior action = new AbstractDefaultAjaxBehavior() {
		private static final long serialVersionUID = 1L;

		private TextRoomMessage getRemoveMsg(String id) {
			return new TextRoomMessage(room.getRoom().getId(), room.getClient(), RoomMessage.Type.activityRemove, id);
		}

		@Override
		protected void respond(AjaxRequestTarget target) {
			if (!isVisible()) {
				return;
			}
			try {
				String id = getRequest().getRequestParameters().getParameterValue(PARAM_ID).toString();
				long roomId = getRequest().getRequestParameters().getParameterValue(PARAM_ROOM_ID).toLong();
				Action act = Action.valueOf(getRequest().getRequestParameters().getParameterValue(ACTION).toString());
				Activity a = activities.get(id);
				if (a == null || !room.getRoom().getId().equals(roomId)) {
					log.error("It seems like we are being hacked!!!!");
					return;
				}
				switch (act) {
					case close:
						remove(target, id);
						break;
					case decline:
						if (room.getClient().hasRight(Right.moderator)) {
							sendRoom(getRemoveMsg(id));
						}
						break;
					case accept:
						Client client = cm.get(a.getUid());
						if (room.getClient().hasRight(Right.moderator) && client != null && client.getRoom() != null && roomId == client.getRoom().getId()) {
							switch (a.getType()) {
								case reqRightModerator:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.moderator);
									break;
								case reqRightAv:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.audio, Right.video);
									break;
								case reqRightPresenter:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.presenter);
									break;
								case reqRightWb:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.whiteBoard);
									break;
								case reqRightShare:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.share);
									break;
								case reqRightRemote:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.remoteControl);
									break;
								case reqRightA:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.audio);
									break;
								case reqRightMuteOthers:
									sendRoom(getRemoveMsg(id));
									room.allowRight(client, Right.muteOthers);
									break;
								default:
									break;
							}
						}
						break;
				}
			} catch (Exception e) {
				log.error("Unexpected exception while processing activity action", e);
			}
		}

		@Override
		public void renderHead(Component component, IHeaderResponse response) {
			super.renderHead(component, response);
			response.render(new PriorityHeaderItem(getNamedFunction("activityAction", this, explicit(PARAM_ROOM_ID), explicit(ACTION), explicit(PARAM_ID))));
		}
	};
	@SpringBean
	private ClientManager cm;

	public ActivitiesPanel(String id, RoomPanel room) {
		super(id);
		this.room = room;
		setVisible(!room.getRoom().isHidden(RoomElement.Activities));
		setOutputMarkupPlaceholderTag(true);
		setMarkupId(id);
		add(action);
	}

	private boolean shouldSkip(final boolean self, final Activity a) {
		return !self && a.getType().isAction() && !room.getClient().hasRight(Right.moderator);
	}

	public void add(Activity a, IPartialPageRequestHandler handler) {
		if (!isVisible()) {
			return;
		}
		final boolean self = getUserId().equals(a.getSender());
		if (shouldSkip(self, a)) {
			return;
		}
		if (a.getType().isAction()) {
			remove(handler, activities.entrySet().parallelStream()
				.filter(e -> a.getSender().equals(e.getValue().getSender()) && a.getType() == e.getValue().getType())
				.map(e -> e.getValue().getId())
				.toArray(String[]::new));
		}
		activities.put(a.getId(), a);
		String text = "";
		final String name = self ? getString("1362") : a.getName();
		final String fmt = ((BasePage)getPage()).isRtl() ? ACTIVITY_FMT_RTL : ACTIVITY_FMT;
		switch (a.getType()) {
			case roomEnter:
				text = String.format(fmt, name, getString("activities.msg.enter"), df.format(a.getCreated()));
				break;
			case roomExit:
				text = String.format(fmt, name, getString("activities.msg.exit"), df.format(a.getCreated()));
				break;
			case reqRightModerator:
				text = String.format(fmt, name, getString("activities.request.right.moderator"), df.format(a.getCreated()));
				break;
			case reqRightPresenter:
				text = String.format(fmt, name, getString("activities.request.right.presenter"), df.format(a.getCreated()));
				break;
			case reqRightWb:
				text = String.format(fmt, name, getString("activities.request.right.wb"), df.format(a.getCreated()));
				break;
			case reqRightShare:
				text = String.format(fmt, name, getString("activities.request.right.share"), df.format(a.getCreated()));
				break;
			case reqRightRemote:
				text = String.format(fmt, name, getString("activities.request.right.remote"), df.format(a.getCreated()));
				break;
			case reqRightA:
				text = String.format(fmt, name, getString("activities.request.right.audio"), df.format(a.getCreated()));
				break;
			case reqRightAv:
				text = String.format(fmt, name, getString("activities.request.right.video"), df.format(a.getCreated()));
				break;
			case reqRightMuteOthers:
				text = String.format(fmt, name, getString("activities.request.right.muteothers"), df.format(a.getCreated()));
				break;
			case haveQuestion:
				text = String.format(fmt, name, getString("activities.ask.question"), df.format(a.getCreated()));
				break;
		}
		final JSONObject aobj = new JSONObject()
			.put("id", a.getId())
			.put("uid", a.getUid())
			.put("cssClass", getClass(a))
			.put("text", text)
			.put("action", a.getType().isAction())
			.put("find", false);

		switch (a.getType()) {
			case reqRightModerator:
			case reqRightPresenter:
			case reqRightWb:
			case reqRightShare:
			case reqRightRemote:
			case reqRightA:
			case reqRightAv:
			case reqRightMuteOthers:
				aobj.put("accept", room.getClient().hasRight(Right.moderator));
				aobj.put("decline", room.getClient().hasRight(Right.moderator));
				break;
			case haveQuestion:
				aobj.put("find", !self);
			case roomEnter:
			case roomExit:
				aobj.put("accept", false);
				aobj.put("decline", false);
				break;
		}
		handler.appendJavaScript(new StringBuilder("Activities.add(").append(aobj.toString()).append(");"));
	}

	public void remove(IPartialPageRequestHandler handler, String...ids) {
		if (ids.length < 1) {
			return;
		}
		JSONArray arr = new JSONArray();
		for (String id : ids) {
			arr.put(id);
			activities.remove(id);
		}
		handler.appendJavaScript(String.format("Activities.remove(%s);", arr));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(ActivitiesPanel.class, "activities.js"))));
	}

	private static CharSequence getClass(Activity a) {
		StringBuilder cls = new StringBuilder();
		switch (a.getType()) {
			case reqRightModerator:
			case reqRightPresenter:
			case reqRightWb:
			case reqRightShare:
			case reqRightRemote:
			case reqRightA:
			case reqRightAv:
			case reqRightMuteOthers:
			case haveQuestion:
				cls.append("ui-state-highlight");
				break;
			case roomEnter:
			case roomExit:
				cls.append("ui-state-default auto-clean");
		}
		return cls;
	}
}
