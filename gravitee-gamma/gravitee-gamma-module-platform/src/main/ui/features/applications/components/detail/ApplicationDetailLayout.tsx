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
import { Badge, Skeleton } from '@gravitee/graphene-core';
import { AppWindowIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';
import { Navigate, useParams } from 'react-router-dom';

import { ApplicationDetailProtectedOutlet } from './ApplicationDetailProtectedOutlet';
import { ApplicationDetailSidebarNav } from './ApplicationDetailSidebarNav';
import {
    APPLICATION_CONSOLE_DEFAULT_DETAIL_PATH,
    APPLICATION_NAV_GROUPS,
    getFirstAccessibleApplicationDetailPath,
} from '../../../../config/applicationDetailNavigation';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { usePermissionServiceSnapshot } from '../../../shared/hooks/usePermissionServiceSnapshot';
import { ApplicationDetailContext, useApplicationDetailContext } from '../../context/ApplicationDetailContext';
import { useApplicationDetail } from '../../hooks/useApplicationDetail';
import { useApplicationPermissions } from '../../hooks/useApplicationPermissions';
import type { ApplicationListItem, ApplicationStatus } from '../../types/application';
import { formatApplicationOwnerLabel, formatApplicationSecurityTypeLabel } from '../../utils/applicationFormatters';

function StatusBadge({ status }: { status: ApplicationStatus }) {
    if (status === 'ACTIVE') {
        return (
            <Badge className="h-6 w-fit rounded-md border-0 bg-primary px-2.5 text-xs font-medium lowercase text-primary-foreground">
                active
            </Badge>
        );
    }
    return (
        <Badge className="h-6 w-fit rounded-md border-0 bg-muted px-2.5 text-xs font-medium lowercase text-muted-foreground">
            archived
        </Badge>
    );
}

function ApplicationInfoHeader({ application, isLoading }: { application: ApplicationListItem | null; isLoading: boolean }) {
    if (isLoading) {
        return (
            <div className="space-y-2 border-b px-3 pb-4 pt-4">
                <div className="flex items-start gap-2.5">
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

    return (
        <div className="space-y-2 border-b px-3 pb-4 pt-4">
            <div className="flex items-start gap-2.5">
                <div className="flex size-8 shrink-0 items-center justify-center rounded-lg border border-primary/30 bg-primary/5">
                    <AppWindowIcon className="size-4 text-primary" aria-hidden />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5">
                    <p
                        className="text-sm font-semibold leading-snug text-foreground"
                        style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                        title={application.name}
                    >
                        {application.name}
                    </p>
                    <StatusBadge status={application.status} />
                </div>
            </div>
            {application.description ? (
                <p
                    className="text-xs text-muted-foreground"
                    style={{
                        lineHeight: '1.625',
                        wordBreak: 'break-all',
                        overflow: 'hidden',
                        display: '-webkit-box',
                        WebkitBoxOrient: 'vertical',
                        WebkitLineClamp: 2,
                    }}
                    title={application.description}
                >
                    {application.description}
                </p>
            ) : null}
            <div className="flex flex-wrap items-center gap-1.5">
                <span className="inline-flex h-5 items-center border border-border bg-background px-1.5 text-xs font-normal text-foreground">
                    {formatApplicationSecurityTypeLabel(application)}
                </span>
                {ownerLabel ? (
                    <span className="inline-flex h-5 items-center bg-muted px-1.5 text-xs font-normal text-foreground">{ownerLabel}</span>
                ) : null}
            </div>
        </div>
    );
}

export function ApplicationDetailLayout() {
    const { applicationId } = useParams<{ applicationId: string }>();
    const basePath = useDetailBasePath('applications', applicationId);
    const { data: application, isLoading, isError } = useApplicationDetail(applicationId);
    const { permissionsReady } = useApplicationPermissions(applicationId);

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">
                    Failed to load application. It may have been deleted or you may not have access.
                </p>
            </div>
        );
    }

    return (
        <ApplicationDetailContext.Provider value={{ application: application ?? null, isLoading, permissionsReady }}>
            <div className="flex gap-6">
                <aside
                    className="sticky top-0 w-56 min-w-0 shrink-0 self-start overflow-x-hidden overflow-y-auto pb-4"
                    style={{ maxHeight: '100dvh', maxWidth: '14rem' }}
                >
                    <ApplicationInfoHeader application={application ?? null} isLoading={isLoading} />
                    <ApplicationDetailSidebarNav
                        groups={APPLICATION_NAV_GROUPS}
                        basePath={basePath}
                        permissionsReady={permissionsReady && !isLoading}
                    />
                </aside>
                <main className="min-w-0 flex-1">
                    <ApplicationDetailProtectedOutlet />
                </main>
            </div>
        </ApplicationDetailContext.Provider>
    );
}

export function ApplicationDetailIndexRedirect() {
    const { permissionsReady } = useApplicationDetailContext();
    const permissionVersion = usePermissionServiceSnapshot();

    const firstAccessible = useMemo(
        () => getFirstAccessibleApplicationDetailPath(APPLICATION_NAV_GROUPS, permissions => permissionService.hasAnyOf(permissions)),
        [permissionVersion],
    );

    if (!permissionsReady) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full" />
            </div>
        );
    }

    return <Navigate to={firstAccessible ?? APPLICATION_CONSOLE_DEFAULT_DETAIL_PATH} replace />;
}
