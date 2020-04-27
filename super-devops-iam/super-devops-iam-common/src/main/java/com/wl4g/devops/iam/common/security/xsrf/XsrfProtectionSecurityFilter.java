/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.iam.common.security.xsrf;

import static com.wl4g.devops.tool.common.log.SmartLoggerFactory.getLogger;
import static com.wl4g.devops.tool.common.web.UserAgentUtils.isBrowser;
import static java.util.Objects.isNull;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wl4g.devops.iam.common.security.xsrf.handler.AccessRejectHandler;
import com.wl4g.devops.iam.common.security.xsrf.handler.InvalidXsrfTokenException;
import com.wl4g.devops.iam.common.security.xsrf.handler.MissingXsrfTokenException;
import com.wl4g.devops.iam.common.security.xsrf.repository.XsrfToken;
import com.wl4g.devops.iam.common.security.xsrf.repository.XsrfTokenRepository;
import com.wl4g.devops.tool.common.log.SmartLogger;
import static com.wl4g.devops.tool.common.web.UrlUtils.*;

/**
 * <p>
 * Applies
 * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(XSRF)"
 * >XSRF</a> protection using a synchronizer token pattern. Developers are
 * required to ensure that {@link XsrfProtectionSecurityFilter} is invoked for
 * any request that allows state to change. Typically this just means that they
 * should ensure their web application follows proper REST semantics (i.e. do
 * not change state with the HTTP methods GET, HEAD, TRACE, OPTIONS).
 * </p>
 *
 * <p>
 * Typically the {@link XsrfTokenRepository} implementation chooses to store the
 * </p>
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2020年4月27日
 * @since
 */
public final class XsrfProtectionSecurityFilter extends OncePerRequestFilter {

	protected SmartLogger log = getLogger(getClass());

	@Autowired
	private XsrfTokenRepository xtokenRepository;

	/**
	 * Specifies a {@link XsrfMatcher} that is used to determine if XSRF
	 * protection should be applied. If the {@link XsrfMatcher} returns true for
	 * a given request, then XSRF protection is applied.
	 *
	 * <p>
	 * The default is to apply XSRF protection for any HTTP method other than
	 * GET, HEAD, TRACE, OPTIONS.
	 * </p>
	 *
	 * @param xsrfMatcher
	 *            the {@link XsrfMatcher} used to determine if XSRF protection
	 *            should be applied.
	 */
	@Autowired
	private XsrfMatcher xsrfProtectionMatcher = DEFAULT_XSRF_MATCHER;

	/**
	 * Specifies a access denied handler that should be used when XSRF
	 * protection fails.
	 *
	 * <p>
	 * The default is to use AccessDeniedHandlerImpl with no arguments.
	 * </p>
	 */
	@Autowired
	private AccessRejectHandler rejectHandler;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// Non browser request ignore validation XSRF token
		if (!isBrowser(request)) {
			log.debug("Skip XSRF token for: {}", buildFullRequestUrl(request));
			filterChain.doFilter(request, response);
			return;
		}

		request.setAttribute(HttpServletResponse.class.getName(), response);
		XsrfToken xsrfToken = xtokenRepository.getXToken(request);
		final boolean missingToken = isNull(xsrfToken);
		if (missingToken) {
			xsrfToken = xtokenRepository.generateXToken(request);
			xtokenRepository.saveXToken(xsrfToken, request, response);
		}
		request.setAttribute(XsrfToken.class.getName(), xsrfToken);
		request.setAttribute(xsrfToken.getParameterName(), xsrfToken);

		if (!xsrfProtectionMatcher.matches(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String actualToken = request.getHeader(xsrfToken.getHeaderName());
		if (actualToken == null) {
			actualToken = request.getParameter(xsrfToken.getParameterName());
		}
		if (!xsrfToken.getToken().equals(actualToken)) {
			log.debug("Invalid XSRF token found for: {}", buildFullRequestUrl(request));
			if (missingToken) {
				rejectHandler.handle(request, response, new MissingXsrfTokenException(actualToken));
			} else {
				rejectHandler.handle(request, response, new InvalidXsrfTokenException(xsrfToken, actualToken));
			}
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * The default {@link RequestMatcher} that indicates if XSRF protection is
	 * required or not. The default is to ignore GET, HEAD, TRACE, OPTIONS and
	 * process all other requests.
	 */
	public static final XsrfMatcher DEFAULT_XSRF_MATCHER = new RequiresXsrfMatcher();

}