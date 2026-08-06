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

import { useHasFeature, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Card, CardContent, CardDescription, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { EntrypointConfigurationSection } from '../features/entrypoints/components/EntrypointConfigurationSection';
import { EntrypointDeleteSheet } from '../features/entrypoints/components/EntrypointDeleteSheet';
import { CreateMappingButton, EntrypointMappingsTable } from '../features/entrypoints/components/EntrypointMappingsTable';
import { EntrypointSheet } from '../features/entrypoints/components/EntrypointSheet';
import { ShardingTagDeleteDialog } from '../features/entrypoints/components/ShardingTagDeleteDialog';
import { ShardingTagFormSheet } from '../features/entrypoints/components/ShardingTagFormSheet';
import { ShardingTagsLicenseDialog } from '../features/entrypoints/components/ShardingTagsLicenseDialog';
import { CreateShardingTagButton, ShardingTagsTable } from '../features/entrypoints/components/ShardingTagsTable';
import { useEntrypointConfigurations } from '../features/entrypoints/hooks/useEntrypointConfigurations';
import { useEntrypointMappings } from '../features/entrypoints/hooks/useEntrypointMappings';
import { useCreateEntrypoint, useDeleteEntrypoint, useUpdateEntrypoint } from '../features/entrypoints/hooks/useEntrypointMutations';
import { useCreateShardingTag, useDeleteShardingTag, useUpdateShardingTag } from '../features/entrypoints/hooks/useShardingTagMutations';
import { useShardingTags } from '../features/entrypoints/hooks/useShardingTags';
import { SHARDING_TAGS_LICENSE_FEATURE } from '../features/entrypoints/license/shardingTagsLicense';
import type {
    EntrypointMappingRow,
    EntrypointTarget,
    NewEntrypointPayload,
    NewOrgTagPayload,
    ShardingTagRow,
    UpdateEntrypointPayload,
    UpdateOrgTagPayload,
} from '../features/entrypoints/types/entrypoint';
import { KAFKA_DOMAIN_PLACEHOLDER } from '../features/entrypoints/utils/entrypointForm';
import { partitionEntrypointsForTagDelete } from '../features/entrypoints/utils/shardingTags';
import { notify } from '../shared/notify';

type SheetState =
    | { type: 'closed' }
    | { type: 'create'; target: EntrypointTarget }
    | { type: 'edit'; entrypoint: EntrypointMappingRow }
    | { type: 'delete'; entrypoint: EntrypointMappingRow };

type TagSheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; tag: ShardingTagRow };

