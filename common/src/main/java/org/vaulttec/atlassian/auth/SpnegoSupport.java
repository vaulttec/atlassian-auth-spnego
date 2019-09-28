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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.spnego.SpnegoAuthenticator;
import net.sourceforge.spnego.SpnegoHttpServletResponse;

public class SpnegoSupport {

	private static final Logger LOG = LoggerFactory.getLogger(SpnegoSupport.class);

	private static final String CONFIG_PARAM_CONFIG_FILES = "config.files";
	private static final String CONFIG_PARAM_INCLUDE_URIS = "include.uris";
	private static final String CONFIG_PARAM_EXCLUDE_URIS = "exclude.uris";
	private static final String INCLUDE_FILE_PROPERTY = "include.file";

	private boolean hasInit;
	private List<Map<String, String>> configs;
	private Set<String> includedUris;
	private Set<String> excludedUris;

	public SpnegoSupport check() throws IllegalStateException {
		if (!hasInit) {
			throw new IllegalStateException("Init must be called before use");
		}
		return this;
	}

	public final void init(final Map<String, String> params) {
		configs = initConfigs(params);
		includedUris = initIncludedUris(params);
		excludedUris = initExcludedUris(params);
		hasInit = true;
	}

	protected List<Map<String, String>> initConfigs(final Map<String, String> params) {
		List<Map<String, String>> configs = new ArrayList<Map<String, String>>();
		String configFiles = params.get(CONFIG_PARAM_CONFIG_FILES);
		LOG.trace("Init: Config files '{}'", configFiles);
		if (configFiles != null) {
			for (String configFile : configFiles.split(",")) {
				Map<String, String> config = readConfig(configFile.trim());
				if (config != null && !config.isEmpty()) {
					configs.add(config);
				}
			}
		}
		return configs;
	}

	protected Map<String, String> readConfig(final String configFile) {
		LOG.trace("Init: Loading config from '{}'", configFile);
		try (FileInputStream in = new FileInputStream(configFile)) {
			Properties properties = new Properties();
			properties.load(in);

			// read additional properties (e.g. credentials) from separate file specified in
			// configuration
			String includeFile = properties.getProperty(INCLUDE_FILE_PROPERTY);
			if (includeFile != null) {
				LOG.trace("Init: Loading additional properties from '{}'", includeFile);
				try (FileInputStream includeIn = new FileInputStream(includeFile)) {
					properties.load(includeIn);
				} catch (IOException e) {
					LOG.error("Error reading include file", e);
					return null;
				}
			}

			// convert properties into map
			Map<String, String> config = new HashMap<String, String>();
			for (final String name : properties.stringPropertyNames()) {
				config.put(name, properties.getProperty(name));
			}
			LOG.info("Init: Config from '{}'={}", configFile, config);
			return config;
		} catch (IOException e) {
			LOG.error("Error reading config file", e);
		}
		return null;
	}

	private Set<String> initIncludedUris(final Map<String, String> params) {
		Set<String> uris = new HashSet<String>();
		String excludeUris = params.get(CONFIG_PARAM_INCLUDE_URIS);
		if (excludeUris != null) {
			for (String uri : excludeUris.split(",")) {
				uris.add(uri.trim());
			}
		}
		LOG.info("Init: Included URIs '{}'", uris);
		return uris;
	}

	private Set<String> initExcludedUris(final Map<String, String> params) {
		Set<String> uris = new HashSet<String>();
		String excludeUris = params.get(CONFIG_PARAM_EXCLUDE_URIS);
		if (excludeUris != null) {
			for (String uri : excludeUris.split(",")) {
				uris.add(uri.trim());
			}
		}
		LOG.info("Init: Excluded URIs '{}'", uris);
		return uris;
	}

	public boolean isIncludedUri(final HttpServletRequest request) {
		return isUri(request, includedUris);
	}

	public boolean isExcludedUri(final HttpServletRequest request) {
		return isUri(request, excludedUris);
	}

	protected boolean isUri(final HttpServletRequest request, final Collection<String> uris) {
		for (String uri : uris) {
			int queryStringIndex = uri.indexOf('?');
			if (queryStringIndex != -1) {
				if (isQueryString(request, uri.substring(queryStringIndex + 1))) {
					if (isUri(request, uri.substring(0, queryStringIndex))) {
						return true;
					}
				}
			} else {
				if (isUri(request, uri)) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean isUri(final HttpServletRequest request, final String uri) {
		if (uri.endsWith("*")) {
			if (uri.startsWith("*")) {
				if (request.getRequestURI().indexOf(uri.substring(1, uri.length() - 1)) != -1) {
					return true;
				}
			} else {
				if (request.getRequestURI().startsWith(uri.substring(0, uri.length() - 1))) {
					return true;
				}
			}
		} else if (uri.startsWith("*")) {
			if (request.getRequestURI().endsWith(uri.substring(1))) {
				return true;
			}
		} else {
			if (request.getRequestURI().equals(uri)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isQueryString(final HttpServletRequest request, final String queryString) {
		if (request.getQueryString() != null) {
			if (queryString.endsWith("*")) {
				if (queryString.startsWith("*")) {
					if (request.getQueryString().indexOf(queryString.substring(1, queryString.length() - 1)) != -1) {
						return true;
					}
				} else {
					if (request.getQueryString().startsWith(queryString.substring(0, queryString.length() - 1))) {
						return true;
					}
				}
			} else if (queryString.startsWith("*")) {
				if (request.getQueryString().endsWith(queryString.substring(1))) {
					return true;
				}

			} else {
				if (request.getQueryString().equals(queryString)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasNegotiationAuthenticationHeader(final HttpServletRequest request,
			final HttpServletResponse response) {
		String header = request.getHeader("Authorization");
		LOG.trace("Authorization header: {}", header);
		if (header == null || header.toUpperCase().indexOf("NEGOTIATE") == -1) {
			try {
				response.addHeader("WWW-Authenticate", "Negotiate");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			} catch (Exception e) {
				LOG.error("Unable to write response");
			}
		}
		return true;
	}

	public String authenticate(final HttpServletRequest request, final HttpServletResponse response) {
		Principal principal = null;
		for (Map<String, String> config : configs) {
			LOG.debug("Executing SPNEGO authentication with configuration '{}' for URI '{}'", config.get("name"),
					request.getRequestURI());
			SpnegoAuthenticator authenticator = null;
			try {
				authenticator = new SpnegoAuthenticator(config);
				principal = authenticator.authenticate(request, new SpnegoHttpServletResponse(response));
				if (principal != null) {
					LOG.debug("SPENGO user in '{}': {}", config.get("name"), principal.getName());

					// remove domain from principal name
					return principal.getName().split("@")[0];
				}
			} catch (UnsupportedOperationException e) {
				LOG.warn("NTLM is not supported");
				return null;
			} catch (LoginException | IOException | GSSException | PrivilegedActionException | URISyntaxException e) {
				LOG.error("SPNEGO authentication throws an exception", e);
				return null;
			} finally {
				if (authenticator != null) {
					authenticator.dispose();
				}
			}
		}
		LOG.warn("No authentication information found in request");
		return null;
	}
}
