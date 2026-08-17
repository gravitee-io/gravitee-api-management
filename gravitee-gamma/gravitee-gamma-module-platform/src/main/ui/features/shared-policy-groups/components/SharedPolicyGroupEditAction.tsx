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
import { Button } from '@gravitee/graphene-core';
import { PencilIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { SharedPolicyGroupEditSheet, type SharedPolicyGroupEditFormValues } from './SharedPolicyGroupEditSheet';
import { notify } from '../../../shared/notify';
import { useUpdateSharedPolicyGroup } from '../hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { toUpdateSharedPolicyGroupPayload } from '../utils/sharedPolicyGroupPayload';
import { ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION, isKubernetesOrigin } from '../utils/sharedPolicyGroupPermissions';

export function SharedPolicyGroupEditAction({ sharedPolicyGroup }: Readonly<{ sharedPolicyGroup: SharedPolicyGroup }>) {
    const canEdit = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const updateMutation = useUpdateSharedPolicyGroup();
    const [editOpen, setEditOpen] = useState(false);

    if (!canEdit || isKubernetesOrigin(sharedPolicyGroup)) {
        return null;
    }

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
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
        <>
            <Button type="button" variant="outline" size="sm" className="gap-1.5" onClick={() => setEditOpen(true)}>
                <PencilIcon className="size-4" aria-hidden />
                Edit
            </Button>
            {editOpen ? (
                <SharedPolicyGroupEditSheet
                    key={sharedPolicyGroup.id}
                    open
                    sharedPolicyGroup={sharedPolicyGroup}
                    onClose={() => setEditOpen(false)}
                    onSubmit={handleEdit}
                />
            ) : null}
        </>
    );
}
