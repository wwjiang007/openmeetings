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
package org.apache.openmeetings.web.room.menu;

import static org.apache.openmeetings.web.app.WebSession.getRights;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.openmeetings.db.dao.room.RoomDao;
import org.apache.openmeetings.db.dao.user.GroupUserDao;
import org.apache.openmeetings.db.entity.room.Invitation;
import org.apache.openmeetings.db.entity.room.Invitation.MessageType;
import org.apache.openmeetings.db.entity.user.Group;
import org.apache.openmeetings.db.entity.user.GroupUser;
import org.apache.openmeetings.db.util.AuthLevelUtil;
import org.apache.openmeetings.service.room.InvitationManager;
import org.apache.openmeetings.web.app.WebSession;
import org.apache.openmeetings.web.common.GroupChoiceProvider;
import org.apache.openmeetings.web.common.InvitationForm;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Select2MultiChoice;

public class RoomInvitationForm extends InvitationForm {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(RoomInvitationForm.class);
	private final RadioGroup<InviteeType> rdi = new RadioGroup<>("inviteeType", Model.of(InviteeType.user));
	private final Long roomId;
	private final WebMarkupContainer groupContainer = new WebMarkupContainer("groupContainer");
	final Select2MultiChoice<Group> groups = new Select2MultiChoice<>("groups"
			, new CollectionModel<>(new ArrayList<>())
			, new GroupChoiceProvider());
	final WebMarkupContainer sipContainer = new WebMarkupContainer("sip-container");
	@SpringBean
	private RoomDao roomDao;
	@SpringBean
	private GroupUserDao groupUserDao;
	@SpringBean
	private InvitationManager invitationManager;

	enum InviteeType {
		user
		, group
	}

	public RoomInvitationForm(String id, Long roomId) {
		super(id);
		this.roomId = roomId;
		boolean showGroups = AuthLevelUtil.hasAdminLevel(getRights());
		add(rdi.add(new AjaxFormChoiceComponentUpdatingBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				boolean groupsEnabled = InviteeType.group == rdi.getModelObject();
				updateButtons(target);
				target.add(groups.setEnabled(groupsEnabled), recipients.setEnabled(!groupsEnabled));
			}
		}));
		groupContainer.add(
			groups.setRequired(true).add(new AjaxFormComponentUpdatingBehavior("change") {
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					url.setModelObject(null);
					updateButtons(target);
				}
			}).setOutputMarkupId(true)
			, new Radio<>("group", Model.of(InviteeType.group))
		);
		rdi.add(recipients, groupContainer.setVisible(showGroups));
		rdi.add(new Radio<>("user", Model.of(InviteeType.user)));
		add(sipContainer.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true));
		sipContainer.add(new Label("room.confno", "")).setVisible(false);
	}

	@Override
	protected void onInitialize() {
		groups.setLabel(new ResourceModel("126"));
		super.onInitialize();
	}

	@Override
	protected void updateButtons(AjaxRequestTarget target) {
		if (rdi.getModelObject() == InviteeType.user) {
			super.updateButtons(target);
		} else {
			Collection<Group> to = groups.getModelObject();
			target.add(
					dialog.getSend().setEnabled(!to.isEmpty())
					, dialog.getGenerate().setEnabled(false)
					);
		}
	}

	@Override
	public void updateModel(AjaxRequestTarget target) {
		super.updateModel(target);
		Invitation i = getModelObject();
		i.setRoom(roomDao.get(roomId));
		if (i.getRoom() != null) {
			target.add(sipContainer.replace(new Label("room.confno", i.getRoom().getConfno())).setVisible(i.getRoom().isSipEnabled()));
		}
		groups.setModelObject(new ArrayList<>());
		groups.setEnabled(false);
		rdi.setModelObject(InviteeType.user);
	}

	@Override
	public void onClick(AjaxRequestTarget target, InvitationForm.Action action) {
		if (InvitationForm.Action.SEND == action && Strings.isEmpty(url.getModelObject()) && rdi.getModelObject() == InviteeType.group) {
			final String userbaseUrl = WebSession.get().getExtendedProperties().getBaseUrl();
			for (Group g : groups.getModelObject()) {
				for (GroupUser ou : groupUserDao.get(g.getId(), 0, Integer.MAX_VALUE)) {
					Invitation i = create(ou.getUser());
					try {
						invitationManager.sendInvitationLink(i, MessageType.CREATE, subject.getModelObject(), message.getModelObject(), false, userbaseUrl);
					} catch (Exception e) {
						log.error("error while sending invitation by Group ", e);
					}
				}
			}
		}
		super.onClick(target, action);
	}
}
