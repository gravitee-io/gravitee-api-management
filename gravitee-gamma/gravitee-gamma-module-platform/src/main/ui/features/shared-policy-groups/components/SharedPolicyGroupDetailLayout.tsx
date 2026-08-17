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

import { Button, Skeleton, Tabs, TabsList, TabsTrigger } from '@gravitee/graphene-core';
import { ArrowLeftIcon, LayersIcon } from '@gravitee/graphene-core/icons';
import { useTransition } from 'react';
import { Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';

import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import { resolveListHrefFromDetailBasePath, useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useSharedPolicyGroupDetail } from '../hooks/useSharedPolicyGroups';
import { toReadableApiType, toReadableFlowPhase } from '../types/sharedPolicyGroup';
import {
    isSharedPolicyGroupDetailTabPath,
    SHARED_POLICY_GROUP_DEFAULT_TAB,
    SHARED_POLICY_GROUP_DETAIL_TABS,
} from '../utils/sharedPolicyGroupDetailNavigation';
import { ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX } from '../utils/sharedPolicyGroupPermissions';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';

export function SharedPolicyGroupDetailLayout() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [isTabPending, startTabTransition] = useTransition();
    const basePath = useDetailBasePath('shared-policy-groups', sharedPolicyGroupId);
    const listHref = resolveListHrefFromDetailBasePath(basePath);
    const { data: sharedPolicyGroup, isLoading, isError, error } = useSharedPolicyGroupDetail(sharedPolicyGroupId);
    const isForbidden = isForbiddenApiError(isError, error);
    const routeSegment = pathname.slice(basePath.length + 1).split('/')[0] ?? '';
    const activeTab = isSharedPolicyGroupDetailTabPath(routeSegment) ? routeSegment : SHARED_POLICY_GROUP_DEFAULT_TAB;

    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
        redirectTo: '../../applications',
    });

    function handleTabChange(tab: string) {
        if (!isSharedPolicyGroupDetailTabPath(tab)) return;
        startTabTransition(() => navigate(`${basePath}/${tab}`));
    }

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
            <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate(listHref)}>
                <ArrowLeftIcon className="size-4" aria-hidden />
                Back to Shared Policy Groups
            </Button>

            <div className="flex items-start gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-highlight text-highlight-foreground">
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

            <Tabs value={activeTab} onValueChange={handleTabChange} aria-busy={isTabPending}>
                <TabsList variant="line" aria-label="Shared Policy Group sections">
                    {SHARED_POLICY_GROUP_DETAIL_TABS.map(tab => (
                        <TabsTrigger key={tab.path} value={tab.path} data-testid={tab.testId}>
                            {tab.label}
                        </TabsTrigger>
                    ))}
                </TabsList>
            </Tabs>

            <Outlet context={sharedPolicyGroup} />
        </div>
    );
}
