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
import {
    Badge,
    Button,
    ContextSidebar,
    ContextToggleButton,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Skeleton,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleStopIcon, CircleXIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { Navigate, Outlet, useParams } from 'react-router-dom';

import { API_PROXY_NAV_GROUPS, ApiDetailSidebarNav } from './ApiDetailSidebarNav';
import { useDetailBasePath } from '../../../../shared/hooks/useDetailBasePath';
import { ApiDetailContext } from '../../context/ApiDetailContext';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useApiPermissions } from '../../hooks/useApiPermissions';
import { deployApi } from '../../services/apis';
import type { ApiDetailDto } from '../../types';
import { apiDetailKeys } from '../../utils/queryKeys';

/** Classic console caps the deployment label at 32 characters. */
const DEPLOYMENT_LABEL_MAX_LENGTH = 32;

function StateIndicator({ state, deploymentState }: { state: ApiDetailDto['state']; deploymentState?: string }) {
    if (state === 'STARTED' && deploymentState === 'NEED_REDEPLOY') {
        return (
            <Badge
                className="gap-1 h-5 px-1.5 text-xs font-medium border-transparent"
                style={{ backgroundColor: 'color-mix(in oklab, var(--color-warning) 12%, transparent)', color: 'var(--color-warning)' }}
            >
                <TriangleAlertIcon className="size-3" />
                Out of sync
            </Badge>
        );
    }

    switch (state) {
        case 'STARTED':
            return (
                <Badge className="gap-1 h-5 px-1.5 text-xs font-medium bg-success/10 text-success border-transparent">
                    <CircleCheckIcon className="size-3" />
                    Started
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
        <div role="status" aria-label="Deploy status" className="flex items-center justify-between border-b px-6 py-2 bg-warning/10">
            <span className="flex items-center gap-1.5 text-sm text-foreground">
                <TriangleAlertIcon className="size-3.5 shrink-0 text-warning" />
                This API has undeployed changes.
            </span>
            <Button size="sm" variant="outline" onClick={onDeploy} disabled={isPending}>
                {isPending ? 'Deploying…' : 'Deploy API'}
            </Button>
        </div>
    );
}

/** Collects an optional deployment label before deploying — mirrors the classic console deploy dialog. */
function DeployConfirmDialog({
    open,
    isPending,
    onConfirm,
    onOpenChange,
}: {
    open: boolean;
    isPending: boolean;
    onConfirm: (label: string) => void;
    onOpenChange: (open: boolean) => void;
}) {
    const labelId = useId();
    const [label, setLabel] = useState('');

    const handleConfirm = () => onConfirm(label.trim());

    return (
        <Dialog
            open={open}
            onOpenChange={next => {
                if (!next) setLabel('');
                onOpenChange(next);
            }}
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Deploy your API</DialogTitle>
                    <DialogDescription>Provide a label to identify your deployment.</DialogDescription>
                </DialogHeader>
                <div className="space-y-2">
                    <Label htmlFor={labelId}>Deployment label (optional)</Label>
                    <Input
                        id={labelId}
                        maxLength={DEPLOYMENT_LABEL_MAX_LENGTH}
                        placeholder="e.g. hotfix-cors, v2.1-release"
                        value={label}
                        onChange={e => setLabel(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleConfirm()}
                    />
                    <p className="text-right text-xs text-muted-foreground">
                        {label.length}/{DEPLOYMENT_LABEL_MAX_LENGTH}
                    </p>
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline" size="sm">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button size="sm" onClick={handleConfirm} disabled={isPending}>
                        {isPending ? 'Deploying…' : 'Deploy'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
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
                    {api.state ? <StateIndicator state={api.state} deploymentState={api.deploymentState} /> : null}
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
    const [contextExpanded, setContextExpanded] = useState(true);
    const [showDeployDialog, setShowDeployDialog] = useState(false);

    const deployMutation = useMutation({
        mutationFn: (deploymentLabel?: string) => {
            if (!env || !apiId) return Promise.resolve();
            return deployApi(env.id, apiId, deploymentLabel);
        },
        onSuccess: () => {
            setShowDeployDialog(false);
            if (env && apiId) {
                queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env.id, apiId) });
            }
        },
    });

    const showDeployBanner = !isError && api?.deploymentState === 'NEED_REDEPLOY' && canDeploy;

    useLayoutConfig(
        {
            viewMode: 'context',
            contextExpanded,
            contextSidebar: (
                <ContextSidebar header={<ApiInfoHeader api={api ?? null} isLoading={isLoading} />}>
                    <ApiDetailSidebarNav groups={API_PROXY_NAV_GROUPS} basePath={basePath} permissionsReady={permissionsReady} />
                </ContextSidebar>
            ),
            leading: <ContextToggleButton expanded={contextExpanded} onToggle={() => setContextExpanded(v => !v)} />,
            breadcrumbs: [
                { label: 'API Proxies', href: `${basePath.slice(0, basePath.lastIndexOf('/apis/'))}${'/apis'}` },
                { label: api?.name ? (api.name.length > 40 ? `${api.name.slice(0, 40).trimEnd()}…` : api.name) : 'Loading…' },
            ],
            banner: showDeployBanner ? (
                <DeployBanner onDeploy={() => setShowDeployDialog(true)} isPending={deployMutation.isPending} />
            ) : null,
            bannerSticky: true,
        },
        [contextExpanded, api, isLoading, basePath, permissionsReady, showDeployBanner, deployMutation.isPending],
    );

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load API. It may have been deleted or you may not have access.</p>
            </div>
        );
    }

    return (
        <ApiDetailContext.Provider value={{ api: api ?? null, isLoading, permissionsReady }}>
            <Outlet />
            <DeployConfirmDialog
                open={showDeployDialog}
                isPending={deployMutation.isPending}
                onConfirm={label => deployMutation.mutate(label || undefined)}
                onOpenChange={setShowDeployDialog}
            />
        </ApiDetailContext.Provider>
    );
}

export function ApiDetailIndexRedirect() {
    return <Navigate to="overview" replace />;
}
