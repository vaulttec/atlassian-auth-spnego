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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSpnegoAuthenticator implements SpnegoAuthenticator {

	private static final Logger LOGGER = LoggerFactory.getLogger(MockSpnegoAuthenticator.class);

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	public SpnegoSupport getSupport() throws IllegalStateException {
		return null;
	}

	@Override
	public Principal getUser(String userName) {
		return null;
	}

	@Override
	public Principal getUserFromSession(HttpServletRequest request) {
		return null;
	}

	@Override
	public boolean authoriseUserAndEstablishSession(HttpServletRequest request, HttpServletResponse response,
			Principal user) {
		return false;
	}
}