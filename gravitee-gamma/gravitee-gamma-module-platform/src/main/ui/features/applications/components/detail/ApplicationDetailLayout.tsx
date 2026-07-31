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
import { permissionService } from '@gravitee/gamma-modules-sdk';
import {
    Avatar,
    AvatarFallback,
    AvatarImage,
    Badge,
    ContextSidebar,
    ContextToggleButton,
    Skeleton,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { AppWindowIcon, CircleCheckIcon, CircleStopIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { ApplicationDetailNoSectionsAvailable } from './ApplicationDetailNoSectionsAvailable';
import { ApplicationDetailPermissionsError } from './ApplicationDetailPermissionsError';
import { ApplicationDetailProtectedOutlet } from './ApplicationDetailProtectedOutlet';
import { ApplicationDetailSidebarNav } from './ApplicationDetailSidebarNav';
import { APPLICATION_NAV_GROUPS, resolveApplicationDetailLandingPath } from '../../../../config/applicationDetailNavigation';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { usePermissionServiceSnapshot } from '../../../shared/hooks/usePermissionServiceSnapshot';
import { truncateLabel } from '../../../shared/utils/truncateLabel';
import { ApplicationDetailContext, useApplicationDetailContext } from '../../context/ApplicationDetailContext';
import { useApplicationDetail } from '../../hooks/useApplicationDetail';
import { useApplicationPermissions } from '../../hooks/useApplicationPermissions';
import type { ApplicationListItem, ApplicationStatus } from '../../types/application';
import { formatApplicationOwnerLabel, formatApplicationSecurityTypeLabel } from '../../utils/applicationFormatters';

function StatusBadge({ status }: { status: ApplicationStatus }) {
    if (status === 'ACTIVE') {
        return (
            <Badge className="gap-1 h-5 w-fit px-1.5 text-xs font-medium bg-success/10 text-success border-transparent">
                <CircleCheckIcon className="size-3" />
                Active
            </Badge>
        );
    }
    return (
        <Badge variant="secondary" className="gap-1 h-5 w-fit px-1.5 text-xs font-medium">
            <CircleStopIcon className="size-3" />
            Archived
        </Badge>
    );
}

function ApplicationInfoHeader({ application, isLoading }: { application: ApplicationListItem | null; isLoading: boolean }) {
    if (isLoading) {
        return (
            <div className="space-y-2.5 border-b px-3 pb-4 pt-4">
                <div className="flex min-w-0 items-start gap-2.5">
                    <Skeleton className="size-8 shrink-0 rounded-lg" />
                    <div className="min-w-0 flex-1 space-y-1.5">
                        <Skeleton className="h-3.5 w-32 rounded" />
                        <Skeleton className="h-6 w-14 rounded-md" />
                    </div>
                </div>
                <Skeleton className="h-3 w-full rounded" />
                <div className="flex gap-1.5">
                    <Skeleton className="h-5 w-14 rounded-md" />
                    <Skeleton className="h-5 w-24 rounded-md" />
                </div>
            </div>
        );
    }

    if (!application) return null;

    const ownerLabel = formatApplicationOwnerLabel(application.owner);
    const securityTypeLabel = formatApplicationSecurityTypeLabel(application);
    const description = application.description?.trim();
    const pictureSrc = application.picture ?? application.picture_url;

    return (
        <div className="space-y-2.5 border-b px-3 pb-4 pt-4">
            <div className="flex min-w-0 items-start gap-2.5">
                <Avatar className="size-8 shrink-0 rounded-lg">
                    {pictureSrc ? (
                        <AvatarImage src={pictureSrc} alt={`${application.name} logo`} className="rounded-lg object-cover" />
                    ) : null}
                    <AvatarFallback className="rounded-lg border border-primary/30 bg-primary/5 text-primary">
                        <AppWindowIcon className="size-4" aria-hidden />
                    </AvatarFallback>
                </Avatar>
                <div className="min-w-0 flex-1 space-y-1">
                    <p className="truncate text-sm font-semibold leading-snug text-foreground" title={application.name}>
                        {application.name}
                    </p>
                    <StatusBadge status={application.status} />
                </div>
            </div>

            {description ? (
                <p className="line-clamp-2 break-all text-xs leading-relaxed text-muted-foreground" title={description}>
                    {description}
                </p>
            ) : null}

            <div className="flex flex-wrap items-center gap-1">
                <span
                    className="inline-flex h-5 max-w-full items-center truncate border border-border bg-background px-1.5 text-xs font-normal text-foreground"
                    title={securityTypeLabel}
                >
                    {securityTypeLabel}
                </span>
                {ownerLabel ? (
                    <span
                        className="inline-flex h-5 max-w-full items-center truncate bg-muted px-1.5 text-xs font-normal text-foreground"
                        title={ownerLabel}
                    >
                        {ownerLabel}
                    </span>
                ) : null}
            </div>
        </div>
    );
}

interface ApplicationDetailLayoutBodyProps {
    readonly application: ApplicationListItem | null | undefined;
    readonly isLoading: boolean;
    readonly permissionsReady: boolean;
    readonly refetchPermissions: () => void;
    readonly basePath: string;
}

function ApplicationDetailLayoutBody({
    application,
    isLoading,
    permissionsReady,
    refetchPermissions,
    basePath,
}: ApplicationDetailLayoutBodyProps) {
    const [contextExpanded, setContextExpanded] = useState(true);
    const applicationsListHref = resolveListHrefFromDetailBasePath(basePath);

    useLayoutConfig(
        {
            viewMode: 'context',
            contextExpanded,
            contextSidebar: (
                <ContextSidebar header={<ApplicationInfoHeader application={application ?? null} isLoading={isLoading} />}>
                    <ApplicationDetailSidebarNav
                        groups={APPLICATION_NAV_GROUPS}
                        basePath={basePath}
                        permissionsReady={permissionsReady && !isLoading}
                    />
                </ContextSidebar>
            ),
            leading: <ContextToggleButton expanded={contextExpanded} onToggle={() => setContextExpanded(v => !v)} />,
            breadcrumbs: [
                { label: 'Applications', href: applicationsListHref },
                {
                    label: application?.name ? truncateLabel(application.name) : 'Loading…',
                },
            ],
        },
        [contextExpanded, application, isLoading, basePath, permissionsReady, applicationsListHref],
    );

    return (
        <ApplicationDetailContext.Provider
            value={{
                application: application ?? null,
                isLoading,
                permissionsReady,
                permissionsError: false,
                refetchPermissions,
            }}
        >
            <div className="min-h-0 flex-1">
                <ApplicationDetailProtectedOutlet />
            </div>
        </ApplicationDetailContext.Provider>
    );
}

export function ApplicationDetailLayout() {
    const navigate = useNavigate();
    const { applicationId } = useParams<{ applicationId: string }>();
    const basePath = useDetailBasePath('applications', applicationId);
    const { data: application, isLoading, isError } = useApplicationDetail(applicationId);
    const { permissionsReady, isError: permissionsError, refetch: refetchPermissions } = useApplicationPermissions(applicationId);

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">
                    Failed to load application. It may have been deleted or you may not have access.
                </p>
            </div>
        );
    }

    if (permissionsError) {
        const applicationsListPath = resolveListHrefFromDetailBasePath(basePath);
        return <ApplicationDetailPermissionsError onRetry={refetchPermissions} onBack={() => navigate(applicationsListPath)} />;
    }

    return (
        <ApplicationDetailLayoutBody
            application={application}
            isLoading={isLoading}
            permissionsReady={permissionsReady}
            refetchPermissions={refetchPermissions}
            basePath={basePath}
        />
    );
}

export function ApplicationDetailIndexRedirect() {
    const { permissionsReady, permissionsError } = useApplicationDetailContext();
    const permissionVersion = usePermissionServiceSnapshot();

    const landingPath = useMemo(
        () => resolveApplicationDetailLandingPath(permissions => permissionService.hasAnyOf(permissions)),
        [permissionVersion],
    );

    if (permissionsError) {
        return null;
    }

    if (!permissionsReady) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    if (!landingPath) {
        return <ApplicationDetailNoSectionsAvailable />;
    }

    return <Navigate to={landingPath} replace />;
}
