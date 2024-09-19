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
package io.gravitee.apim.core.integration.model;

import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record IntegrationSubscription(String integrationId, Type type, String apiKey, Map<String, String> metadata) {
    public enum Type {
        API_KEY,
        OAUTH2,
    }

    public static IntegrationSubscription apiKey(String integrationId, String apiKey, Map<String, String> metadata) {
        return new IntegrationSubscription(integrationId, Type.API_KEY, apiKey, metadata);
    }

    public static IntegrationSubscription oAuth(String integrationId, Map<String, String> metadata) {
        return new IntegrationSubscription(integrationId, Type.OAUTH2, null, metadata);
    }
}
