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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * A folder driven by a file-listing source mirrors a remote repository: fetching it re-runs the
 * import, which deletes every child the import does not know about. Such folders are only created
 * through the import endpoint, which stamps their source; attaching a file-listing source to any
 * other folder by update is rejected, so hand-made children can never end up under an import's
 * authority. An import-managed folder keeps accepting updates of its own source.
 */
@RequiredArgsConstructor
public class FileListingSourceOnFolderRule implements UpdatePortalNavigationItemValidationRule {

    private final Predicate<PortalNavigationItemSource> supportsFileListing;

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem.getType() == PortalNavigationItemType.FOLDER && toUpdate.getSource() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        var importManaged = existingItem.getSource() != null && existingItem.getSource().isSubtreeImport();
        if (supportsFileListing.test(toUpdate.getSource())) {
            if (!importManaged) {
                throw InvalidPortalNavigationItemDataException.fileListingSourceOnlyCreatedByImport(existingItem.getId().json());
            }
            return;
        }
        // The symmetric hole: fetching an import-managed folder re-runs the import, which starts by
        // listing the source — a source that cannot list files would break the folder at the next fetch
        if (importManaged) {
            throw InvalidPortalNavigationItemDataException.importManagedFolderNeedsFileListingSource(existingItem.getId().json());
        }
    }
}
