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
import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ensureDefaultPageForPortal } from '../storage/ensure-default-page';
import { getNavItems } from '../storage/navigation-items.storage';
import { findFirstPageNavItem, findFirstVisiblePageNavItem } from '../utils/slug';

interface PortalFirstPageRedirectProps {
    readonly mode: 'view' | 'edit';
}

export function PortalFirstPageRedirect({ mode }: PortalFirstPageRedirectProps) {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    useEffect(() => {
        if (!id) {
            return;
        }

        let cancelled = false;

        void (async () => {
            await ensureDefaultPageForPortal(id);
            const items = await getNavItems(id);
            const firstPage = mode === 'edit'
                ? findFirstPageNavItem(items)
                : findFirstVisiblePageNavItem(items);

            if (cancelled || !firstPage) {
                return;
            }

            const path =
                mode === 'edit'
                    ? `/portals/${id}/edit/${firstPage.slug}`
                    : `/portals/${id}/${firstPage.slug}`;
            navigate(path, { replace: true });
        })();

        return () => {
            cancelled = true;
        };
    }, [id, mode, navigate]);

    return <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>;
}
