/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.security.filter;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.security.filter.error.ErrorHelper;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class GraviteeContextAuthorizationFilter extends GenericFilterBean {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        log.debug("Check if authenticated user is associated to the resolved GraviteeContext");
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String organizationId = GraviteeContext.getCurrentOrganization();
        var principal = (UsernamePasswordAuthenticationToken) httpServletRequest.getUserPrincipal();
        var userDetails = Objects.isNull(principal) ? null : (UserDetails) principal.getPrincipal();

        // If user authenticated, he needs to have access to the organization
        if (Objects.nonNull(userDetails)) {
            if (Objects.isNull(userDetails.getOrganizationId())) {
                ErrorHelper.sendError(httpServletResponse, HttpStatusCode.FORBIDDEN_403, "No organization associated to user");
                return;
            }
            if (!organizationId.equals(userDetails.getOrganizationId())) {
                ErrorHelper.sendError(httpServletResponse, HttpStatusCode.FORBIDDEN_403, "User is not allowed to access this organization");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
