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
import {
    Badge,
    buildLinearBreadcrumbs,
    ContextSidebar,
    ContextToggleButton,
    Skeleton,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';

import { usePortal } from '../../../settings/hooks/usePortal';
import { usePortalsNavigation } from '../../config/navigation';
import {
    resolvePortalSettingsSectionLabel,
    truncatePortalBreadcrumbLabel,
} from '../../config/portalDetailNavigation';
import { getPortalPublishStatus } from '../../utils/portal-display';
import type { DeveloperPortal } from '../../types';
import { PortalDetailSidebarNav } from './PortalDetailSidebarNav';

function StatusBadge({ portal }: { readonly portal: DeveloperPortal }) {
    const status = getPortalPublishStatus(portal);
    if (status === 'Published') {
        return (
            <Badge variant="outline" className="w-fit border-success/30 text-success">
                Published
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className="w-fit text-muted-foreground">
            Draft
        </Badge>
    );
}

function PortalInfoHeader({
    portal,
    isLoading,
}: {
    readonly portal: DeveloperPortal | null;
    readonly isLoading: boolean;
}) {
    if (isLoading) {
        return (
            <div className="space-y-2.5 border-b px-3 pb-4 pt-4">
                <Skeleton className="h-4 w-36 rounded" />
                <Skeleton className="h-5 w-20 rounded-md" />
            </div>
        );
    }

    if (!portal) {
        return null;
    }

    return (
        <div className="space-y-2.5 border-b px-3 pb-4 pt-4">
            <div className="min-w-0 space-y-1">
                <p className="truncate text-sm font-semibold leading-snug text-foreground" title={portal.name}>
                    {portal.name}
                </p>
                <StatusBadge portal={portal} />
            </div>
        </div>
    );
}

export function PortalDetailLayout() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { pathname } = useLocation();
    const navigate = useNavigate();
    const { homePath, portalSettingsPath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading } = usePortal(portalId);
    const [contextExpanded, setContextExpanded] = useState(true);

    const settingsBasePath = useMemo(() => portalSettingsPath(portalId), [portalId, portalSettingsPath]);

    const sectionLabel = useMemo(() => resolvePortalSettingsSectionLabel(pathname), [pathname]);

    const breadcrumbs = useMemo(
        () =>
            buildLinearBreadcrumbs(navigate, [
                { label: 'Developer Portals', to: homePath },
                {
                    label: portal?.name ? truncatePortalBreadcrumbLabel(portal.name) : 'Loading…',
                    to: portalSettingsSectionPath(portalId, 'general'),
                },
                { label: sectionLabel },
            ]),
        [homePath, navigate, portal?.name, portalId, portalSettingsSectionPath, sectionLabel],
    );

    useLayoutConfig(
        {
            viewMode: 'context',
            contextExpanded,
            contextSidebar: (
                <ContextSidebar header={<PortalInfoHeader portal={portal ?? null} isLoading={loading} />}>
                    <PortalDetailSidebarNav basePath={settingsBasePath} />
                </ContextSidebar>
            ),
            leading: <ContextToggleButton expanded={contextExpanded} onToggle={() => setContextExpanded(v => !v)} />,
            breadcrumbs,
        },
        [contextExpanded, portal, loading, settingsBasePath, breadcrumbs],
    );

    return <Outlet />;
}
