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
package org.apache.openmeetings.web.room.wb;

import static org.apache.openmeetings.core.util.WebSocketHelper.alwaysTrue;
import static org.apache.openmeetings.core.util.WebSocketHelper.publish;
import static org.apache.openmeetings.core.util.WebSocketHelper.sendRoom;
import static org.apache.openmeetings.util.OpenmeetingsVariables.PARAM_SRC;
import static org.apache.openmeetings.util.OpenmeetingsVariables.PARAM__SRC;

import java.util.function.Predicate;

import org.apache.openmeetings.db.entity.basic.Client;
import org.apache.openmeetings.db.entity.file.BaseFileItem;
import org.apache.openmeetings.util.NullStringer;
import org.apache.openmeetings.util.ws.IClusterWsMessage;
import org.apache.openmeetings.web.room.RoomPreviewResourceReference;
import org.apache.openmeetings.web.room.RoomResourceReference;
import org.apache.openmeetings.web.user.record.Mp4RecordingResourceReference;
import org.apache.openmeetings.web.user.record.PngRecordingResourceReference;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.FileSystemResourceReference;

import com.github.openjson.JSONObject;

public class WbWebSocketHelper {
	public static final String PARAM_OBJ = "obj";
	private static final String PARAM__POSTER = "_poster";

	private WbWebSocketHelper() {
		// denied
	}

	public static boolean send(IClusterWsMessage inMsg) {
		if (inMsg instanceof WsMessageWb) {
			WsMessageWb m = (WsMessageWb)inMsg;
			if (m.getUid() == null) {
				sendWbAll(m.getRoomId(), m.getMeth(), m.getObj(), false);
			} else {
				sendWbOthers(m.getRoomId(), m.getMeth(), m.getObj(), m.getUid(), false);
			}
			return true;
		} else if (inMsg instanceof WsMessageWbFile) {
			WsMessageWbFile m = (WsMessageWbFile)inMsg;
			sendWbFile(m.getRoomId(), m.getWbId(), m.getRoomUid(), m.getFile(), m.getFileItem(), false);
			return true;
		}
		return false;
	}

	public static void sendWbAll(Long roomId, WbAction meth, JSONObject obj) {
		sendWbAll(roomId, meth, obj, true);
	}

	private static void sendWbAll(Long roomId, WbAction meth, JSONObject obj, boolean publish) {
		if (publish) {
			publish(new WsMessageWb(roomId, meth, obj, null));
		}
		sendWb(roomId, meth, obj, alwaysTrue());
	}

	public static void sendWbOthers(Long roomId, WbAction meth, JSONObject obj, final String uid) {
		sendWbOthers(roomId, meth, obj, uid, true);
	}

	private static void sendWbOthers(Long roomId, WbAction meth, JSONObject obj, final String uid, boolean publish) {
		if (publish) {
			publish(new WsMessageWb(roomId, meth, obj, uid));
		}
		sendWb(roomId, meth, obj, c -> !uid.equals(c.getUid()));
	}

	public static JSONObject getWbJson(Long wbId) {
		return new JSONObject().put("wbId", wbId);
	}

	public static JSONObject getObjWbJson(Long wbId, Object o) {
		return getWbJson(wbId).put(PARAM_OBJ, o);
	}

	private static CharSequence urlFor(final ResourceReference ref, PageParameters params) {
		RequestCycle rc = RequestCycle.get();
		ResourceReferenceRequestHandler handler = new ResourceReferenceRequestHandler(ref, params);
		return rc.getUrlRenderer().renderContextRelativeUrl(rc.mapUrlFor(handler).toString());
	}

	public static JSONObject addFileUrl(String ruid, JSONObject inFile, BaseFileItem fi, Client c) {
		JSONObject file = new JSONObject(inFile.toString(new NullStringer()));
		final FileSystemResourceReference ref;
		final PageParameters pp = new PageParameters()
				.add("id", fi.getId())
				.add("ruid", ruid)
				.add("wuid", inFile.optString("uid"));
		if (c != null) {
			pp.add("uid", c.getUid());
		}
		file.put("deleted", !fi.exists());
		switch (fi.getType()) {
			case VIDEO:
				ref = new RoomResourceReference();
				file.put(PARAM__SRC, urlFor(ref, pp));
				file.put(PARAM__POSTER, urlFor(new RoomPreviewResourceReference(), pp));
				break;
			case RECORDING:
				ref = new Mp4RecordingResourceReference();
				file.put(PARAM__SRC, urlFor(ref, pp));
				file.put(PARAM__POSTER, urlFor(new PngRecordingResourceReference(), pp));
				break;
			case PRESENTATION:
				ref = new RoomResourceReference();
				file.put(PARAM__SRC, urlFor(ref, pp));
				break;
			default:
				ref = new RoomResourceReference();
				file.put(PARAM_SRC, urlFor(ref, pp));
				break;
		}
		return file;
	}

	public static void sendWbFile(Long roomId, long wbId, String ruid, JSONObject file, BaseFileItem fi) {
		sendWbFile(roomId, wbId, ruid, file, fi, true);
	}

	//This is required cause WebSocketHelper will send message async
	private static String patchUrl(String url, Client c) {
		return url + "&uid=" + c.getUid();
	}

	private static JSONObject patchUrls(BaseFileItem fi, Client c, JSONObject inFile) {
		JSONObject f = new JSONObject(inFile.toString()); // deep copy to ensure thread safety
		switch (fi.getType()) {
			case VIDEO:
			case RECORDING:
				f.put(PARAM__SRC, patchUrl(f.getString(PARAM__SRC), c));
				f.put(PARAM__POSTER, patchUrl(f.getString(PARAM__POSTER), c));
				break;
			case PRESENTATION:
				f.put(PARAM__SRC, patchUrl(f.getString(PARAM__SRC), c));
				break;
			default:
				f.put(PARAM_SRC, patchUrl(f.getString(PARAM_SRC), c));
				break;
		}
		return f;
	}

	private static void sendWbFile(Long roomId, long wbId, String ruid, JSONObject file, BaseFileItem fi, boolean publish) {
		if (publish) {
			publish(new WsMessageWbFile(roomId, wbId, ruid, file, fi));
		}
		final JSONObject f = addFileUrl(ruid, file, fi, null);
		sendRoom(
				roomId
				, new JSONObject().put("type", "wb")
				, alwaysTrue()
				, (o, c) -> o.put("func", WbAction.CREATE_OBJ.jsName())
							.put("param", getObjWbJson(wbId, patchUrls(fi, c, f))));
	}

	private static void sendWb(Long roomId, WbAction meth, JSONObject obj, Predicate<Client> check) {
		sendRoom(
				roomId
				, new JSONObject().put("type", "wb")
				, check
				, (o, c) -> o.put("func", meth.jsName())
							.put("param", obj)
			);
	}
}
