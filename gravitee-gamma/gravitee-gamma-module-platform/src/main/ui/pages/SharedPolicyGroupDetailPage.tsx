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
import { Button, DateCell } from '@gravitee/graphene-core';
import { PencilIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import {
    SharedPolicyGroupEditSheet,
    type SharedPolicyGroupEditFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupEditSheet';
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

/** Overview tab — metadata details (header/tabs live on SharedPolicyGroupDetailLayout). */
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

    // Layout already handles loading / not-found for the shell; keep a light fallback for the tab outlet.
    if (isLoading || isError || !sharedPolicyGroup) {
        return null;
    }

    return (
        <div className="space-y-6" data-testid="shared-policy-group-overview">
            <section className="space-y-4 rounded-xl border bg-card p-5">
                <div className="flex items-center justify-between gap-3">
                    <h2 className="text-base font-semibold">Details</h2>
                    {showEdit && (
                        <Button type="button" variant="outline" size="sm" className="shrink-0 gap-1.5" onClick={() => setEditOpen(true)}>
                            <PencilIcon className="size-4" aria-hidden />
                            Edit
                        </Button>
                    )}
                </div>
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
