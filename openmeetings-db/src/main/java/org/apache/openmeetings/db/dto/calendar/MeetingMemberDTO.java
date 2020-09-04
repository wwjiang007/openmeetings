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
package org.apache.openmeetings.db.dto.calendar;

import static org.apache.openmeetings.db.util.DtoHelper.optLong;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.openmeetings.db.dao.user.GroupDao;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.dto.user.UserDTO;
import org.apache.openmeetings.db.entity.calendar.MeetingMember;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.wicket.util.string.Strings;

import com.github.openjson.JSONObject;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MeetingMemberDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private UserDTO user;

	public MeetingMemberDTO() {
		//def constructor
	}

	public MeetingMemberDTO(MeetingMember mm) {
		this.id = mm.getId();
		this.user = new UserDTO(mm.getUser());
	}

	public MeetingMember get(UserDao userDao, GroupDao groupDao, User owner) {
		MeetingMember mm = new MeetingMember();
		mm.setId(id);
		if (user.getId() != null) {
			mm.setUser(userDao.get(user.getId()));
		} else {
			User u = null;
			if (User.Type.EXTERNAL == user.getType()) {
				// try to get ext. user
				u = userDao.getExternalUser(user.getExternalId(), user.getExternalType());
			}
			if (u == null && user.getAddress() != null) {
				u = userDao.getContact(user.getAddress().getEmail()
						, user.getFirstname()
						, user.getLastname()
						, user.getLanguageId()
						, user.getTimeZoneId()
						, owner);
			}
			if (u == null) {
				user.setType(User.Type.CONTACT);
				u = user.get(userDao, groupDao);
				u.getRights().clear();
			}
			if (Strings.isEmpty(u.getTimeZoneId())) {
				u.setTimeZoneId(owner.getTimeZoneId());
			}
			mm.setUser(u);
		}
		return mm;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserDTO getUser() {
		return user;
	}

	public void setUser(UserDTO user) {
		this.user = user;
	}

	public static MeetingMemberDTO get(JSONObject o) {
		MeetingMemberDTO m = new MeetingMemberDTO();
		m.id = optLong(o, "id");
		m.user = UserDTO.get(o.optJSONObject("user"));
		return m;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
}
