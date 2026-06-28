/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { OpenApiSpecSource, PortalNavigationItem } from '../../portals/types';
import { findApiAncestor } from '../../portal-shell/utils/find-api-ancestor';
import { fetchOpenApiSpecFromUrl, getOpenApiSpec } from '../services/openapi.service';

export async function resolveOpenApiSpecContent(
    specSource: OpenApiSpecSource,
    navItems: readonly PortalNavigationItem[],
    navigationItemId: string,
    fallbackContent = '',
): Promise<string> {
    switch (specSource.type) {
        case 'INLINE':
            return specSource.content;
        case 'URL':
            try {
                const spec = await fetchOpenApiSpecFromUrl(specSource.url);
                return spec.content;
            } catch {
                return fallbackContent;
            }
        case 'API': {
            const apiId = specSource.apiId || findApiAncestor(navItems, navigationItemId)?.apiId;
            if (!apiId) {
                return fallbackContent;
            }
            const spec = await getOpenApiSpec(apiId);
            return spec.content;
        }
        default:
            return fallbackContent;
    }
}