export function EntrypointsAndShardingTagsPage() {
    const canCreate = useHasPermission({ anyOf: ['environment-entrypoint-c', 'organization-entrypoint-c'] });
    const canEdit = useHasPermission({ anyOf: ['environment-entrypoint-u', 'organization-entrypoint-u'] });
    const canDelete = useHasPermission({ anyOf: ['environment-entrypoint-d', 'organization-entrypoint-d'] });
    const canEditConfig = useHasPermission({ anyOf: ['environment-settings-u'] });
    const canReadTags = useHasPermission({ anyOf: ['environment-tag-r', 'organization-tag-r'] });
    const canCreateTag = useHasPermission({ anyOf: ['environment-tag-c', 'organization-tag-c'] });
    const canUpdateTag = useHasPermission({ anyOf: ['environment-tag-u', 'organization-tag-u'] });
    const canDeleteTag = useHasPermission({ anyOf: ['environment-tag-d', 'organization-tag-d'] });
    const hasShardingTagsLicense = useHasFeature(SHARDING_TAGS_LICENSE_FEATURE);

    const { data: configurationData, isLoading: isConfigurationLoading, isError: isConfigurationError } = useEntrypointConfigurations();
    const {
        rows,
        tags,
        environments,
        isLoading: isMappingsLoading,
        isError: isMappingsError,
        isNameResolutionError,
    } = useEntrypointMappings();
    const {
        rows: tagRows,
        groups,
        isLoading: isTagsLoading,
        isError: isTagsError,
        isGroupsLoading,
        isGroupNameResolutionError,
    } = useShardingTags();

    const createMutation = useCreateEntrypoint();
    const updateMutation = useUpdateEntrypoint();
    const deleteMutation = useDeleteEntrypoint();
    const createTagMutation = useCreateShardingTag();
    const updateTagMutation = useUpdateShardingTag();
    const deleteTagMutation = useDeleteShardingTag();

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });
    const [tagSheet, setTagSheet] = useState<TagSheetState>({ type: 'closed' });
    const [licenseDialogOpen, setLicenseDialogOpen] = useState(false);
    const [tagToDelete, setTagToDelete] = useState<ShardingTagRow | null>(null);
    const [isDeletingTag, setIsDeletingTag] = useState(false);

    const tagDeleteImpact = useMemo(
        () => (tagToDelete ? partitionEntrypointsForTagDelete(rows, tagToDelete.key) : { toUpdate: [], toDelete: [] }),
        [rows, tagToDelete],
    );

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    function openCreate(target: EntrypointTarget) {
        setSheet({ type: 'create', target });
    }

    function openEdit(row: EntrypointMappingRow) {
        setSheet({ type: 'edit', entrypoint: row });
    }

    function openDelete(row: EntrypointMappingRow) {
        setSheet({ type: 'delete', entrypoint: row });
    }

    async function handleCreate(data: NewEntrypointPayload | UpdateEntrypointPayload) {
        try {
            await createMutation.mutateAsync(data as NewEntrypointPayload);
            notify.success('Entrypoint mapping created successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to create entrypoint mapping');
        }
    }

    async function handleUpdate(data: NewEntrypointPayload | UpdateEntrypointPayload) {
        try {
            await updateMutation.mutateAsync(data as UpdateEntrypointPayload);
            notify.success('Entrypoint mapping updated successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to update entrypoint mapping');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.entrypoint.id);
            notify.success('Entrypoint mapping deleted successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete entrypoint mapping');
        }
    }

    function handleUpgrade() {
        setLicenseDialogOpen(true);
    }

    function closeTagSheet() {
        setTagSheet({ type: 'closed' });
    }

    function handleCreateTag() {
        if (!hasShardingTagsLicense) {
            handleUpgrade();
            return;
        }
        setTagSheet({ type: 'create' });
    }

    function handleEditTag(tag: ShardingTagRow) {
        if (!hasShardingTagsLicense) {
            handleUpgrade();
            return;
        }
        setTagSheet({ type: 'edit', tag });
    }

    function handleDeleteTag(tag: ShardingTagRow) {
        if (!hasShardingTagsLicense) {
            handleUpgrade();
            return;
        }
        setTagSheet({ type: 'closed' });
        setTagToDelete(tag);
    }

    function closeDeleteDialog() {
        setTagToDelete(null);
    }

    async function handleConfirmDelete() {
        if (!tagToDelete) return;
        setIsDeletingTag(true);
        try {
            const { toUpdate, toDelete } = partitionEntrypointsForTagDelete(rows, tagToDelete.key);
            await Promise.all([
                ...toUpdate.map(entrypoint =>
                    updateMutation.mutateAsync({
                        id: entrypoint.id,
                        target: entrypoint.target,
                        value: entrypoint.value,
                        tags: entrypoint.tags.filter(tag => tag !== tagToDelete.key),
                        environmentIds: entrypoint.environmentIds,
                    }),
                ),
                ...toDelete.map(entrypoint => deleteMutation.mutateAsync(entrypoint.id)),
            ]);
            await deleteTagMutation.mutateAsync(tagToDelete.key);
            notify.success('Tag successfully deleted');
            closeDeleteDialog();
        } catch (error) {
            notify.error(error, 'Failed to delete sharding tag');
        } finally {
            setIsDeletingTag(false);
        }
    }

    async function handleCreateSubmit(data: NewOrgTagPayload) {
        await createTagMutation.mutateAsync(data);
        notify.success('Tag successfully created');
        closeTagSheet();
    }

    async function handleEditSubmit(data: UpdateOrgTagPayload) {
        if (tagSheet.type !== 'edit') return;
        await updateTagMutation.mutateAsync({ tagKey: tagSheet.tag.key, payload: data });
        notify.success('Tag successfully updated');
        closeTagSheet();
    }

    const showMappingsHeaderCreate = canCreate && rows.length > 0;
    const formTarget = sheet.type === 'create' ? sheet.target : sheet.type === 'edit' ? sheet.entrypoint.target : 'HTTP';
    const defaultForm =
        sheet.type === 'create' && sheet.target === 'KAFKA' ? { kafkaDomain: KAFKA_DOMAIN_PLACEHOLDER, kafkaPort: '9092' } : undefined;

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Entrypoints & Sharding Tags</h1>
                <p className="text-sm text-muted-foreground">
                    Manage sharding tags, entrypoints, and mappings between them both for Console and the Developer Portal.
                </p>
            </div>

            <Alert>
                <InfoIcon className="size-4" aria-hidden />
                <AlertDescription>
                    Include entrypoint and sharding tag configuration according to the values already used by the deployed API Gateway(s).
                </AlertDescription>
            </Alert>

            <EntrypointConfigurationSection
                configs={configurationData?.configs ?? []}
                failedEnvironmentNames={configurationData?.failedEnvironmentNames ?? []}
                isLoading={isConfigurationLoading}
                isError={isConfigurationError}
                canEdit={canEditConfig}
            />

            {canReadTags ? (
                <Card>
                    <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                        <div className="space-y-1.5">
                            <CardTitle>Sharding Tags</CardTitle>
                            <CardDescription>Tags used to route APIs to specific gateway groups</CardDescription>
                        </div>
                        {canCreateTag && tagRows.length > 0 ? (
                            <CreateShardingTagButton
                                hasLicense={hasShardingTagsLicense}
                                onCreate={handleCreateTag}
                                onUpgrade={handleUpgrade}
                            />
                        ) : null}
                    </CardHeader>
                    <CardContent className="space-y-3">
                        <Alert>
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>
                                Add the sharding tag&apos;s key to the API Gateway configuration file to manage API deployments.
                            </AlertDescription>
                        </Alert>
                        {isGroupNameResolutionError && !isTagsLoading && !isTagsError ? (
                            <Alert>
                                <InfoIcon className="size-4" aria-hidden />
                                <AlertDescription>
                                    Some restricted group names could not be loaded. IDs may be shown instead of display names.
                                </AlertDescription>
                            </Alert>
                        ) : null}
                        {isTagsLoading ? (
                            <div className="space-y-2">
                                {Array.from({ length: 4 }).map((_, i) => (
                                    <Skeleton key={i} className="h-12 w-full rounded-md" />
                                ))}
                            </div>
                        ) : isTagsError ? (
                            <Alert variant="destructive">
                                <AlertDescription>Failed to load sharding tags. Please refresh and try again.</AlertDescription>
                            </Alert>
                        ) : (
                            <ShardingTagsTable
                                rows={tagRows}
                                canCreate={canCreateTag}
                                hasLicense={hasShardingTagsLicense}
                                canEdit={canUpdateTag}
                                canDelete={canDeleteTag}
                                onEdit={handleEditTag}
                                onDelete={handleDeleteTag}
                                onCreate={handleCreateTag}
                                onUpgrade={handleUpgrade}
                            />
                        )}
                    </CardContent>
                </Card>
            ) : null}

            <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                    <div className="space-y-1.5">
                        <CardTitle>Entrypoint Mappings</CardTitle>
                        <CardDescription>Entrypoint to be displayed in the Developer Portal if an API has a given tag</CardDescription>
                    </div>
                    {showMappingsHeaderCreate ? <CreateMappingButton onCreate={openCreate} /> : null}
                </CardHeader>
                <CardContent className="space-y-3">
                    {isNameResolutionError && !isMappingsLoading && !isMappingsError ? (
                        <Alert>
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>
                                Some environment or sharding tag names could not be loaded. IDs may be shown instead of display names.
                            </AlertDescription>
                        </Alert>
                    ) : null}
                    {isMappingsLoading ? (
                        <div className="space-y-2">
                            {Array.from({ length: 4 }).map((_, i) => (
                                <Skeleton key={i} className="h-12 w-full rounded-md" />
                            ))}
                        </div>
                    ) : isMappingsError ? (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load entrypoint mappings. Please refresh and try again.</AlertDescription>
                        </Alert>
                    ) : (
                        <EntrypointMappingsTable
                            rows={rows}
                            canCreate={canCreate}
                            canEdit={canEdit}
                            canDelete={canDelete}
                            onCreate={canCreate ? openCreate : undefined}
                            onEdit={canEdit ? openEdit : undefined}
                            onDelete={canDelete ? openDelete : undefined}
                        />
                    )}
                </CardContent>
            </Card>

            <EntrypointSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                mode={sheet.type === 'edit' ? 'edit' : 'create'}
                target={formTarget}
                entrypoint={sheet.type === 'edit' ? sheet.entrypoint : undefined}
                tags={tags}
                environments={environments}
                defaultForm={defaultForm}
                existingRows={rows}
                onClose={closeSheet}
                onSubmit={sheet.type === 'edit' ? handleUpdate : handleCreate}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />

            <EntrypointDeleteSheet
                open={sheet.type === 'delete'}
                entrypoint={sheet.type === 'delete' ? sheet.entrypoint : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />

            <ShardingTagFormSheet
                open={tagSheet.type === 'create'}
                mode="create"
                existingTags={tagRows}
                groups={groups}
                isGroupsLoading={isGroupsLoading}
                onClose={closeTagSheet}
                onSubmit={handleCreateSubmit}
                isSaving={createTagMutation.isPending}
            />
            <ShardingTagFormSheet
                open={tagSheet.type === 'edit'}
                mode="edit"
                tag={tagSheet.type === 'edit' ? tagSheet.tag : null}
                existingTags={tagRows}
                groups={groups}
                isGroupsLoading={isGroupsLoading}
                onClose={closeTagSheet}
                onSubmit={handleEditSubmit}
                isSaving={updateTagMutation.isPending}
            />
            <ShardingTagDeleteDialog
                open={tagToDelete !== null}
                tag={tagToDelete}
                entrypointsToUpdate={tagDeleteImpact.toUpdate.map(entrypoint => entrypoint.value)}
                entrypointsToDelete={tagDeleteImpact.toDelete.map(entrypoint => entrypoint.value)}
                onClose={closeDeleteDialog}
                onConfirm={handleConfirmDelete}
                isDeleting={isDeletingTag}
            />
            <ShardingTagsLicenseDialog open={licenseDialogOpen} onOpenChange={setLicenseDialogOpen} />
        </div>
    );
}
