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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.OPENAPI_PORTAL_PAGE_CONTENT_CONFIGURATION_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class OpenApiPortalPageContentConfigurationUpgrader implements Upgrader {

    private static final String DEFAULT_CONFIGURATION = "{\"viewer\":\"REDOC\"}";

    private final PortalPageContentRepository portalPageContentRepository;

    public OpenApiPortalPageContentConfigurationUpgrader(@Lazy PortalPageContentRepository portalPageContentRepository) {
        this.portalPageContentRepository = portalPageContentRepository;
    }

    @Override
    public int getOrder() {
        return OPENAPI_PORTAL_PAGE_CONTENT_CONFIGURATION_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var openApiContents = portalPageContentRepository.findAllByType(PortalPageContent.Type.OPENAPI);
        var updated = 0;

        for (var content : openApiContents) {
            if (hasConfiguration(content)) {
                continue;
            }

            content.setConfiguration(DEFAULT_CONFIGURATION);
            portalPageContentRepository.update(content);
            updated++;
        }

        log.info("OpenAPI portal page content configuration upgrader completed. Updated {}/{} contents.", updated, openApiContents.size());
        return true;
    }

    private static boolean hasConfiguration(PortalPageContent content) {
        return content.getConfiguration() != null && !content.getConfiguration().isBlank();
    }
}
