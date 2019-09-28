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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class SpnegoAuthenticatorTest {

	@Test
	public void testGetUserWithSession() {
		Principal user = mock(Principal.class);
		SpnegoAuthenticator authenticator = spy(new MockSpnegoAuthenticator());
		when(authenticator.getUserFromSession(any())).thenReturn(user);

		assertEquals(user, authenticator.getUserViaSPNEGO(null, null));
	}

	@Test
	public void testGetUserWithAuthentication() {
		Principal user = mock(Principal.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		SpnegoSupport support = mock(SpnegoSupport.class);
		when(support.isExcludedUri(any())).thenReturn(false);
		when(support.hasNegotiationAuthenticationHeader(any(), any())).thenReturn(true);
		when(support.authenticate(any(), any())).thenReturn("user1");

		SpnegoAuthenticator authenticator = spy(new MockSpnegoAuthenticator());
		when(authenticator.getUser("user1")).thenReturn(user);
		when(authenticator.getSupport()).thenReturn(support);
		when(authenticator.authoriseUserAndEstablishSession(any(), any(), any())).thenReturn(true);

		assertEquals(user, authenticator.getUserViaSPNEGO(request, response));
	}
}
