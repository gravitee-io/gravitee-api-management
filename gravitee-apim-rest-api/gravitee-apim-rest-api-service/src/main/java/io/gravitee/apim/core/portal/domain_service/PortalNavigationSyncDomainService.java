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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.NavigationFolderMapper;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.NavigationAction;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlan;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanner;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationSyncDomainService {

    private static final PortalArea AREA = PortalArea.TOP_NAVBAR;
    private static final int MAX_CASCADE_DEPTH = 50;

    private final PortalNavigationItemCrudService crudService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalPageContentCrudService pageContentCrudService;

    public void sync(AuditInfo auditInfo, List<NavigationPath> desired) {
        final var currentFolders = queryService.search(
            PortalNavigationItemQueryCriteria.builder()
                .environmentId(auditInfo.environmentId())
                .area(AREA)
                .type(PortalNavigationItemType.FOLDER)
                .build()
        );
        final var plan = NavigationSyncPlanner.plan(desired == null ? List.of() : desired, currentFolders);
        execute(plan, auditInfo);
    }

    private void execute(NavigationSyncPlan plan, AuditInfo auditInfo) {
        final var byPath = new HashMap<String, PortalNavigationItemContainer>();
        plan.actions().forEach(action -> applyAction(action, byPath, auditInfo));
    }

    private void applyAction(NavigationAction action, Map<String, PortalNavigationItemContainer> byPath, AuditInfo auditInfo) {
        switch (action) {
            case FolderActions.FolderMutation m -> applyMutation(m, byPath, auditInfo);
            case FolderActions.DeleteFolder d -> cascadeDelete(d.item(), auditInfo.environmentId(), 0);
        }
    }

    private void applyMutation(
        FolderActions.FolderMutation mutation,
        Map<String, PortalNavigationItemContainer> byPath,
        AuditInfo auditInfo
    ) {
        final var df = mutation.desired();
        final var parent = df.parentPath() == null ? null : byPath.get(df.parentPath());
        final PortalNavigationFolder result = switch (mutation) {
            case FolderActions.CreateFolder c -> createFolder(c.desired(), parent, auditInfo);
            case FolderActions.UpdateFolder u -> applyUpdate(u.existing(), u.desired(), parent);
        };
        byPath.put(df.path(), result);
    }

    private PortalNavigationFolder createFolder(FolderActions.DesiredFolder df, PortalNavigationItemContainer parent, AuditInfo auditInfo) {
        final var create = CreatePortalNavigationItem.builder()
            .title(df.title())
            .segment(df.segment())
            .area(AREA)
            .type(PortalNavigationItemType.FOLDER)
            .order(df.order())
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
        return (PortalNavigationFolder) crudService.create(
            PortalNavigationItem.from(create, auditInfo.organizationId(), auditInfo.environmentId(), parent)
        );
    }

    private PortalNavigationFolder applyUpdate(
        PortalNavigationFolder existing,
        FolderActions.DesiredFolder desired,
        PortalNavigationItemContainer parent
    ) {
        if (NavigationFolderMapper.matches(existing, desired, parent)) return existing;
        NavigationFolderMapper.apply(desired, parent, existing);
        return (PortalNavigationFolder) crudService.update(existing);
    }

    private void cascadeDelete(PortalNavigationItem item, String environmentId, int depth) {
        if (depth > MAX_CASCADE_DEPTH) throw new IllegalStateException(
            "Maximum portal navigation nesting level of %d exceeded".formatted(MAX_CASCADE_DEPTH)
        );
        queryService
            .findByParentIdAndEnvironmentId(environmentId, item.getId())
            .forEach(child -> cascadeDelete(child, environmentId, depth + 1));
        if (item instanceof PortalNavigationPage page) pageContentCrudService.delete(page.getPortalPageContentId());
        crudService.delete(item.getId());
    }
}
