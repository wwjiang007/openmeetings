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
package org.apache.openmeetings.web.user.dashboard.admin;

import org.apache.openmeetings.web.app.Application;
import org.apache.wicket.model.Model;
import org.wicketstuff.dashboard.AbstractWidget;
import org.wicketstuff.dashboard.WidgetLocation;
import org.wicketstuff.dashboard.web.WidgetView;

public class AdminWidget extends AbstractWidget {
	private static final long serialVersionUID = 1L;
	public static final String WIDGET_ID_ADMIN = "AdminWidget";

	public AdminWidget() {
		location = new WidgetLocation(0, 2);
	}

	@Override
	public String getId() {
		return WIDGET_ID_ADMIN;
	}

	@Override
	public String getTitle() {
		return Application.getString("dashboard.widget.admin.title");
	}

	@Override
	public WidgetView createView(String viewId) {
		return new AdminWidgetView(viewId, new Model<>(this));
	}
}
