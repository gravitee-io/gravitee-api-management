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
package io.gravitee.apim.infra.query_service.portal_page;

import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalPageQueryServiceImpl implements PortalPageQueryService {

    private final PortalPageRepository pageRepository;

    private final PortalPageContextRepository contextRepository;

    private final PortalPageAdapter pageAdapter = PortalPageAdapter.INSTANCE;

    public PortalPageQueryServiceImpl(@Lazy PortalPageRepository pageRepository, @Lazy PortalPageContextRepository contextRepository) {
        this.pageRepository = pageRepository;
        this.contextRepository = contextRepository;
    }

    @Override
    public List<PortalPageWithViewDetails> findByEnvironmentIdAndContext(String environmentId, PortalViewContext context) {
        try {
            var pagesContext = contextRepository
                .findAllByContextTypeAndEnvironmentId(PortalPageContextType.valueOf(context.name()), environmentId)
                .stream()
                .collect(Collectors.toMap(PortalPageContext::getPageId, Function.identity()));
            var pages = pageRepository
                .findByIds(pagesContext.keySet().stream().toList())
                .stream()
                .collect(Collectors.toMap(PortalPage::getId, p -> p));
            return pages
                .entrySet()
                .stream()
                .map(e -> new PortalPageWithViewDetails(pageAdapter.map(e.getValue()), pageAdapter.map(pagesContext.get(e.getKey()))))
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while trying to find portal pages viewDetails by environment ID and viewDetails",
                e
            );
        }
    }

    @Override
    public PortalPageWithViewDetails findById(PageId pageId) {
        try {
            var page = pageRepository.findById(pageId.toString()).orElse(null);
            var context = contextRepository.findByPageId(pageId.toString());
            return new PortalPageWithViewDetails(pageAdapter.map(page), pageAdapter.map(context));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException();
        }
    }
}
