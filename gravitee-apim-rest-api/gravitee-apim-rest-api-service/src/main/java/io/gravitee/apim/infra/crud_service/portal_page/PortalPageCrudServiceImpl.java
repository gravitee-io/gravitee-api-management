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
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.management.api.PortalPageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortalPageCrudServiceImpl implements PortalPageCrudService {

    @Lazy
    private final PortalPageRepository portalPageRepository;

    private final PortalPageAdapter portalPageAdapter = PortalPageAdapter.INSTANCE;

    @Override
    public List<PortalPage> findByIds(List<PageId> pageIds) {
        return portalPageRepository
            .findByIds(pageIds.stream().map(PageId::toString).toList())
            .stream()
            .map(portalPageAdapter::toEntity)
            .toList();
    }
}
