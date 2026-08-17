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
import { ArrowLeftIcon, LayersIcon } from '@gravitee/graphene-core/icons';
import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom';

import { SharedPolicyGroupDeployActions } from './SharedPolicyGroupDeployActions';
import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useSharedPolicyGroupDetail } from '../hooks/useSharedPolicyGroups';
import { toReadableApiType, toReadableFlowPhase } from '../types/sharedPolicyGroup';
import { SHARED_POLICY_GROUP_DETAIL_TABS } from '../utils/sharedPolicyGroupDetailNavigation';

export function SharedPolicyGroupDetailLayout() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const navigate = useNavigate();
    const basePath = useDetailBasePath('shared-policy-groups', sharedPolicyGroupId);
    const listHref = resolveListHrefFromDetailBasePath(basePath);
    const { data: sharedPolicyGroup, isLoading, isError } = useSharedPolicyGroupDetail(sharedPolicyGroupId);

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
                <Skeleton className="h-10 w-72" />
                <Skeleton className="h-64 w-full" />
            </div>
        );
    }

    if (isError || !sharedPolicyGroup) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate(listHref)}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Shared Policy Groups
                </Button>
                <p className="text-sm text-muted-foreground">Shared Policy Group not found or failed to load.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6" data-testid="shared-policy-group-detail">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 cursor-pointer" onClick={() => navigate(listHref)}>
                <ArrowLeftIcon className="size-4" aria-hidden />
                Back to Shared Policy Groups
            </Button>

            <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex min-w-0 items-start gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-700">
                        <LayersIcon className="size-5" aria-hidden />
                    </div>
                    <div className="min-w-0 space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                            <h1 className="truncate text-2xl font-semibold tracking-tight">{sharedPolicyGroup.name}</h1>
                            <SharedPolicyGroupStatusBadge lifecycleState={sharedPolicyGroup.lifecycleState} />
                        </div>
                        {sharedPolicyGroup.description ? (
                            <p className="text-sm text-muted-foreground">{sharedPolicyGroup.description}</p>
                        ) : null}
                        <p className="text-sm text-muted-foreground">
                            {toReadableApiType(sharedPolicyGroup.apiType)} · {toReadableFlowPhase(sharedPolicyGroup.phase)}
                        </p>
                    </div>
                </div>
                <SharedPolicyGroupDeployActions sharedPolicyGroup={sharedPolicyGroup} />
            </div>

            <div className="border-b">
                <nav className="flex items-center gap-6" aria-label="Shared Policy Group sections">
                    {SHARED_POLICY_GROUP_DETAIL_TABS.map(tab => (
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
