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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.model.RestrictedDomainEntity;
import io.gravitee.rest.api.service.AccessPointService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class AccessPointServiceImpl extends AbstractService implements AccessPointService {

    @Autowired
    @Lazy
    private AccessPointRepository accessPointRepository;

    @Override
    public String getConsoleUrl(final String organizationId) {
        try {
            return buildHttpUrl(findCustomDomain(AccessPointReferenceType.ORGANIZATION, organizationId, AccessPointTarget.CONSOLE));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting console access point for environment '%s'", organizationId),
                e
            );
        }
    }

    @Override
    public String getPortalUrl(final String environmentId) {
        try {
            return buildHttpUrl(findCustomDomain(AccessPointReferenceType.ENVIRONMENT, environmentId, AccessPointTarget.PORTAL));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public List<RestrictedDomainEntity> getGatewayRestrictedDomains(final String organizationId, final String environmentId) {
        try {
            List<AccessPoint> mergeList = new ArrayList<>();

            // Retrieve domain for orga
            List<AccessPoint> domainsForOrg = accessPointRepository.findByReferenceAndTarget(
                AccessPointReferenceType.ORGANIZATION,
                organizationId,
                AccessPointTarget.GATEWAY
            );
            if (domainsForOrg.size() == 1) {
                mergeList.addAll(domainsForOrg);
            } else {
                mergeList.addAll(domainsForOrg.stream().filter(AccessPoint::isOverriding).toList());
            }

            // Retrieve domain for env
            List<AccessPoint> domainsForEnv = accessPointRepository.findByReferenceAndTarget(
                AccessPointReferenceType.ENVIRONMENT,
                environmentId,
                AccessPointTarget.GATEWAY
            );
            if (domainsForEnv.size() == 1) {
                mergeList.addAll(domainsForEnv);
            } else {
                mergeList.addAll(domainsForEnv.stream().filter(AccessPoint::isOverriding).toList());
            }

            return mergeList
                .stream()
                .map(customDomain ->
                    RestrictedDomainEntity.builder().domain(customDomain.getHost()).secured(customDomain.isSecured()).build()
                )
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format(
                    "An error occurs while getting gateway restricted domain for organization '%s' and environment '%s'",
                    organizationId,
                    environmentId
                ),
                e
            );
        }
    }

    private AccessPoint findCustomDomain(
        final AccessPointReferenceType referenceType,
        final String referenceId,
        final AccessPointTarget target
    ) throws TechnicalException {
        AccessPoint foundAccessPoint = null;
        List<AccessPoint> referenceAndTarget = accessPointRepository.findByReferenceAndTarget(referenceType, referenceId, target);
        if (referenceAndTarget != null && !referenceAndTarget.isEmpty()) {
            if (referenceAndTarget.size() == 1) {
                foundAccessPoint = referenceAndTarget.get(0);
            } else {
                foundAccessPoint = referenceAndTarget.stream().filter(AccessPoint::isOverriding).findFirst().orElse(null);
            }
        }
        return foundAccessPoint;
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
