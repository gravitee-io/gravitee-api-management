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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.PORTAL_MENU_LINKS_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PortalMenuLinksUpgrader implements Upgrader {

    PortalMenuLinkRepository portalMenuLinkRepository;
    PortalPageRepository portalPageRepository;
    PortalPageContextRepository portalPageContextRepository;

    public PortalMenuLinksUpgrader(
            @Lazy PortalMenuLinkRepository portalMenuLinkRepository,
            @Lazy PortalPageRepository portalPageRepository,
            @Lazy PortalPageContextRepository portalPageContextRepository
    ) {
        this.portalMenuLinkRepository = portalMenuLinkRepository;
        this.portalPageRepository = portalPageRepository;
        this.portalPageContextRepository = portalPageContextRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrateMenuLinks);
    }

    @Override
    public int getOrder() {
        return PORTAL_MENU_LINKS_UPGRADER;
    }

    // shoud be idempotent if already created
    private boolean migrateMenuLinks() throws TechnicalException {
        for (final var menuLink : portalMenuLinkRepository.findAll()) {
            final var now = new Date();
            final var portalPage = portalPageRepository.create(
                    PortalPage.builder()
                            .id(UuidString.generateRandom())
                            .environmentId(menuLink.getEnvironmentId())
                            .name(menuLink.getName())
                            .content(menuLink.getTarget())
                            .createdAt(now)
                            .updatedAt(now)
                            .build()
            );
            portalPageContextRepository.create(
                    PortalPageContext.builder()
                            .id(UuidString.generateRandom())
                            .pageId(portalPage.getId())
                            .contextType(PortalPageContextType.TOPBAR)
                            .environmentId(menuLink.getEnvironmentId())
                            .published(true)
                            .build()
            );
        }
        return true;
    }
}
