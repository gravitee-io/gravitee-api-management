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
import { useCallback } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

export function usePortalPageNavigation(portalId?: string) {
    const { id: routePortalId } = useParams<{ id: string }>();
    const location = useLocation();
    const navigate = useNavigate();
    const isEditMode = /\/edit(\/|$)/.test(location.pathname);
    const resolvedPortalId = portalId ?? routePortalId;

    const navigateToPageSlug = useCallback(
        (slug: string) => {
            if (!resolvedPortalId) {
                return;
            }
            const path = isEditMode
                ? `/portals/${resolvedPortalId}/edit/${slug}`
                : `/portals/${resolvedPortalId}/${slug}`;
            navigate(path);
        },
        [resolvedPortalId, isEditMode, navigate],
    );

    const getPagePath = useCallback(
        (slug: string) => {
            if (!resolvedPortalId) {
                return `/${slug}`;
            }
            return isEditMode
                ? `/portals/${resolvedPortalId}/edit/${slug}`
                : `/portals/${resolvedPortalId}/${slug}`;
        },
        [resolvedPortalId, isEditMode],
    );

    return { navigateToPageSlug, getPagePath };
}
