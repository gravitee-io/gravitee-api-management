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
import type { ApiDocumentationItem, PortalVisibility, UpdateApiDocumentationItem } from '../../../types/apiDocumentation';

export interface DocumentationItemChanges {
    title?: string;
    visibility?: PortalVisibility;
    published?: boolean;
    order?: number;
    parentId?: string;
}

/**
 * The update replacing an item with the given changes. An update replaces the whole item, so everything not changed
 * is copied from the item — a page's or folder's external source above all, whose omission unlinks it, and a link's
 * address, without which the server refuses the update.
 */
export function toUpdatePayload(item: ApiDocumentationItem, changes: DocumentationItemChanges = {}): UpdateApiDocumentationItem {
    const fields = {
        title: changes.title ?? item.title,
        order: changes.order ?? item.order,
        published: changes.published ?? item.published,
        visibility: changes.visibility ?? item.visibility,
        parentId: 'parentId' in changes ? changes.parentId : item.parentId,
    };
    if (item.type === 'LINK') {
        return { type: 'LINK', ...fields, url: item.url };
    }
    return { type: item.type, ...fields, source: item.source };
}
