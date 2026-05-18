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
package io.gravitee.apim.core.api.service_provider;

public interface ApiTemplateModelProvider {
    /**
     * Returns an API model object suitable for FreeMarker template rendering (e.g. {@code ${api.name}}).
     *
     * @param organizationId the organization the API belongs to
     * @param environmentId  the environment the API belongs to
     * @param apiId          the technical API ID
     * @return an object exposing API properties that FreeMarker can navigate
     */
    Object getApiTemplateModel(String organizationId, String environmentId, String apiId);
}
