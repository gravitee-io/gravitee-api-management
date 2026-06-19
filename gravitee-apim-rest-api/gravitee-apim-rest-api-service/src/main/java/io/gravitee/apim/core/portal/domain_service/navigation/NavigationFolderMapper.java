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
package io.gravitee.apim.core.portal.domain_service.navigation;

import io.gravitee.apim.core.portal.domain_service.navigation.actions.FolderActions;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import jakarta.annotation.Nullable;
import java.util.Objects;

public final class NavigationFolderMapper {

    private NavigationFolderMapper() {}

    public static boolean matches(
        PortalNavigationFolder existing,
        FolderActions.DesiredFolder desired,
        @Nullable PortalNavigationItemId parentId
    ) {
        return (
            Objects.equals(existing.getTitle(), desired.title()) &&
            Objects.equals(existing.getSegment(), desired.segment().value()) &&
            existing.getOrder() == desired.order() &&
            Objects.equals(existing.getParentId(), parentId)
        );
    }

    public static void apply(FolderActions.DesiredFolder source, PortalNavigationItemContainer parent, PortalNavigationFolder target) {
        target.setTitle(source.title());
        target.setSegment(source.segment().value());
        target.setOrder(source.order());
        if (parent == null) {
            target.markAsRoot();
        } else {
            target.updateParent(parent);
        }
    }
}
