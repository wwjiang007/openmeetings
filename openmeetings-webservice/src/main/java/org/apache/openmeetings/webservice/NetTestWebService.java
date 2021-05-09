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
package org.apache.openmeetings.webservice;


import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.openmeetings.webservice.util.RateLimited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("netTestWebService")
@Path("/networktest")
public class NetTestWebService {
	private static final Logger log = LoggerFactory.getLogger(NetTestWebService.class);
	public enum TestType {
		UNKNOWN,
		PING,
		JITTER,
		DOWNLOAD_SPEED,
		UPLOAD_SPEED
	}

	private static final int PING_PACKET_SIZE = 64;
	private static final int JITTER_PACKET_SIZE = 1024;
	private static final int MAX_DOWNLOAD_SIZE = 5 * 1024 * 1024;
	private static final int MAX_UPLOAD_SIZE = 5 * 512 * 1024;
	public static final AtomicInteger CLIENT_COUNT = new AtomicInteger();
	private static int maxClients = 100;

	@PostConstruct
	private void report() {
		log.debug("MaxClients: {}", maxClients);
	}

	@RateLimited
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/")
	public Response get(@QueryParam("type") String type, @QueryParam("size") int inSize) {
		final int size;
		final TestType testType = getTypeByString(type);
		log.debug("Network test:: get, {}, {}", testType, inSize);
		if (TestType.UNKNOWN == testType) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		// choose data to send
		switch (testType) {
			case PING:
				size = PING_PACKET_SIZE;
				break;
			case JITTER:
				size = JITTER_PACKET_SIZE;
				break;
			default:
				final int count = CLIENT_COUNT.incrementAndGet();
				log.info("... download: client count: {}", count);
				size = inSize;
				break;
		}
		if (size > MAX_DOWNLOAD_SIZE) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.ok()
				.type(MediaType.APPLICATION_OCTET_STREAM).entity(new InputStream() {
					int pos = 0;

					@Override
					public int read() throws IOException {
						pos++;
						return pos > size ? -1 : ThreadLocalRandom.current().nextInt(0, 0xFF);
					}

					@Override
					public int available() throws IOException {
						return size - pos;
					}

					@Override
					public void close() throws IOException {
						if (TestType.DOWNLOAD_SPEED == testType) {
							final int count = CLIENT_COUNT.decrementAndGet();
							log.info("... close: client count: {}", count);
						}
						super.close();
					}
				})
				.header("Cache-Control", "no-cache, no-store, no-transform")
				.header("Pragma", "no-cache")
				.header("Content-Length", String.valueOf(size))
				.build();
	}

	@RateLimited
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/")
	public int upload(@QueryParam("size") int size, InputStream stream) throws IOException {
		if (size > MAX_UPLOAD_SIZE) {
			return -1;
		}
		CLIENT_COUNT.incrementAndGet();
		byte[] b = new byte[1024];
		int totalCount = 0;
		int count;
		try {
			while ((count = stream.read(b)) > -1) {
				totalCount += count;
			}
			log.debug("Total bytes read {}", totalCount);
		} finally {
			CLIENT_COUNT.decrementAndGet();
		}
		return totalCount;
	}

	public static TestType getTypeByString(String typeString) {
		if ("ping".equals(typeString)) {
			return TestType.PING;
		} else if ("jitter".equals(typeString)) {
			return TestType.JITTER;
		} else if ("download".equals(typeString)) {
			return TestType.DOWNLOAD_SPEED;
		} else if ("upload".equals(typeString)) {
			return TestType.UPLOAD_SPEED;
		}

		return TestType.UNKNOWN;
	}

	@Value("${nettest.max.clients}")
	private void setMaxClients(int count) {
		maxClients = count;
	}

	public static int getMaxClients() {
		return maxClients;
	}
}
