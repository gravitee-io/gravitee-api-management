/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DocumentationSystemFolderUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DocumentationSystemFolderUpgrader.class);

    @Autowired
    private PageService pageService;

    @Autowired
    private ApiService apiService;

    @Override
    public boolean upgrade(ExecutionContext executionContext) {
        PageQuery query = new PageQuery.Builder().type(PageType.SYSTEM_FOLDER).build();
        // searching for system folders.
        if (pageService.search(executionContext.getEnvironmentId(), query).isEmpty()) {
            logger.info("No system folders found. Add system folders in documentation, for portal and each API.");

            GraviteeContext.setCurrentEnvironment(GraviteeContext.getDefaultEnvironment());
            // Portal documentation
            Map<SystemFolderType, String> systemFolderIds = pageService.initialize(executionContext);

            String headerSystemFolderId = systemFolderIds.get(SystemFolderType.HEADER);
            String topFooterSystemFolderId = systemFolderIds.get(SystemFolderType.TOPFOOTER);

            // Create link to existing documentation in footer
            List<PageEntity> pagesToLink = pageService
                .search(executionContext.getEnvironmentId(), new PageQuery.Builder().homepage(false).rootParent(true).build())
                .stream()
                .filter(p -> PageType.SWAGGER.name().equals(p.getType()) || PageType.MARKDOWN.name().equals(p.getType()))
                .collect(Collectors.toList());

            if (!pagesToLink.isEmpty()) {
                PageEntity docFolder = createFolder(executionContext, topFooterSystemFolderId, "Documentation");
                pagesToLink.forEach(
                    page ->
                        createLink(executionContext, docFolder.getId(), page.getName(), page.getId(), "page", Boolean.FALSE, Boolean.TRUE)
                );
            }

            // Create link to root documentation folder in header
            createLink(executionContext, headerSystemFolderId, "Documentation", "root", "page", Boolean.TRUE, Boolean.FALSE);

            // Apis documentation
            apiService
                .findAllLightByEnvironment(executionContext, executionContext.getEnvironmentId())
                .forEach(api -> pageService.createSystemFolder(executionContext, api.getId(), SystemFolderType.ASIDE, 0));
        }
        return true;
    }

    private PageEntity createFolder(ExecutionContext executionContext, String parentId, String name) {
        NewPageEntity newFolder = new NewPageEntity();
        newFolder.setName(name);
        newFolder.setPublished(true);
        newFolder.setType(PageType.FOLDER);
        newFolder.setParentId(parentId);
        newFolder.setVisibility(Visibility.PUBLIC);

        return pageService.createPage(executionContext, newFolder);
    }

    private void createLink(
        ExecutionContext executionContext,
        String parentId,
        String name,
        String resourceRef,
        String resourceType,
        Boolean isFolder,
        Boolean inherit
    ) {
        NewPageEntity newLink = new NewPageEntity();
        newLink.setContent(resourceRef);
        newLink.setName(name);
        newLink.setPublished(true);
        newLink.setType(PageType.LINK);
        newLink.setParentId(parentId);
        newLink.setVisibility(Visibility.PUBLIC);

        Map<String, String> configuration = new HashMap<>();
        configuration.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, resourceType);
        if (isFolder != null) {
            configuration.put(PageConfigurationKeys.LINK_IS_FOLDER, String.valueOf(isFolder));
        }
        if (inherit != null) {
            configuration.put(PageConfigurationKeys.LINK_INHERIT, String.valueOf(inherit));
        }

        newLink.setConfiguration(configuration);

        pageService.createPage(executionContext, newLink);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
