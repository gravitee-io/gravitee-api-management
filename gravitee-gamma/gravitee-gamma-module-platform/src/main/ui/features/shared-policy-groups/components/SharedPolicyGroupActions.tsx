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
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@gravitee/graphene-core';
import { ClockIcon, MoreHorizontalIcon, PencilIcon, RocketIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { SharedPolicyGroupEditSheet, type SharedPolicyGroupEditFormValues } from './SharedPolicyGroupEditSheet';
import { SharedPolicyGroupRemoveDialog } from './SharedPolicyGroupRemoveDialog';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { notify } from '../../../shared/notify';
import {
    useDeleteSharedPolicyGroup,
    useDeploySharedPolicyGroup,
    useUndeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { sharedPolicyGroupHistoryHref } from '../utils/sharedPolicyGroupDetailNavigation';
import { toUpdateSharedPolicyGroupPayload } from '../utils/sharedPolicyGroupPayload';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION,
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
    isKubernetesOrigin,
} from '../utils/sharedPolicyGroupPermissions';

interface SharedPolicyGroupActionsProps {
    readonly sharedPolicyGroup: SharedPolicyGroup;
    readonly listHref: string;
}

export function SharedPolicyGroupActions({ sharedPolicyGroup, listHref }: SharedPolicyGroupActionsProps) {
    const canUpdate = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION]);
    const canDelete = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_DELETE_PERMISSION]);
    const navigate = useNavigate();
    const updateMutation = useUpdateSharedPolicyGroup();
    const deployMutation = useDeploySharedPolicyGroup();
    const undeployMutation = useUndeploySharedPolicyGroup();
    const deleteMutation = useDeleteSharedPolicyGroup();
    const [editOpen, setEditOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const kubernetesOrigin = isKubernetesOrigin(sharedPolicyGroup);
    const canMutate = !kubernetesOrigin;
    const historyHref = `${listHref}/${sharedPolicyGroupHistoryHref(sharedPolicyGroup.id)}`;

    async function handleEdit(values: SharedPolicyGroupEditFormValues) {
        try {
            // The edit sheet only touches name/description/prerequisite. `sharedPolicyGroup.steps`
            // here is the group's real, already-loaded steps, so send it back explicitly rather
            // than omitting the field — the backend only leaves existing steps alone when `steps`
            // is `null`, and an accidental `[]` here would wipe the policies instead.
            await updateMutation.mutateAsync({
                id: sharedPolicyGroup.id,
                payload: { ...toUpdateSharedPolicyGroupPayload(values), steps: sharedPolicyGroup.steps },
            });
            notify.success('Shared Policy Group updated');
            setEditOpen(false);
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group update!');
        }
    }

    async function handleDeploy() {
        try {
            await deployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group deployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group deployment!');
        }
    }

    async function handleUndeploy() {
        try {
            await undeployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group undeployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group undeployment!');
        }
    }

    async function handleDelete() {
        try {
            await deleteMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group removed');
            setDeleteOpen(false);
            navigate(listHref);
        } catch (error) {
            notify.error(error, 'An error occurred while removing the Shared Policy Group');
        }
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button type="button" variant="ghost" size="icon" aria-label="Shared Policy Group actions">
                        <MoreHorizontalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    {canUpdate && canMutate ? (
                        <DropdownMenuItem onSelect={() => setEditOpen(true)}>
                            <PencilIcon className="mr-2 size-4" aria-hidden />
                            Edit
                        </DropdownMenuItem>
                    ) : null}
                    {canUpdate && canMutate && sharedPolicyGroup.lifecycleState !== 'DEPLOYED' ? (
                        <DropdownMenuItem disabled={deployMutation.isPending} onSelect={() => void handleDeploy()}>
                            <RocketIcon className="mr-2 size-4" aria-hidden />
                            {deployMutation.isPending ? 'Deploying…' : 'Deploy'}
                        </DropdownMenuItem>
                    ) : null}
                    {canUpdate && canMutate && sharedPolicyGroup.lifecycleState !== 'UNDEPLOYED' ? (
                        <DropdownMenuItem disabled={undeployMutation.isPending} onSelect={() => void handleUndeploy()}>
                            <RocketIcon className="mr-2 size-4" aria-hidden />
                            {undeployMutation.isPending ? 'Undeploying…' : 'Undeploy'}
                        </DropdownMenuItem>
                    ) : null}
                    <DropdownMenuItem onSelect={() => navigate(historyHref)}>
                        <ClockIcon className="mr-2 size-4" aria-hidden />
                        Version History
                    </DropdownMenuItem>
                    {canUpdate && canDelete && canMutate ? (
                        <>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem variant="destructive" onSelect={() => setDeleteOpen(true)}>
                                <Trash2Icon className="mr-2 size-4" aria-hidden />
                                Delete
                            </DropdownMenuItem>
                        </>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
            {editOpen ? (
                <SharedPolicyGroupEditSheet
                    key={sharedPolicyGroup.id}
                    open
                    sharedPolicyGroup={sharedPolicyGroup}
                    onClose={() => setEditOpen(false)}
                    onSubmit={handleEdit}
                />
            ) : null}
            <SharedPolicyGroupRemoveDialog
                open={deleteOpen}
                onOpenChange={setDeleteOpen}
                isPending={deleteMutation.isPending}
                onConfirm={() => void handleDelete()}
            />
        </>
    );
}
