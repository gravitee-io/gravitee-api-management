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
package io.gravitee.apim.core.access_point.query_service;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.List;
import java.util.Optional;

public interface AccessPointQueryService {
    Optional<ReferenceContext> getReferenceContext(final String host);

    List<AccessPoint> getConsoleAccessPoints();

    List<AccessPoint> getConsoleAccessPoints(final String organizationId);
    AccessPoint getConsoleAccessPoint(final String organizationId);
    AccessPoint getConsoleApiAccessPoint(String organizationId);

    List<AccessPoint> getPortalAccessPoints();

    List<AccessPoint> getPortalAccessPoints(final String environmentId);
    AccessPoint getPortalAccessPoint(final String environmentId);
    AccessPoint getPortalApiAccessPoint(String environmentId);

    List<AccessPoint> getPortalInstanceAccessPoints();

    List<AccessPoint> getPortalInstanceAccessPoints(final String environmentId);

    /** @return the portal access points on this host, in every environment: stored data may hold several */
    List<AccessPoint> findPortalInstancesByHost(final String host);

    /**
     * Unlike {@link #getReferenceContext(String)}, which reads a failed lookup as no access point, this one fails: it guards
     * the hosts given to new access points.
     *
     * @return the access points on this host, whatever their target and environment
     */
    List<AccessPoint> findByHost(final String host);

    List<AccessPoint> getGatewayAccessPoints(final String environmentId);

    List<AccessPoint> getKafkaGatewayAccessPoints(final String environmentId);

    List<AccessPoint> getGammaConsoleAccessPoints();

    List<AccessPoint> getGammaConsoleAccessPoints(final String organizationId);
    AccessPoint getGammaConsoleAccessPoint(final String organizationId);
    AccessPoint getGammaApiAccessPoint(final String organizationId);
}
