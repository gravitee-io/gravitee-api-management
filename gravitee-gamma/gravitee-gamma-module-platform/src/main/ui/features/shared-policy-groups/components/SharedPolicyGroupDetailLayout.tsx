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

import { Button, DateCell, Skeleton, useLayoutConfig } from '@gravitee/graphene-core';
import { ArrowLeftIcon, LayersIcon } from '@gravitee/graphene-core/icons';
import { Outlet, useLocation, useNavigate, useParams, useResolvedPath } from 'react-router-dom';

import { SharedPolicyGroupActions } from './SharedPolicyGroupActions';
import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import { useForbiddenResourceRedirect } from '../../../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../../../shared/utils/apiErrors';
import { useSharedPolicyGroupDetail } from '../hooks/useSharedPolicyGroups';
import { type SharedPolicyGroup, toReadableApiType, toReadableFlowPhase } from '../types/sharedPolicyGroup';
import { ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX } from '../utils/sharedPolicyGroupPermissions';

function SharedPolicyGroupHeader({ sharedPolicyGroup, listHref }: Readonly<{ sharedPolicyGroup: SharedPolicyGroup; listHref: string }>) {
    return (
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
                    <p className="truncate text-sm text-muted-foreground" title={sharedPolicyGroup.description}>
                        {sharedPolicyGroup.description}
                    </p>
                ) : null}
                <p className="text-sm text-muted-foreground">
                    {toReadableApiType(sharedPolicyGroup.apiType)} · {toReadableFlowPhase(sharedPolicyGroup.phase)}
                </p>
                {sharedPolicyGroup.prerequisiteMessage ? (
                    <p className="text-sm text-muted-foreground">
                        <span className="font-medium">Prerequisite: </span>
                        {sharedPolicyGroup.prerequisiteMessage}
                    </p>
                ) : null}
                <dl className="flex flex-wrap gap-4 gap-y-1 text-xs text-muted-foreground">
                    <div className="flex gap-1">
                        <dt className="font-medium">Last updated</dt>
                        <dd>
                            {sharedPolicyGroup.updatedAt ? (
                                <DateCell value={new Date(sharedPolicyGroup.updatedAt)} format="absolute" />
                            ) : (
                                '—'
                            )}
                        </dd>
                    </div>
                    <div className="flex gap-1">
                        <dt className="font-medium">Last deployed</dt>
                        <dd>
                            {sharedPolicyGroup.deployedAt ? (
                                <DateCell value={new Date(sharedPolicyGroup.deployedAt)} format="absolute" />
                            ) : (
                                '—'
                            )}
                        </dd>
                    </div>
                </dl>
            </div>
            <div className="ml-auto shrink-0">
                <SharedPolicyGroupActions key={sharedPolicyGroup.id} sharedPolicyGroup={sharedPolicyGroup} listHref={listHref} />
            </div>
        </div>
    );
}

export function SharedPolicyGroupDetailLayout() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const navigate = useNavigate();
    const location = useLocation();
    const listHref = useResolvedPath('..').pathname;
    const studioHref = useResolvedPath('studio').pathname;
    const isHistoryTab = location.pathname.endsWith('/history');
    const backHref = isHistoryTab ? studioHref : listHref;
    const backLabel = isHistoryTab ? 'Back to Shared Policy Group' : 'Back to Shared Policy Group list';
    const { data: sharedPolicyGroup, isLoading, isError, error } = useSharedPolicyGroupDetail(sharedPolicyGroupId);
    const isForbidden = isForbiddenApiError(isError, error);

    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
        redirectTo: '../../applications',
    });

    useLayoutConfig(
        {
            breadcrumbs: [{ label: 'Shared Policy Groups', href: listHref }, { label: sharedPolicyGroup?.name ?? 'Loading…' }],
        },
        [listHref, sharedPolicyGroup?.name],
    );

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
                <Skeleton className="h-64 w-full" />
            </div>
        );
    }

    if (isError || !sharedPolicyGroup) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate(listHref)}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Shared Policy Group list
                </Button>
                <p className="text-sm text-muted-foreground">Shared Policy Group not found or failed to load.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6" data-testid="shared-policy-group-detail">
            <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate(backHref)}>
                <ArrowLeftIcon className="size-4" aria-hidden />
                {backLabel}
            </Button>

            <SharedPolicyGroupHeader sharedPolicyGroup={sharedPolicyGroup} listHref={listHref} />

            <Outlet key={sharedPolicyGroupId} context={sharedPolicyGroup} />
        </div>
    );
}
