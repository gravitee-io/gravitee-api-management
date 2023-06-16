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
package io.gravitee.rest.api.service.v4;

import io.gravitee.rest.api.model.v4.license.GraviteeLicenseEntity;

public interface GraviteeLicenseService {
    String FEATURE_CUSTOM_ROLES = "apim-custom-roles";
    String FEATURE_DEBUG_MODE = "apim-debug-mode";
    String FEATURE_SHARDING_TAGS = "apim-sharding-tags";
    String FEATURE_OPEN_ID_CONNECT_SSO = "apim-openid-connect-sso";
    String FEATURE_AUDIT_TRAIL = "apim-audit-trail";
    String FEATURE_DCR_REGISTRATION = "apim-dcr-registration";
    String FEATURE_ENTRYPOINT_WEBHOOK = "apim-en-entrypoint-webhook";
    String FEATURE_ENTRYPOINT_HTTP_GET = "apim-en-entrypoint-http-get";
    String FEATURE_ENTRYPOINT_WEBSOCKET = "apim-en-entrypoint-websocket";
    String FEATURE_ENTRYPOINT_HTTP_POST = "apim-en-entrypoint-http-post";
    String FEATURE_ENTRYPOINT_SSE = "apim-en-entrypoint-sse";
    String FEATURE_ENDPOINT_MQTT5 = "apim-en-endpoint-mqtt5";
    String FEATURE_ENDPOINT_KAFKA = "apim-en-endpoint-kafka";

    GraviteeLicenseEntity getLicense();

    boolean isFeatureEnabled(String featureName);

    default boolean isFeatureMissing(String featureName) {
        return !isFeatureEnabled(featureName);
    }
}
