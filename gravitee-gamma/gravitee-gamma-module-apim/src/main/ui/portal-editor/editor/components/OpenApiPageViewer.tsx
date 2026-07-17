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
import { useEffect, useState } from 'react';

import { OpenApiRendererView } from '../../blocks/OpenApiBlock/OpenApiRendererView';
import type { OpenApiPageContent, PortalNavigationItem, PortalNavigationOpenApiPage } from '../../portals/types';
import { resolveOpenApiSpecContent } from '../utils/resolve-openapi-spec';
import styles from './OpenApiPageViewer.module.scss';

interface OpenApiPageViewerProps {
    readonly page: PortalNavigationOpenApiPage;
    readonly content: OpenApiPageContent;
    readonly navItems: readonly PortalNavigationItem[];
}

export function OpenApiPageViewer({ page, content, navItems }: OpenApiPageViewerProps) {
    const [specContent, setSpecContent] = useState(content.specContent);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);

        void (async () => {
            const resolved = await resolveOpenApiSpecContent(page.specSource, navItems, page.id, content.specContent);
            if (!cancelled) {
                setSpecContent(resolved);
                setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [content.specContent, navItems, page.id, page.specSource]);

    if (loading) {
        return <p className={styles.loading}>Loading OpenAPI spec…</p>;
    }

    return (
        <div className={styles.viewer}>
            <OpenApiRendererView renderer={content.renderer} specContent={specContent} />
        </div>
    );
}
