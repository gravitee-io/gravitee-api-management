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

import { Button, Card, CardContent, CardHeader, CardTitle, DateCell, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon, LayersIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';

import { SharedPolicyGroupStatusBadge } from '../features/shared-policy-groups/components/SharedPolicyGroupStatusBadge';
import { useSharedPolicyGroupDetail } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import { toReadableApiType, toReadableFlowPhase } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX } from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

function DetailField({ label, value }: Readonly<{ label: string; value: ReactNode }>) {
    return (
        <div className="space-y-1">
            <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
            <dd className="text-sm">{value}</dd>
        </div>
    );
}

export function SharedPolicyGroupDetailPage() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const { data: sharedPolicyGroup, isLoading, isError, error } = useSharedPolicyGroupDetail(sharedPolicyGroupId);
    const isForbidden = isForbiddenApiError(isError, error);

    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: ENVIRONMENT_SHARED_POLICY_GROUP_PERMISSION_PREFIX,
        redirectTo: '../../applications',
    });

    if (isForbidden) {
        return null;
    }

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-32 w-full rounded-xl" />
            </div>
        );
    }

    if (isError || !sharedPolicyGroup) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Shared Policy Groups
                    </Link>
                </Button>
                <p className="text-sm text-muted-foreground">Shared Policy Group not found or failed to load.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Shared Policy Groups
                </Link>
            </Button>

            <Card>
                <CardContent className="p-5">
                    <div className="flex items-start gap-3">
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-highlight text-highlight-foreground">
                            <LayersIcon className="size-5" aria-hidden />
                        </div>
                        <div className="min-w-0 space-y-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <h1 className="text-xl font-semibold tracking-tight">{sharedPolicyGroup.name}</h1>
                                <SharedPolicyGroupStatusBadge lifecycleState={sharedPolicyGroup.lifecycleState} />
                            </div>
                            {sharedPolicyGroup.description && (
                                <p className="text-sm text-muted-foreground">{sharedPolicyGroup.description}</p>
                            )}
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                        <DetailField label="API type" value={toReadableApiType(sharedPolicyGroup.apiType)} />
                        <DetailField label="Phase" value={toReadableFlowPhase(sharedPolicyGroup.phase)} />
                        <DetailField
                            label="Last updated"
                            value={
                                sharedPolicyGroup.updatedAt ? (
                                    <DateCell value={new Date(sharedPolicyGroup.updatedAt)} format="absolute" />
                                ) : (
                                    '—'
                                )
                            }
                        />
                        <DetailField
                            label="Last deployed"
                            value={
                                sharedPolicyGroup.deployedAt ? (
                                    <DateCell value={new Date(sharedPolicyGroup.deployedAt)} format="absolute" />
                                ) : (
                                    '—'
                                )
                            }
                        />
                    </dl>
                    {sharedPolicyGroup.prerequisiteMessage && (
                        <DetailField label="Prerequisite message" value={sharedPolicyGroup.prerequisiteMessage} />
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
