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

import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EntrypointConnectorPluginService extends ConnectorPluginService {
    /**
     * Retrieve the subscription schema of the entrypoint if it exists.
     * @param connectorId is the id of the entrypoint
     * @return the subscription schema as a string, else {@code null}
     */
    default String getSubscriptionSchema(String connectorId) {
        return getSubscriptionSchema(connectorId, null);
    }

    /**
     * Retrieve the subscription schema of the entrypoint if it exists.
     * @param connectorId is the id of the entrypoint
     * @param schemaDisplayFormat is the format of the schema to return. Can be "gv-schema-form" or empty for default format.
     * @return the subscription schema as a string, else {@code null}
     */
    String getSubscriptionSchema(String connectorId, final SchemaDisplayFormat schemaDisplayFormat);

    String validateEntrypointSubscriptionConfiguration(final String entrypointId, final String configuration);
}
