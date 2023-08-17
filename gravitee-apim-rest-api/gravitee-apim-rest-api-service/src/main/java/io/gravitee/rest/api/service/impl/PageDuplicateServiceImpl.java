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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.PageDuplicateService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.PageConverter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PageDuplicateServiceImpl implements PageDuplicateService {

    private final PageService pageService;
    private final PageConverter pageConverter;

    public PageDuplicateServiceImpl(PageService pageService, PageConverter pageConverter) {
        this.pageService = pageService;
        this.pageConverter = pageConverter;
    }

    @Override
    public Map<String, String> duplicatePages(
        ExecutionContext executionContext,
        String sourceApiId,
        String duplicatedApiId,
        String userId
    ) {
        final List<PageEntity> pagesToDuplicate = pageService.search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder().api(sourceApiId).build(),
            true
        );

        var root = new PageServiceImpl.PageEntityTreeNode(new PageEntity()).appendListToTree(pagesToDuplicate);
        return duplicateChildrenPages(executionContext, userId, duplicatedApiId, null, root.children);
    }

    private Map<String, String> duplicateChildrenPages(
        final ExecutionContext executionContext,
        String userId,
        String duplicateApiId,
        String parentId,
        List<PageServiceImpl.PageEntityTreeNode> children
    ) {
        Map<String, String> idsMap = new HashMap<>();

        for (final PageServiceImpl.PageEntityTreeNode child : children) {
            var sourcePageId = child.data.getId();
            var newPageId = UuidString.generateForEnvironment(executionContext.getEnvironmentId(), duplicateApiId, sourcePageId);
            var newPageEntity = pageConverter.toNewPageEntity(
                child.data.toBuilder().lastContributor(userId).parentId(parentId).build(),
                true
            );

            var duplicatedPage = pageService.createPage(executionContext, duplicateApiId, newPageEntity, newPageId);
            idsMap.put(sourcePageId, duplicatedPage.getId());

            if (child.children != null && !child.children.isEmpty()) {
                idsMap.putAll(
                    this.duplicateChildrenPages(executionContext, userId, duplicateApiId, duplicatedPage.getId(), child.children)
                );
            }
        }

        return idsMap;
    }
}
