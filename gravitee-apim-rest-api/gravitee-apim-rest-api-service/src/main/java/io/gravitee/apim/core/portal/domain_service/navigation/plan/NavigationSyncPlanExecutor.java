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
package io.gravitee.apim.core.portal.domain_service.navigation.plan;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.navigation.NavigationFolderMapper;
import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public final class NavigationSyncPlanExecutor {

    private static final int MAX_CASCADE_DEPTH = 50;

    private final PortalNavigationItemCrudService crudService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalPageContentCrudService pageContentCrudService;

    public void execute(
        NavigationSyncPlan plan,
        AuditInfo auditInfo,
        PortalNavigationItemContainer root,
        Function<String, PortalNavigationItemId> idFactory,
        DeleteStrategy strategy
    ) {
        final var byPath = new HashMap<String, PortalNavigationItemContainer>();
        plan
            .actions()
            .forEach(action -> {
                switch (action) {
                    case FolderActions.FolderMutation m -> applyMutation(m, byPath, auditInfo, root, idFactory);
                    case FolderActions.DeleteFolder d -> applyDelete(d, auditInfo.environmentId(), strategy);
                }
            });
    }

    private void applyMutation(
        FolderActions.FolderMutation mutation,
        Map<String, PortalNavigationItemContainer> byPath,
        AuditInfo auditInfo,
        PortalNavigationItemContainer root,
        Function<String, PortalNavigationItemId> idFactory
    ) {
        final var df = mutation.desired();
        final var parent = df.parentPath() == null ? root : byPath.get(df.parentPath());
        final PortalNavigationFolder result = switch (mutation) {
            case FolderActions.CreateFolder c -> createFolder(c.desired(), parent, auditInfo, idFactory);
            case FolderActions.UpdateFolder u -> applyUpdate(u.existing(), u.desired(), parent);
        };
        byPath.put(df.path(), result);
    }

    private void applyDelete(FolderActions.DeleteFolder delete, String environmentId, DeleteStrategy strategy) {
        cascadeDelete(delete.item(), environmentId, strategy, 0);
    }

    private void cascadeDelete(PortalNavigationItem item, String environmentId, DeleteStrategy strategy, int depth) {
        if (depth > MAX_CASCADE_DEPTH) throw new IllegalStateException(
            "Maximum portal navigation nesting level of %d exceeded".formatted(MAX_CASCADE_DEPTH)
        );
        if (strategy.shouldSkip().test(item)) return;
        queryService
            .findByParentIdAndEnvironmentId(environmentId, item.getId())
            .forEach(child -> cascadeDelete(child, environmentId, strategy, depth + 1));
        if (strategy.alsoDeleteContent() && item instanceof PortalNavigationPage page && page.getPortalPageContentId() != null) {
            pageContentCrudService.delete(page.getPortalPageContentId());
        }
        crudService.delete(item.getId());
    }

    private PortalNavigationFolder createFolder(
        FolderActions.DesiredFolder df,
        PortalNavigationItemContainer parent,
        AuditInfo auditInfo,
        Function<String, PortalNavigationItemId> idFactory
    ) {
        final var folderId = idFactory.apply(df.path());
        final var create = CreatePortalNavigationItem.builder()
            .id(folderId)
            .title(df.title())
            .segment(df.segment().value())
            .area(PortalArea.TOP_NAVBAR)
            .type(PortalNavigationItemType.FOLDER)
            .order(df.order())
            .visibility(df.visibility())
            .published(df.published())
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
        if (NavigationFolderMapper.matches(existing, desired, parent == null ? null : parent.getId())) return existing;
        NavigationFolderMapper.apply(desired, parent, existing);
        return (PortalNavigationFolder) crudService.update(existing);
    }
}
