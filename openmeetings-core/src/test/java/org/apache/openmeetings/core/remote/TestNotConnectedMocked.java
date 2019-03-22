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
package org.apache.openmeetings.core.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.github.openjson.JSONObject;

public class TestNotConnectedMocked extends BaseMockedTest {
	@Test
	public void testNotConnected() {
		handler.onMessage(null, MSG_BASE);
	}

	@Test
	public void testRecordingAllowed() {
		assertFalse(streamProcessor.recordingAllowed(null));
	}

	@Test
	public void testStartRecording() {
		streamProcessor.startRecording(null);
	}

	@Test
	public void testStopRecording() {
		streamProcessor.stopRecording(null);
	}

	@Test
	public void testIsRecording() {
		assertFalse(streamProcessor.isRecording(null));
	}

	@Test
	public void testGetRecordingUser() {
		assertEquals(new JSONObject().toString(), handler.getRecordingUser(null).toString());
	}
}
