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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Badge, Button, Skeleton } from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleStopIcon, CircleXIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, Outlet, useParams } from 'react-router-dom';

import { API_PROXY_NAV_GROUPS, ApiDetailSidebarNav } from './ApiDetailSidebarNav';
import { useDetailBasePath } from '../../../../shared/hooks/useDetailBasePath';
import { ApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useApiPermissions } from '../../hooks/useApiPermissions';
import { deployApi } from '../../services/apis';
import type { ApiDetailDto } from '../../types';
import { apiDetailKeys } from '../../utils/queryKeys';

function StateIndicator({ state }: { state: ApiDetailDto['state'] }) {
    switch (state) {
        case 'STARTED':
            return (
                <Badge className="gap-1 h-5 px-1.5 text-xs font-medium bg-success/10 text-success border-transparent">
                    <CircleCheckIcon className="size-3" />
                    Deployed
                </Badge>
            );
        case 'STOPPED':
            return (
                <Badge variant="secondary" className="gap-1 h-5 px-1.5 text-xs font-medium">
                    <CircleStopIcon className="size-3" />
                    Stopped
                </Badge>
            );
        case 'CLOSED':
            return (
                <Badge variant="outline" className="gap-1 h-5 px-1.5 text-xs font-medium text-muted-foreground">
                    <CircleXIcon className="size-3" />
                    Closed
                </Badge>
            );
        default:
            return null;
    }
}

function ApiAvatar({ api }: { api: ApiDetailDto }) {
    const [imgError, setImgError] = useState(false);
    const pictureUrl = api._links?.pictureUrl;
    if (!pictureUrl || imgError) return null;
    return <img src={pictureUrl} alt={api.name} className="size-8 rounded-lg shrink-0 object-cover" onError={() => setImgError(true)} />;
}

function DeployBanner({ onDeploy, isPending }: { onDeploy: () => void; isPending: boolean }) {
    return (
        <Alert className="rounded-none border-x-0 border-t-0 flex items-center justify-between gap-4">
            <div className="flex items-center gap-2">
                <TriangleAlertIcon className="size-4 text-warning shrink-0" />
                <AlertDescription>This API has pending changes. Redeploy to apply them to the gateway.</AlertDescription>
            </div>
            <Button size="sm" onClick={onDeploy} disabled={isPending} className="shrink-0">
                {isPending ? 'Deploying…' : 'Deploy API'}
            </Button>
        </Alert>
    );
}

function ApiInfoHeader({ api, isLoading }: { api: ApiDetailDto | null; isLoading: boolean }) {
    if (isLoading) {
        return (
            <div className="px-3 pt-4 pb-4 border-b space-y-3">
                <div className="flex items-start gap-2.5">
                    <Skeleton className="size-8 rounded-lg shrink-0" />
                    <div className="space-y-1.5 min-w-0 flex-1">
                        <Skeleton className="h-3.5 w-32 rounded" />
                        <Skeleton className="h-3 w-16 rounded" />
                    </div>
                </div>
                <div className="flex gap-1.5">
                    <Skeleton className="h-5 w-14 rounded-md" />
                    <Skeleton className="h-5 w-12 rounded-md" />
                </div>
            </div>
        );
    }

    if (!api) return null;

    return (
        <div className="px-3 pt-4 pb-4 border-b space-y-2.5">
            <div className="flex items-start gap-2.5">
                <ApiAvatar api={api} />
                <div className="space-y-1 min-w-0 flex-1">
                    <p
                        className="text-sm font-semibold text-foreground leading-snug"
                        style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                        title={api.name}
                    >
                        {api.name}
                    </p>
                    {api.state ? <StateIndicator state={api.state} /> : null}
                </div>
            </div>

            {/* Description */}
            {api.description ? (
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
                >
                    {api.description}
                </p>
            ) : null}

            {/* Tech chips */}
            <div className="flex flex-wrap items-center gap-1">
                {api.type ? (
                    <Badge variant="secondary" className="text-xs px-1.5 py-0 h-5">
                        {api.type === 'PROXY' ? 'HTTP Proxy' : 'Event-driven'}
                    </Badge>
                ) : null}
                {api.apiVersion ? (
                    <Badge variant="outline" className="text-xs font-mono px-1.5 py-0 h-5">
                        {api.apiVersion}
                    </Badge>
                ) : null}
            </div>
        </div>
    );
}

export function ApiDetailLayout() {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const basePath = useDetailBasePath('apis', apiId);
    const { data: api, isLoading, isError } = useApiDetail(apiId);
    const { permissionsReady } = useApiPermissions(apiId);
    const canDeploy = useHasPermission({ anyOf: ['api-definition-u'] });
    const queryClient = useQueryClient();

    const deployMutation = useMutation({
        mutationFn: () => {
            if (!env || !apiId) return Promise.resolve();
            return deployApi(env.id, apiId);
        },
        onSuccess: () => {
            if (env && apiId) {
                queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env.id, apiId) });
            }
        },
    });

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load API. It may have been deleted or you may not have access.</p>
            </div>
        );
    }

    const showDeployBanner = api?.deploymentState === 'NEED_REDEPLOY' && canDeploy;

    return (
        <ApiDetailContext.Provider value={{ api: api ?? null, isLoading, permissionsReady }}>
            <div className="flex" style={{ height: 'calc(100dvh - 5rem)' }}>
                <aside className="w-56 min-w-0 shrink-0 overflow-y-auto overflow-x-hidden pb-4" style={{ maxWidth: '14rem' }}>
                    <ApiInfoHeader api={api ?? null} isLoading={isLoading} />
                    <ApiDetailSidebarNav groups={API_PROXY_NAV_GROUPS} basePath={basePath} permissionsReady={permissionsReady} />
                </aside>
                <div className="w-px bg-border shrink-0" />
                <main className="min-w-0 flex-1 overflow-y-auto">
                    {showDeployBanner && <DeployBanner onDeploy={() => deployMutation.mutate()} isPending={deployMutation.isPending} />}
                    <Outlet />
                </main>
            </div>
        </ApiDetailContext.Provider>
    );
}

export function ApiDetailIndexRedirect() {
    return <Navigate to="overview" replace />;
}
