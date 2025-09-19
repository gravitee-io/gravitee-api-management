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

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.security.filter.error.ErrorHelper;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class GraviteeContextFilter extends GenericFilterBean {

    private static final String ERROR_MSG = "Invalid organization or environment";
    private static final String ORGANIZATIONS_PATH = "organizations";
    private static final String ENVIRONMENTS_PATH = "environments";
    private final InstallationTypeDomainService installationTypeDomainService;
    private final AccessPointQueryService accessPointQueryService;
    private final EnvironmentService environmentService;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        log.debug("Initializing GraviteeContext from incoming request");
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        ExecutionContext accessPointCxt = getFromAccessPoints(httpServletRequest);
        ExecutionContext pathCxt = getFromRequest(httpServletRequest);
        if (accessPointCxt != null || pathCxt.hasOrganizationId() || pathCxt.hasEnvironmentId()) {
            ExecutionContext resolvedCtx;
            if (accessPointCxt != null) {
                resolvedCtx = resolveFromAccessPoint(accessPointCxt, pathCxt);
            } else {
                resolvedCtx = resolveFromPath(pathCxt);
            }
            // In case the resolved execution is null, return a bad request
            if (resolvedCtx != null) {
                GraviteeContext.fromExecutionContext(resolvedCtx);
            } else {
                log.warn("No execution context resolved from the request %s".formatted(((HttpServletRequest) request).getPathInfo()));
                ErrorHelper.sendError(httpServletResponse, HttpStatusCode.BAD_REQUEST_400, ERROR_MSG);
                return;
            }

            log.debug("GraviteeContext initialized from incoming request [context={}]", resolvedCtx);
        } else {
            GraviteeContext.fromExecutionContext(new ExecutionContext());
        }

        chain.doFilter(request, response);
    }

    @Nullable
    private ExecutionContext resolveFromPath(final ExecutionContext pathExecutionContext) {
        String organizationId;
        if (pathExecutionContext.hasOrganizationId()) {
            organizationId = pathExecutionContext.getOrganizationId();
            if (pathExecutionContext.hasEnvironmentId()) {
                try {
                    EnvironmentEntity environment = environmentService.findByOrgAndIdOrHrid(
                        organizationId,
                        pathExecutionContext.getEnvironmentId()
                    );
                    return new ExecutionContext(environment);
                } catch (Exception e) {
                    return null;
                }
            } else {
                return new ExecutionContext(organizationId);
            }
        } else {
            if (pathExecutionContext.hasEnvironmentId()) {
                try {
                    EnvironmentEntity environment = environmentService.findById(pathExecutionContext.getEnvironmentId());
                    return new ExecutionContext(environment);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        // Shouldn't happen as path context must contain either organization or environment
        return null;
    }

    @Nullable
    private ExecutionContext resolveFromAccessPoint(
        final ExecutionContext accessPointContext,
        final ExecutionContext pathExecutionContext
    ) {
        // Validate if path execution context contains an organization id that it is the same as the one from access point
        if (
            pathExecutionContext.hasOrganizationId() &&
            !accessPointContext.getOrganizationId().equals(pathExecutionContext.getOrganizationId())
        ) {
            return null;
        }
        if (pathExecutionContext.hasEnvironmentId()) {
            try {
                EnvironmentEntity environment = environmentService.findByOrgAndIdOrHrid(
                    accessPointContext.getOrganizationId(),
                    pathExecutionContext.getEnvironmentId()
                );
                // Validate if access point execution context contains an environment id that it is the same as the one from path
                if (accessPointContext.hasEnvironmentId() && !accessPointContext.getEnvironmentId().equals(environment.getId())) {
                    return null;
                }
                return new ExecutionContext(environment);
            } catch (Exception e) {
                // Ignore parent exception
                return null;
            }
        } else if (accessPointContext.hasEnvironmentId()) {
            return new ExecutionContext(accessPointContext.getOrganizationId(), accessPointContext.getEnvironmentId());
        } else {
            return new ExecutionContext(accessPointContext.getOrganizationId());
        }
    }

    private ExecutionContext getFromAccessPoints(final HttpServletRequest httpServletRequest) {
        ExecutionContext accessPointContext = null;
        if (installationTypeDomainService.isMultiTenant()) {
            Optional<ReferenceContext> optionalReferenceContext = getReferenceContextFromServer(httpServletRequest).or(() ->
                getReferenceContextFromReferer(httpServletRequest)
            );
            if (optionalReferenceContext.isPresent()) {
                ReferenceContext referenceContext = optionalReferenceContext.get();
                if (referenceContext.getReferenceType() == ReferenceContext.Type.ENVIRONMENT) {
                    EnvironmentEntity environment = environmentService.findById(referenceContext.getReferenceId());
                    accessPointContext = new ExecutionContext(environment);
                } else if (referenceContext.getReferenceType() == ReferenceContext.Type.ORGANIZATION) {
                    accessPointContext = new ExecutionContext(referenceContext.getReferenceId());
                } else {
                    throw new IllegalStateException(String.format("Unsupported reference type '%s'", referenceContext.getReferenceType()));
                }
            }
        }
        return accessPointContext;
    }

    private Optional<ReferenceContext> getReferenceContextFromServer(final HttpServletRequest httpServletRequest) {
        return getReferenceContext(httpServletRequest.getServerName(), httpServletRequest.getServerPort());
    }

    private Optional<? extends ReferenceContext> getReferenceContextFromReferer(final HttpServletRequest httpServletRequest) {
        // Find related api access points
        String refererHeaderValue = httpServletRequest.getHeader(HttpHeaderNames.REFERER);
        if (refererHeaderValue != null) {
            try {
                URL refererUrl = new URL(refererHeaderValue);
                return getReferenceContext(refererUrl.getHost(), refererUrl.getPort());
            } catch (MalformedURLException e) {
                // Ignore this except
                log.warn("Unable to retrieve access point from origin due to an error when reading header.");
            }
        }
        return Optional.empty();
    }

    @NonNull
    private Optional<ReferenceContext> getReferenceContext(final String host, final int port) {
        return accessPointQueryService.getReferenceContext(host).or(() -> accessPointQueryService.getReferenceContext(host + ":" + port));
    }

    private ExecutionContext getFromRequest(final HttpServletRequest httpServletRequest) {
        String organizationId = null;
        String environmentId = null;
        String pathInfo = httpServletRequest.getPathInfo();
        String[] pathParams = pathInfo.split("/");
        int pathIndex = 0;
        while (pathIndex < pathParams.length && (organizationId == null || environmentId == null)) {
            String param = pathParams[pathIndex];
            if (ORGANIZATIONS_PATH.equals(param)) {
                pathIndex++;
                if (pathIndex < pathParams.length) {
                    organizationId = pathParams[pathIndex];
                }
            } else if (ENVIRONMENTS_PATH.equals(param)) {
                pathIndex++;
                if (pathIndex < pathParams.length) {
                    environmentId = pathParams[pathIndex];
                }
            } else {
                pathIndex++;
            }
        }

        return new ExecutionContext(organizationId, environmentId);
    }
}
