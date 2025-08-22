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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PortalPageAdapter {

    public static PortalPage toDomain(io.gravitee.repository.management.model.PortalPage entity) {
        if (entity == null) return null;
        return PortalPageFactory.createGraviteeMarkdownPage(entity.getId(), entity.getContent());
    }

    public static io.gravitee.repository.management.model.PortalPage toEntity(PortalPage domain, List<PortalViewContext> contexts) {
        if (domain == null) return null;
        io.gravitee.repository.management.model.PortalPage entity = new io.gravitee.repository.management.model.PortalPage();
        entity.setId(domain.id().id().toString());
        entity.setContent(domain.pageContent().content());
        if (contexts != null) {
            entity.setContexts(contexts.stream().map(Enum::name).collect(Collectors.toList()));
        } else {
            entity.setContexts(Collections.emptyList());
        }
        return entity;
    }
}
