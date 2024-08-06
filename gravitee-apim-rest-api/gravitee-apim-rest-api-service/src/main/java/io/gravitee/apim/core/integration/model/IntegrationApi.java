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

import io.gravitee.definition.model.federation.FederatedApi;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record IntegrationApi(
    String integrationId,
    String uniqueId,
    String id,
    String name,
    String description,
    String version,
    Map<String, String> connectionDetails,
    List<Plan> plans,
    List<Page> pages,
    Collection<Metadata> metadata
) {
    public record Plan(String id, String name, String description, PlanType type, List<String> characteristics) {}
    public enum PlanType {
        API_KEY,
        OAUTH2,
    }

    public record Page(PageType pageType, String content, String filename) {}
    public enum PageType {
        ASCIIDOC,
        ASYNCAPI,
        MARKDOWN,
        MARKDOWN_TEMPLATE,
        SWAGGER,
    }

    public record Metadata(String name, String value, io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat format) {}

    public FederatedApi.FederatedApiBuilder toFederatedApiDefinitionBuilder() {
        return FederatedApi.builder().providerId(id).apiVersion(version).name(name).server(connectionDetails);
    }
}
