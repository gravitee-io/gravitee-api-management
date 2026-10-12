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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolves the organization of an MCP request the way the Management API does: from the {@code organizations/{id}}
 * path segment, or from the host's access point on a multi-tenant installation.
 *
 * <p>Unlike {@code GraviteeContextFilter}, it never reads the {@code Referer} header, which the caller controls, and
 * accepts organization access points only.
 */
public class McpMountResolver {

    private static final Pattern ORGANIZATION_PATH = Pattern.compile("^/organizations/([^/]+)/?$");

    private final InstallationTypeDomainService installationType;
    private final AccessPointQueryService accessPoints;
    private final OrganizationService organizations;
    private final InstallationAccessQueryService installationAccess;

    public McpMountResolver(
        InstallationTypeDomainService installationType,
        AccessPointQueryService accessPoints,
        OrganizationService organizations,
        InstallationAccessQueryService installationAccess
    ) {
        this.installationType = installationType;
        this.accessPoints = accessPoints;
        this.organizations = organizations;
        this.installationAccess = installationAccess;
    }

    /**
     * @param path the request path under the mount, without the metadata suffix
     * @return empty when the URL names no organization this installation serves there
     */
    public Optional<McpMount> resolve(String path, String host, int port) {
        String pathOrganization = null;
        if (!path.isEmpty() && !path.equals("/")) {
            var matcher = ORGANIZATION_PATH.matcher(path);
            if (!matcher.matches()) {
                return Optional.empty();
            }
            pathOrganization = matcher.group(1);
        }
        var hostOrganization = installationType.isMultiTenant() ? hostOrganization(host, port) : null;
        if (pathOrganization != null && hostOrganization != null && !pathOrganization.equals(hostOrganization)) {
            return Optional.empty();
        }
        if (pathOrganization != null && !exists(pathOrganization)) {
            return Optional.empty();
        }

        var organizationId = pathOrganization != null
            ? pathOrganization
            : hostOrganization != null
                ? hostOrganization
                : GraviteeContext.getDefaultOrganization();
        var apiUrl = installationAccess.getMcpAPIUrl(organizationId);
        var resource = apiUrl == null || pathOrganization == null ? apiUrl : apiUrl + "/organizations/" + pathOrganization;
        return Optional.of(new McpMount(organizationId, pathOrganization != null || hostOrganization != null, resource));
    }

    private String hostOrganization(String host, int port) {
        return accessPoints
            .getReferenceContext(host)
            .or(() -> accessPoints.getReferenceContext(host + ":" + port))
            .filter(reference -> reference.getReferenceType() == ReferenceContext.Type.ORGANIZATION)
            .map(ReferenceContext::getReferenceId)
            .orElse(null);
    }

    private boolean exists(String organizationId) {
        try {
            organizations.findById(organizationId);
            return true;
        } catch (OrganizationNotFoundException e) {
            return false;
        }
    }
}
