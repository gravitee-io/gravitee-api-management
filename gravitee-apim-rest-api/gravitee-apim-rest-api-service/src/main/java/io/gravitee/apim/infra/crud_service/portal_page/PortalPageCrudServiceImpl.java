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
package io.gravitee.apim.infra.crud_service.portal_page;

import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalPageCrudServiceImpl implements PortalPageCrudService {

    @Lazy
    private final PortalPageRepository portalPageRepository;

    @Lazy
    private final PortalPageContextRepository portalPageContextRepository;

    private final PortalPageAdapter portalPageAdapter;

    @Override
    public List<PortalPage> byPortalViewContext(String environmentId, PortalViewContext portalViewContext) {
        try {
            var contexts = portalPageContextRepository.findAllByContextTypeAndEnvironmentId(
                io.gravitee.repository.management.model.PortalPageContextType.valueOf(portalViewContext.name()),
                environmentId
            );
            if (contexts == null || contexts.isEmpty()) {
                return List.of();
            }
            return contexts
                .stream()
                .map(ctx -> {
                    try {
                        return portalPageRepository.findById(ctx.getPageId());
                    } catch (TechnicalException e) {
                        return Optional.<io.gravitee.repository.management.model.PortalPage>empty();
                    }
                })
                .flatMap(Optional::stream)
                .map(portalPageAdapter::toEntity)
                .toList();
        } catch (io.gravitee.repository.exceptions.TechnicalException e) {
            return List.of();
        }
    }

    @Override
    public boolean portalViewContextExists(String environmentId, PortalViewContext key) {
        try {
            var contexts = portalPageContextRepository.findAllByContextTypeAndEnvironmentId(
                io.gravitee.repository.management.model.PortalPageContextType.valueOf(key.name()),
                environmentId
            );
            return contexts != null && !contexts.isEmpty();
        } catch (io.gravitee.repository.exceptions.TechnicalException e) {
            // Handle or log exception as needed
            return false;
        }
    }
}
