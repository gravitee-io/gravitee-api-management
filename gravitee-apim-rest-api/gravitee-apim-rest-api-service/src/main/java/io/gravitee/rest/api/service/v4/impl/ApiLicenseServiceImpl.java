/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiLicenseServiceImpl implements ApiLicenseService {

    private final GraviteeLicenseService graviteeLicenseService;
    private final ApiSearchService apiSearchService;

    private static final Map<String, String> ENDPOINT_FEATURES = Map.of(
        "kafka",
        GraviteeLicenseService.FEATURE_ENDPOINT_KAFKA,
        "mqtt5",
        GraviteeLicenseService.FEATURE_ENDPOINT_MQTT5
    );

    private static final Map<String, String> ENTRYPOINT_FEATURES = Map.of(
        "http-post",
        GraviteeLicenseService.FEATURE_ENTRYPOINT_HTTP_POST,
        "http-get",
        GraviteeLicenseService.FEATURE_ENTRYPOINT_HTTP_GET,
        "webhook",
        GraviteeLicenseService.FEATURE_ENTRYPOINT_WEBHOOK,
        "websocket",
        GraviteeLicenseService.FEATURE_ENTRYPOINT_WEBSOCKET,
        "sse",
        GraviteeLicenseService.FEATURE_ENTRYPOINT_SSE
    );

    public ApiLicenseServiceImpl(GraviteeLicenseService graviteeLicenseService, ApiSearchService apiSearchService) {
        this.graviteeLicenseService = graviteeLicenseService;
        this.apiSearchService = apiSearchService;
    }

    @Override
    public void checkLicense(ExecutionContext executionContext, String apiId) throws ForbiddenFeatureException {
        checkLicense(executionContext, apiSearchService.findGenericById(executionContext, apiId));
    }

    @Override
    public void checkLicense(ExecutionContext executionContext, GenericApiEntity genericApiEntity) {
        if (!DefinitionVersion.V4.equals(genericApiEntity.getDefinitionVersion())) {
            return;
        }

        var apiEntity = (ApiEntity) genericApiEntity;
        checkEndpointGroups(apiEntity.getEndpointGroups());
        checkListeners(apiEntity.getListeners());
    }

    private void checkEndpointGroups(List<EndpointGroup> endpointGroups) {
        if (endpointGroups == null) {
            return;
        }

        endpointGroups.forEach(this::checkEndpointGroup);
    }

    private void checkEndpointGroup(EndpointGroup endpointGroup) {
        Optional
            .ofNullable(ENDPOINT_FEATURES.get(endpointGroup.getType()))
            .filter(graviteeLicenseService::isFeatureMissing)
            .ifPresent(feature -> {
                throw new ForbiddenFeatureException(feature);
            });
    }

    private void checkListeners(List<Listener> listeners) {
        if (listeners == null) {
            return;
        }

        listeners.forEach(this::checkListener);
    }

    private void checkListener(Listener listener) {
        if (listener.getEntrypoints() == null) {
            return;
        }

        listener.getEntrypoints().forEach(this::checkEntrypoint);
    }

    private void checkEntrypoint(Entrypoint entrypoint) {
        Optional
            .ofNullable(ENTRYPOINT_FEATURES.get(entrypoint.getType()))
            .filter(graviteeLicenseService::isFeatureMissing)
            .ifPresent(feature -> {
                throw new ForbiddenFeatureException(feature);
            });
    }
}
