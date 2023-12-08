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
package stub;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.List;
import java.util.Optional;

public class AccessPointQueryServiceStub implements AccessPointQueryService {

    @Override
    public Optional<ReferenceContext> getReferenceContext(String host) {
        return Optional.empty();
    }

    @Override
    public List<AccessPoint> getConsoleAccessPoints() {
        return List.of();
    }

    @Override
    public List<AccessPoint> getConsoleAccessPoints(final String organizationId) {
        return List.of();
    }

    @Override
    public AccessPoint getConsoleAccessPoint(final String organizationId) {
        return null;
    }

    @Override
    public AccessPoint getConsoleApiAccessPoint(final String organizationId) {
        return null;
    }

    @Override
    public List<AccessPoint> getPortalAccessPoints() {
        return List.of();
    }

    @Override
    public List<AccessPoint> getPortalAccessPoints(final String environmentId) {
        return List.of();
    }

    @Override
    public AccessPoint getPortalAccessPoint(final String environmentId) {
        return null;
    }

    @Override
    public AccessPoint getPortalApiAccessPoint(final String environmentId) {
        return null;
    }

    @Override
    public List<AccessPoint> getGatewayAccessPoints(final String environmentId) {
        return List.of();
    }
}
