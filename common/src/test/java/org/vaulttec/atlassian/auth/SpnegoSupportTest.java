/*
 * Atlassian SPNEGO Authenticator
 * Copyright (c) 2019 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaulttec.atlassian.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class SpnegoSupportTest {

	@Test
	public void testReadConfig() {
		SpnegoSupport spnegoSupport = new SpnegoSupport();
		Map<String, String> config = spnegoSupport.readConfig("src/test/resources/spnego1.properties");

		assertNotNull(config);
		assertEquals("path1/krb5.conf", config.get("spnego.krb5.conf"));
		assertEquals("user1", config.get("spnego.preauth.username"));
	}

	@Test
	public void testInitConfigs() {
		@SuppressWarnings("serial")
		Map<String, String> params = new HashMap<String, String>() {
			{
				put("config.files", "src/test/resources/spnego1.properties, src/test/resources/spnego2.properties");
			}
		};

		SpnegoSupport spnegoSupport = new SpnegoSupport();
		List<Map<String, String>> configs = spnegoSupport.initConfigs(params);

		assertNotNull(configs);
		assertEquals(configs.size(), 2);
		assertEquals("path1/krb5.conf", configs.get(0).get("spnego.krb5.conf"));
		assertEquals("user1", configs.get(0).get("spnego.preauth.username"));
		assertEquals("path2/krb5.conf", configs.get(1).get("spnego.krb5.conf"));
		assertEquals("user2", configs.get(1).get("spnego.preauth.username"));
	}

	@Test
	public void testIsExcludedUri() {
		@SuppressWarnings("serial")
		Map<String, String> params = new HashMap<String, String>() {
			{
				put("exclude.uris", "/startwith/*, */endswith, */substring/*, /exactmatch" );
			}
		};

		SpnegoSupport spnegoSupport = new SpnegoSupport();
		spnegoSupport.init(params);

		assertFalse(spnegoSupport.isExcludedUri("/nomatch/testresource"));
		assertTrue(spnegoSupport.isExcludedUri("/startwith/testresource"));
		assertTrue(spnegoSupport.isExcludedUri("/exactmatch"));
		assertFalse(spnegoSupport.isExcludedUri("/exactmatch/testresource"));
		assertTrue(spnegoSupport.isExcludedUri("/startwith/testresource"));
		assertFalse(spnegoSupport.isExcludedUri("/test/startwith/testresource"));
		assertTrue(spnegoSupport.isExcludedUri("/testresource/endswith"));
		assertFalse(spnegoSupport.isExcludedUri("/testresource/endswith/test"));
		assertTrue(spnegoSupport.isExcludedUri("/testresource/substring/test"));
		assertTrue(spnegoSupport.isExcludedUri("/substring/test"));
		assertTrue(spnegoSupport.isExcludedUri("/test/substring/"));
		assertTrue(spnegoSupport.isExcludedUri("/substring/"));
	}

	@Test
	public void testHasNegotiationAuthenticationHeader() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Negotiate <token>");
		HttpServletResponse response = mock(HttpServletResponse.class);

		SpnegoSupport spnegoSupport = new SpnegoSupport();

		assertTrue(spnegoSupport.hasNegotiationAuthenticationHeader(request, response));
	}

	@Test
	public void testHasNoNegotiationAuthenticationHeader() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn(null);
		HttpServletResponse response = mock(HttpServletResponse.class);

		SpnegoSupport spnegoSupport = new SpnegoSupport();

		assertFalse(spnegoSupport.hasNegotiationAuthenticationHeader(request, response));
		verify(response).addHeader("WWW-Authenticate", "Negotiate");
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}
}
