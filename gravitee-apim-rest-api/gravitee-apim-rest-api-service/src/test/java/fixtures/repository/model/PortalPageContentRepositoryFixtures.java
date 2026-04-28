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
package fixtures.repository.model;

import io.gravitee.repository.management.model.PortalPageContent;

public class PortalPageContentRepositoryFixtures {

    private static final String ORGANIZATION_ID = "ORG";
    private static final String ENVIRONMENT_ID = "ENV";

    private PortalPageContentRepositoryFixtures() {}

    public static PortalPageContent anOpenApiPageContent(String id, String content, String configuration) {
        return PortalPageContent.builder()
            .id(id)
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .type(PortalPageContent.Type.OPENAPI)
            .content(content)
            .configuration(configuration)
            .build();
    }
}
