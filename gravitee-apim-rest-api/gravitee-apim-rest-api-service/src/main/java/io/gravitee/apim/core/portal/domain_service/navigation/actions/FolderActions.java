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
package io.gravitee.apim.core.portal.domain_service.navigation.actions;

import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.Slug;

public final class FolderActions {

    private FolderActions() {}

    public sealed interface FolderAction extends NavigationAction permits FolderMutation, DeleteFolder {}

    public sealed interface FolderMutation extends FolderAction permits CreateFolder, UpdateFolder {
        DesiredFolder desired();
    }

    public record CreateFolder(DesiredFolder desired) implements FolderMutation {}

    public record UpdateFolder(PortalNavigationFolder existing, DesiredFolder desired) implements FolderMutation {}

    public record DeleteFolder(PortalNavigationItem item) implements FolderAction {}

    public record DesiredFolder(
        String path,
        String parentPath,
        Slug segment,
        String title,
        int order,
        PortalVisibility visibility,
        boolean published
    ) {}
}
