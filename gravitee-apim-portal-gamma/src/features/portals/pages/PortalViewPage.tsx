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
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { PortalShell } from '../../portal-shell/components/PortalShell';
import { usePortalTheme } from '../../theming/hooks/usePortalTheme';
import { useDarkMode } from '../../theming/hooks/useDarkMode';
import { getPortal } from '../storage/portals.storage';
import { seedCatalogDataIfEmpty } from '../storage/seed-catalog-data';
import type { DeveloperPortal } from '../types';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { PortalTenantPreviewProvider } from '../../tenants/context/PortalTenantPreviewContext';
import { TenantPreviewBanner } from '../../tenants/components/TenantPreviewBanner';
import { getPortalTenant } from '../../tenants/storage/portal-tenants.storage';
import { seedPortalTenantsForPortal } from '../../tenants/storage/seed-portal-tenants';
import type { PortalTenant } from '../../tenants/types/portal-tenant.types';

export function PortalViewPage() {
    const { id, slug } = useParams<{ id: string; slug?: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [previewTenant, setPreviewTenant] = useState<PortalTenant | undefined>();
    const [loading, setLoading] = useState(true);
    const themeState = usePortalTheme(id ?? '');
    const darkModeState = useDarkMode(themeState.theme.activeMode);
    const asTenantId = searchParams.get('asTenant');

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        void (async () => {
            await seedCatalogDataIfEmpty();
            await seedPortalTenantsForPortal(id);
            const result = await getPortal(id);
            setPortal(result);

            if (asTenantId) {
                const tenant = await getPortalTenant(asTenantId);
                setPreviewTenant(tenant?.portalId === id ? tenant : undefined);
            } else {
                setPreviewTenant(undefined);
            }

            setLoading(false);
        })();
    }, [id, asTenantId]);

    const getPagePath = useCallback(
        (pageSlug: string) => {
            const base = `/portals/${id}/${pageSlug}`;
            return asTenantId ? `${base}?asTenant=${asTenantId}` : base;
        },
        [id, asTenantId],
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

    const shell = (
        <PortalShell
            portal={portal}
            layout={portal.layout}
            mode="preview"
            pageWidth={portal.pageWidth}
            onPortalChange={setPortal}
            slug={slug}
            getPagePath={getPagePath}
            onNavigate={handleNavigate}
            theme={themeState.theme}
            themeReady={!themeState.loading}
            isDark={darkModeState.isDark}
            previewTenant={previewTenant}
        />
    );

    return (
        <div className="flex h-screen flex-col overflow-hidden">
            {previewTenant && id && (
                <TenantPreviewBanner tenantName={previewTenant.name} portalId={id} />
            )}
            {previewTenant ? (
                <PortalTenantPreviewProvider tenant={previewTenant}>{shell}</PortalTenantPreviewProvider>
            ) : (
                shell
            )}
        </div>
    );
}
