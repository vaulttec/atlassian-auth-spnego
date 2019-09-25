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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.security.login.JiraSeraphAuthenticator;
import com.atlassian.seraph.config.SecurityConfig;

public class JiraSpnegoAuthenticator extends JiraSeraphAuthenticator implements SpnegoAuthenticator {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(JiraSpnegoAuthenticator.class);

	private final SpnegoSupport support = new SpnegoSupport();

	@Override
	public final void init(final Map<String, String> params, final SecurityConfig config) {
		super.init(params, config);
		support.init(params);
	}

	@Override
	public Logger getLogger() {
		return LOG;
	}

	@Override
	public SpnegoSupport getSupport() throws IllegalStateException {
		return support.check();
	}

	@Override
	public Principal getUser(final String userName) {
		return super.getUser(userName);
	}

	@Override
	public Principal getUserFromSession(final HttpServletRequest request) {
		return super.getUserFromSession(request);
	}

	@Override
	public final Principal getUser(final HttpServletRequest request, final HttpServletResponse response) {
		return getUserViaSPNEGO(request, response);
	}

	@Override
	public boolean authoriseUserAndEstablishSession(final HttpServletRequest request,
			final HttpServletResponse response, final Principal user) {
		if (super.authoriseUserAndEstablishSession(request, response, user)) {
			if (response != null) {
				getRememberMeService().addRememberMeCookie(request, response, user.getName());
			}
			return true;
		}
		return false;
	}
}
