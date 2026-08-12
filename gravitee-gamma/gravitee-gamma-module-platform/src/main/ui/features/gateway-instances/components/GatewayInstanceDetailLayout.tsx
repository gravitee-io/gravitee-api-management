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

import { Button, Skeleton, cn } from '@gravitee/graphene-core';
import { ArrowLeftIcon, ServerIcon } from '@gravitee/graphene-core/icons';
import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom';

import { GatewayInstanceStatusBadge } from './GatewayInstanceStatusBadge';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useGatewayInstanceDetail } from '../hooks/useGatewayInstanceDetail';

const TABS = [
    { path: 'environment', label: 'Environment', testId: 'instances-detail-environment' },
    { path: 'monitoring', label: 'Monitoring', testId: 'instances-detail-monitoring' },
] as const;

export function GatewayInstanceDetailLayout() {
    const { instanceId } = useParams<{ instanceId: string }>();
    const navigate = useNavigate();
    const basePath = useDetailBasePath('gateways', instanceId);
    const gatewaysListHref = resolveListHrefFromDetailBasePath(basePath);
    const { data: instance, isLoading, isError, error } = useGatewayInstanceDetail(instanceId);

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: 'environment-instance-',
        redirectTo: '../../applications',
    });

    if (isForbidden) {
        return null;
    }

    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-40" />
                <div className="flex items-start gap-3">
                    <Skeleton className="size-10 rounded-lg" />
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-64" />
                        <Skeleton className="h-4 w-96" />
                    </div>
                </div>
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-64 w-full" />
            </div>
        );
    }

    if (isError || !instance) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate(gatewaysListHref)}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Gateways
                </Button>
                <p className="text-sm text-muted-foreground">Gateway instance not found or failed to load.</p>
            </div>
        );
    }

    const title = instance.hostname || instanceId || 'Gateway instance';

    return (
        <div className="space-y-6" data-testid="gateway-instance-detail">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 cursor-pointer" onClick={() => navigate(gatewaysListHref)}>
                <ArrowLeftIcon className="size-4" aria-hidden />
                Back to Gateways
            </Button>

            <div className="flex items-start gap-3">
                <div className="shrink-0 rounded-lg bg-primary/10 p-2.5">
                    <ServerIcon className="size-5 text-primary" aria-hidden />
                </div>
                <div className="min-w-0 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                        <h1 className="text-2xl font-semibold font-mono tracking-tight truncate">{title}</h1>
                        <GatewayInstanceStatusBadge state={instance.state} />
                    </div>
                    <p className="text-sm text-muted-foreground">
                        {instance.ip && instance.port ? `${instance.ip}:${instance.port}` : null}
                        {instance.version ? (
                            <>
                                {instance.ip && instance.port ? ' · ' : null}
                                {instance.version}
                            </>
                        ) : null}
                    </p>
                    {/* Classic always shows a tags line (configured tags or "No tag configured"). */}
                    <p className="text-sm text-muted-foreground" data-testid="gateway-instance-tags">
                        {instance.tags?.length ? `Tags: ${instance.tags.join(', ')}` : 'No tag configured'}
                    </p>
                </div>
            </div>

            <div className="border-b">
                <nav className="flex items-center gap-6" aria-label="Gateway sections">
                    {TABS.map(tab => (
                        <NavLink
                            key={tab.path}
                            to={`${basePath}/${tab.path}`}
                            data-testid={tab.testId}
                            className={({ isActive }) =>
                                cn(
                                    '-mb-px border-b-2 px-0.5 pb-3 text-sm transition-colors',
                                    isActive
                                        ? 'border-foreground font-semibold text-foreground'
                                        : 'border-transparent text-muted-foreground hover:text-foreground',
                                )
                            }
                        >
                            {tab.label}
                        </NavLink>
                    ))}
                </nav>
            </div>

            <Outlet />
        </div>
    );
}
