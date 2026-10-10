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
import { Button } from '@gravitee/graphene-core';
import { UsersIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { notify } from '../../../shared/notify';
import { ManageGroupsSheet } from '../../shared/components';
import { useIntegrationEnvironmentGroups } from '../hooks/useIntegrationEnvironmentGroups';
import { useUpdateIntegration } from '../hooks/useUpdateIntegration';
import type { Integration } from '../types/integration';

const EMPTY_GROUP_IDS: string[] = [];

interface IntegrationManageGroupsProps {
    integration: Integration;
    canSave: boolean;
}

export function IntegrationManageGroups({ integration, canSave }: Readonly<IntegrationManageGroupsProps>) {
    const [open, setOpen] = useState(false);
    const { data: allGroups = [] } = useIntegrationEnvironmentGroups({ enabled: canSave });
    const updateIntegration = useUpdateIntegration();

    function saveGroups(groupIds: string[]) {
        updateIntegration.mutate(
            {
                integrationId: integration.id,
                request: { name: integration.name, description: integration.description ?? '', groups: groupIds },
            },
            {
                onSuccess: () => {
                    setOpen(false);
                    notify.success('Changes successfully saved!');
                },
                onError: error => notify.error(error, 'Failed to save groups.'),
            },
        );
    }

    return (
        <>
            <Button type="button" variant="outline" size="sm" onClick={() => setOpen(true)} disabled={!canSave}>
                <UsersIcon className="size-4" aria-hidden="true" />
                Manage groups
            </Button>
            {canSave ? (
                <ManageGroupsSheet
                    open={open}
                    allGroups={allGroups}
                    currentGroupIds={integration.groups ?? EMPTY_GROUP_IDS}
                    description="Select the groups that should have access to this integration."
                    onClose={() => setOpen(false)}
                    onSave={saveGroups}
                    isSaving={updateIntegration.isPending}
                />
            ) : null}
        </>
    );
}
