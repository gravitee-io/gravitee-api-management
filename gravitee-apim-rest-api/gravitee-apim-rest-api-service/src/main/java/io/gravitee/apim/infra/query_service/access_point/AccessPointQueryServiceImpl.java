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

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.infra.adapter.AccessPointAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                    ReferenceContext.builder()
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
    public List<AccessPoint> getConsoleAccessPoints() {
        try {
            return accessPointRepository
                .findByTarget(AccessPointAdapter.INSTANCE.fromEntity(AccessPoint.Target.CONSOLE))
                .stream()
                .map(AccessPointAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while getting all console access points", e);
        }
    }

    @Override
    public List<AccessPoint> getConsoleAccessPoints(final String organizationId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organizationId, AccessPoint.Target.CONSOLE, true).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting console access point for organization '%s'", organizationId),
                e
            );
        }
    }

    @Override
    public AccessPoint getConsoleAccessPoint(final String organizationId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organizationId, AccessPoint.Target.CONSOLE, false)
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
    public AccessPoint getConsoleApiAccessPoint(final String organizationId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organizationId, AccessPoint.Target.CONSOLE_API, false)
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
    public List<AccessPoint> getPortalAccessPoints() {
        try {
            return findAccessPoints(AccessPoint.Target.PORTAL);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while getting all portal access points", e);
        }
    }

    @Override
    public List<AccessPoint> getPortalAccessPoints(final String environmentId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environmentId, AccessPoint.Target.PORTAL, true).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public AccessPoint getPortalAccessPoint(final String environmentId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environmentId, AccessPoint.Target.PORTAL, false)
                .findFirst()
                .orElse(null);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting portal access point for environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public AccessPoint getPortalApiAccessPoint(final String environmentId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environmentId, AccessPoint.Target.PORTAL_API, false)
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
    public List<AccessPoint> getGatewayAccessPoints(final String environmentId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environmentId, AccessPoint.Target.GATEWAY, false).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting gateway restricted domain from environment '%s'", environmentId),
                e
            );
        }
    }

    @Override
    public List<AccessPoint> getKafkaGatewayAccessPoints(String environmentId) {
        try {
            return findAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, environmentId, AccessPoint.Target.KAFKA_GATEWAY, false).toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while getting gateway restricted domain from environment '%s'", environmentId),
                e
            );
        }
    }

    private List<AccessPoint> findAccessPoints(final AccessPoint.Target target) throws TechnicalException {
        return accessPointRepository
            .findByTarget(AccessPointAdapter.INSTANCE.fromEntity(target))
            .stream()
            .map(AccessPointAdapter.INSTANCE::toEntity)
            .toList();
    }

    private Stream<AccessPoint> findAccessPoints(
        final AccessPoint.ReferenceType referenceType,
        final String referenceId,
        final AccessPoint.Target target,
        final boolean includeAll
    ) throws TechnicalException {
        List<io.gravitee.repository.management.model.AccessPoint> filteredList = new ArrayList<>();

        // Retrieve domain for env
        List<io.gravitee.repository.management.model.AccessPoint> accessPoints = accessPointRepository.findByReferenceAndTarget(
            AccessPointAdapter.INSTANCE.fromEntity(referenceType),
            referenceId,
            AccessPointAdapter.INSTANCE.fromEntity(target)
        );
        if (accessPoints.size() == 1 || includeAll) {
            filteredList.addAll(accessPoints);
        } else {
            filteredList.addAll(accessPoints.stream().filter(io.gravitee.repository.management.model.AccessPoint::isOverriding).toList());
        }
        return filteredList.stream().map(AccessPointAdapter.INSTANCE::toEntity);
    }
}
