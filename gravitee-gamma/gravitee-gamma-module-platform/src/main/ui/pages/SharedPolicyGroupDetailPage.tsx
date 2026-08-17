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
import { Button, Card, CardContent, CardHeader, CardTitle, DateCell } from '@gravitee/graphene-core';
import { PencilIcon } from '@gravitee/graphene-core/icons';
import { type ReactNode, useState } from 'react';
import { useOutletContext } from 'react-router-dom';

import {
    SharedPolicyGroupEditSheet,
    type SharedPolicyGroupEditFormValues,
} from '../features/shared-policy-groups/components/SharedPolicyGroupEditSheet';
import { useUpdateSharedPolicyGroup } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { type SharedPolicyGroup, toReadableApiType, toReadableFlowPhase } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { toUpdateSharedPolicyGroupPayload } from '../features/shared-policy-groups/utils/sharedPolicyGroupPayload';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
    isKubernetesOrigin,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { notify } from '../shared/notify';

function DetailField({ label, value }: Readonly<{ label: string; value: ReactNode }>) {
    return (
        <div className="space-y-1">
            <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
            <dd className="text-sm">{value}</dd>
        </div>
    );
}

/** Overview tab — metadata details (header/tabs live on SharedPolicyGroupDetailLayout). */
export function SharedPolicyGroupDetailPage() {
    const sharedPolicyGroup = useOutletContext<SharedPolicyGroup>();
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const updateMutation = useUpdateSharedPolicyGroup();
    const [editOpen, setEditOpen] = useState(false);

    const showEdit = canEdit && !isKubernetesOrigin(sharedPolicyGroup);

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        if (!sharedPolicyGroup) return;
        try {
            await updateMutation.mutateAsync({
                id: sharedPolicyGroup.id,
                payload: toUpdateSharedPolicyGroupPayload(values),
            });
            notify.success('Shared Policy Group updated');
            setEditOpen(false);
        } catch (updateError) {
            notify.error(updateError, 'Error during Shared Policy Group update!');
        }
    }

    return (
        <div className="space-y-6" data-testid="shared-policy-group-overview">
            <Card>
                <CardHeader className="flex-row items-center justify-between gap-3">
                    <CardTitle className="text-base">Details</CardTitle>
                    {showEdit ? (
                        <Button type="button" variant="outline" size="sm" className="shrink-0 gap-1.5" onClick={() => setEditOpen(true)}>
                            <PencilIcon className="size-4" aria-hidden />
                            Edit
                        </Button>
                    ) : null}
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

            {editOpen ? (
                <SharedPolicyGroupEditSheet
                    key={sharedPolicyGroup.id}
                    open
                    sharedPolicyGroup={sharedPolicyGroup}
                    onClose={() => setEditOpen(false)}
                    onSubmit={handleEdit}
                />
            ) : null}
        </div>
    );
}
