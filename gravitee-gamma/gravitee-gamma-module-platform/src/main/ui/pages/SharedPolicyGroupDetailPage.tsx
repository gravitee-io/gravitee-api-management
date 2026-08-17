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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, DateCell, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon, LayersIcon, PencilIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import {
    SharedPolicyGroupEditSheet,
    type SharedPolicyGroupEditFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupEditSheet';
import { SharedPolicyGroupStatusBadge } from '../features/shared-policy-groups/components/SharedPolicyGroupStatusBadge';
import { useUpdateSharedPolicyGroup } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupDetail } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import { toReadableApiType, toReadableFlowPhase } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
    isKubernetesOrigin,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { toUpdateSharedPolicyGroupPayload } from '../features/shared-policy-groups/utils/sharedPolicyGroupPayload';
import { notify } from '../shared/notify';

function DetailField({ label, value }: Readonly<{ label: string; value: React.ReactNode }>) {
    return (
        <div className="space-y-1">
            <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
            <dd className="text-sm">{value}</dd>
        </div>
    );
}

export function SharedPolicyGroupDetailPage() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const { data: sharedPolicyGroup, isLoading, isError } = useSharedPolicyGroupDetail(sharedPolicyGroupId);
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const updateMutation = useUpdateSharedPolicyGroup();
    const [editOpen, setEditOpen] = useState(false);

    const showEdit = Boolean(sharedPolicyGroup && canEdit && !isKubernetesOrigin(sharedPolicyGroup));

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        if (!sharedPolicyGroup) return;
        try {
            await updateMutation.mutateAsync({
                id: sharedPolicyGroup.id,
                payload: toUpdateSharedPolicyGroupPayload(sharedPolicyGroup, values),
            });
            notify.success('Shared Policy Group updated');
            setEditOpen(false);
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group update!');
        }
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

            <section className="rounded-xl border bg-card p-5">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex min-w-0 items-start gap-3">
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-700">
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
                    {showEdit && (
                        <Button type="button" variant="outline" size="sm" className="shrink-0 gap-1.5" onClick={() => setEditOpen(true)}>
                            <PencilIcon className="size-4" aria-hidden />
                            Edit
                        </Button>
                    )}
                </div>
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">Details</h2>
                <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                    <DetailField label="API type" value={toReadableApiType(sharedPolicyGroup.apiType)} />
                    <DetailField label="Phase" value={toReadableFlowPhase(sharedPolicyGroup.phase)} />
                    <DetailField
                        label="Last updated"
                        value={
                            sharedPolicyGroup.updatedAt ? <DateCell value={new Date(sharedPolicyGroup.updatedAt)} format="absolute" /> : '—'
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
            </section>

            <SharedPolicyGroupEditSheet
                open={editOpen}
                sharedPolicyGroup={sharedPolicyGroup}
                onClose={() => setEditOpen(false)}
                onSubmit={handleEdit}
                isSaving={updateMutation.isPending}
            />
        </div>
    );
}
