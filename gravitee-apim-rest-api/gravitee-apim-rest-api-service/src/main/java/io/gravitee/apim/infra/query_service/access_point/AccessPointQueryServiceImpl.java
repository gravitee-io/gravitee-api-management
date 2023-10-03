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
package io.gravitee.apim.infra.query_service.access_point;

import io.gravitee.apim.core.access_point.model.RestrictedDomainEntity;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessPointQueryServiceImpl implements AccessPointQueryService {

    private final AccessPointRepository accessPointRepository;

    public AccessPointQueryServiceImpl(@Lazy AccessPointRepository accessPointRepository) {
        this.accessPointRepository = accessPointRepository;
    }

    @Override
    public Optional<ReferenceContext> getReferenceContext(final String host) {
        try {
            return accessPointRepository
                .findByHost(host)
                .map(accessPoint ->
                    ReferenceContext
                        .builder()
                        .referenceId(accessPoint.getReferenceId())
                        .referenceType(ReferenceContext.Type.valueOf(accessPoint.getReferenceType().name()))
                        .build()
                );
        } catch (TechnicalException e) {
            log.debug("Unable to retrieve access point from given host '{}'", host, e);
            return Optional.empty();
        }
    }

    @Override
    public List<String> getConsoleUrls(final String organizationId, final boolean includeDefault) {
        try {
            return findUrls(AccessPointReferenceType.ORGANIZATION, organizationId, AccessPointTarget.CONSOLE, includeDefault).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting console access point for environment '%s'", organizationId),
                e
            );
        }
    }

    @Override
    public String getConsoleUrl(final String organizationId) {
        try {
            return findUrls(AccessPointReferenceType.ORGANIZATION, organizationId, AccessPointTarget.CONSOLE, false)
                .findFirst()
                .orElse(null);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting console access point for organization '%s'", organizationId),
                e
            );
        }
    }

    @Override
    public String getConsoleApiUrl(final String organizationId) {
        try {
            return findUrls(AccessPointReferenceType.ORGANIZATION, organizationId, AccessPointTarget.CONSOLE_API, false)
                .findFirst()
                .orElse(null);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting console api access point for organization '%s'", organizationId),
                e
            );
        }
    }

    @Override
    public List<String> getPortalUrls(final String environmentId, final boolean includeDefault) {
        try {
            return findUrls(AccessPointReferenceType.ENVIRONMENT, environmentId, AccessPointTarget.PORTAL, includeDefault).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public String getPortalUrl(final String environmentId) {
        try {
            return findUrls(AccessPointReferenceType.ENVIRONMENT, environmentId, AccessPointTarget.PORTAL, false).findFirst().orElse(null);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public String getPortalApiUrl(final String environmentId) {
        try {
            return findUrls(AccessPointReferenceType.ENVIRONMENT, environmentId, AccessPointTarget.PORTAL_API, false)
                .findFirst()
                .orElse(null);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal api access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public List<RestrictedDomainEntity> getGatewayRestrictedDomains(final String environmentId) {
        try {
            List<AccessPoint> filteredList = findAccessPoints(
                AccessPointReferenceType.ENVIRONMENT,
                environmentId,
                AccessPointTarget.GATEWAY,
                false
            );

            return filteredList
                .stream()
                .map(customDomain ->
                    RestrictedDomainEntity.builder().domain(customDomain.getHost()).secured(customDomain.isSecured()).build()
                )
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting gateway restricted domain from environment '%s'", environmentId),
                e
            );
        }
    }

    private Stream<String> findUrls(
        final AccessPointReferenceType referenceType,
        final String referenceId,
        final AccessPointTarget target,
        final boolean includeAll
    ) throws TechnicalException {
        return findAccessPoints(referenceType, referenceId, target, includeAll).stream().map(this::buildHttpUrl);
    }

    private List<AccessPoint> findAccessPoints(
        final AccessPointReferenceType referenceType,
        final String referenceId,
        final AccessPointTarget target,
        final boolean includeAll
    ) throws TechnicalException {
        List<AccessPoint> filteredList = new ArrayList<>();

        // Retrieve domain for env
        List<AccessPoint> domainsForEnv = accessPointRepository.findByReferenceAndTarget(referenceType, referenceId, target);
        if (domainsForEnv.size() == 1 || includeAll) {
            filteredList.addAll(domainsForEnv);
        } else {
            filteredList.addAll(domainsForEnv.stream().filter(AccessPoint::isOverriding).toList());
        }
        return filteredList;
    }

    private String buildHttpUrl(final AccessPoint accessPoint) {
        if (accessPoint != null) {
            StringBuilder consoleUrl = new StringBuilder();
            if (accessPoint.isSecured()) {
                consoleUrl.append("https");
            } else {
                consoleUrl.append("http");
            }
            consoleUrl.append("://").append(accessPoint.getHost());
            return consoleUrl.toString();
        }
        return null;
    }
}
