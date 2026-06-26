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
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { PortalShell } from '../../portal-shell/components/PortalShell';
import { getPortal } from '../storage/portals.storage';
import type { DeveloperPortal } from '../types';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';

export function PortalViewPage() {
    const { id, slug } = useParams<{ id: string; slug?: string }>();
    const navigate = useNavigate();
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        void getPortal(id).then(result => {
            setPortal(result);
            setLoading(false);
        });
    }, [id]);

    const getPagePath = useCallback(
        (pageSlug: string) => `/portals/${id}/${pageSlug}`,
        [id],
    );

    const handleNavigate = useCallback(
        (path: string, options?: { replace?: boolean }) => {
            navigate(path, options);
        },
        [navigate],
    );

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading portal…</p>;
    }

    if (!portal) {
        return (
            <NotFoundPage
                homePath="/"
                homeLabel="Back to dashboards"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
                className="min-h-screen"
            />
        );
    }

    return (
        <div className="flex h-screen flex-col overflow-hidden">
            <PortalShell
                portal={portal}
                layout={portal.layout}
                mode="preview"
                pageWidth="medium"
                onPortalChange={setPortal}
                slug={slug}
                getPagePath={getPagePath}
                onNavigate={handleNavigate}
            />
        </div>
    );
}
