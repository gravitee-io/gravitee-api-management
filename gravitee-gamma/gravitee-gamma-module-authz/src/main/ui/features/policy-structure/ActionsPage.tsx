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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    DataTable,
    DataTablePagination,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    toast,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon, ZapIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { ColumnDef } from '@tanstack/react-table';
import { useDeferredValue, useMemo, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { authzApiService } from '../../shared/api/authz-api.service';
import { authzQueryKeys } from '../../shared/api/query-keys';
import { formatEntityUid, fromBackend } from '../../shared/entity-adapter';
import type { EntityInstance } from '../../shared/entity.types';
import { useEntities } from '../../shared/hooks/useEntities';
import { CreateActionDialog } from './CreateActionDialog';

const ACTION_PREFIX = 'action.';
const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

const ACTIONS_HELP =
    'Actions are the verbs the policy engine authorizes (e.g. call_tool, invoke, read). Reference them in the action clause of a policy. They live under the reserved action. Entity ID prefix.';

function displayNameOf(entity: EntityInstance): string {
    if (entity.displayName) return entity.displayName;
    const name = entity.attrs.name;
    if (typeof name === 'string' && name) return name;
    const displayName = entity.attrs.displayName;
    if (typeof displayName === 'string' && displayName) return displayName;
    return entity.uid.id;
}

function descriptionOf(entity: EntityInstance): string {
    const description = entity.attrs.description;
    return typeof description === 'string' ? description : '';
}

function sourceLabelOf(entity: EntityInstance): string {
    if (entity.source === 'apim') return 'APIM';
    if (entity.source === 'gravitee-catalog') return 'Gravitee Catalog';
    return 'Local';
}

function actionName(entity: EntityInstance): string {
    return entity.uid.id;
}

function applyFilter(actions: readonly EntityInstance[], search: string): EntityInstance[] {
    const needle = search.trim().toLowerCase();
    if (!needle) return [...actions];
    return actions.filter(
        a =>
            actionName(a).toLowerCase().includes(needle) ||
            formatEntityUid(a.uid).toLowerCase().includes(needle) ||
            displayNameOf(a).toLowerCase().includes(needle) ||
            descriptionOf(a).toLowerCase().includes(needle),
    );
}

export function ActionsPage() {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const queryClient = useQueryClient();
    const actionsQuery = useEntities(environmentId, undefined, { kind: 'RESOURCE', entityIdPrefix: ACTION_PREFIX });

    const [search, setSearch] = useState('');
    const [addOpen, setAddOpen] = useState(false);
    const [pendingDelete, setPendingDelete] = useState<EntityInstance | null>(null);
    const [deletingEntityId, setDeletingEntityId] = useState<string | undefined>();

    const deferredSearch = useDeferredValue(search);

    const actions = useMemo(() => (actionsQuery.data ? actionsQuery.data.data.map(fromBackend) : []), [actionsQuery.data]);
    const total = actionsQuery.data?.total ?? 0;
    const filtered = useMemo(() => applyFilter(actions, deferredSearch), [actions, deferredSearch]);

    const pendingDeleteUid = pendingDelete ? formatEntityUid(pendingDelete.uid) : '';
    const pendingDeleteName = pendingDelete ? displayNameOf(pendingDelete) : '';

    function handleChanged() {
        void queryClient.invalidateQueries({ queryKey: authzQueryKeys.entities.all(environmentId) });
    }

    async function confirmDelete() {
        if (!pendingDelete) return;
        const uid = formatEntityUid(pendingDelete.uid);
        const friendly = displayNameOf(pendingDelete);
        setDeletingEntityId(uid);
        try {
            await authzApiService.deleteEntity(environmentId, uid);
            toast.success(`Removed ${friendly}`);
            setDeletingEntityId(undefined);
            setPendingDelete(null);
            handleChanged();
        } catch (err) {
            toast.error(`Failed to remove: ${err instanceof Error ? err.message : String(err)}`);
            setDeletingEntityId(undefined);
        }
    }

    const columns = useMemo<ColumnDef<EntityInstance>[]>(
        () => [
            {
                id: 'name',
                header: 'Action',
                size: 220,
                cell: ({ row }) => <span className="block truncate font-medium">{displayNameOf(row.original)}</span>,
            },
            {
                id: 'entityId',
                header: 'Entity ID',
                size: 280,
                cell: ({ row }) => <span className="font-mono text-xs text-foreground">{formatEntityUid(row.original.uid)}</span>,
            },
            {
                id: 'description',
                header: 'Description',
                size: 320,
                cell: ({ row }) => {
                    const description = descriptionOf(row.original);
                    return description ? (
                        <span className="block truncate text-sm text-muted-foreground">{description}</span>
                    ) : (
                        <span className="text-sm text-muted-foreground/60">—</span>
                    );
                },
            },
            {
                id: 'source',
                header: 'Source',
                size: 120,
                cell: ({ row }) => <Badge variant="secondary">{sourceLabelOf(row.original)}</Badge>,
            },
            {
                id: 'actions',
                header: '',
                size: 60,
                cell: ({ row }) => {
                    const uid = formatEntityUid(row.original.uid);
                    const isLocal = row.original.source === 'local';
                    if (!isLocal) return null;
                    return (
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setPendingDelete(row.original)}
                            disabled={deletingEntityId === uid}
                            aria-label={`Delete ${uid}`}
                            title="Remove from Authorization"
                        >
                            <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                        </Button>
                    );
                },
            },
        ],
        [deletingEntityId],
    );

    return (
        <div className="flex flex-col gap-4">
            <header className="flex items-start gap-3">
                <ZapIcon className="mt-1 size-5 text-muted-foreground" aria-hidden />
                <div>
                    <h1 className="text-xl font-semibold">Actions</h1>
                    <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
                        The action vocabulary the policy engine evaluates. Define the verbs your policies grant or forbid.
                    </p>
                </div>
            </header>

            <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }} aria-label="Key metrics">
                <KpiTile label="Total actions" value={total} loading={actionsQuery.isLoading} Icon={ZapIcon} iconClassName="bg-warning/10 text-warning" />
            </div>

            {actionsQuery.error !== undefined && (
                <Alert variant="destructive">
                    <AlertTitle>Could not load actions</AlertTitle>
                    <AlertDescription>{actionsQuery.error}</AlertDescription>
                </Alert>
            )}

            <Alert>
                <AlertDescription>{ACTIONS_HELP}</AlertDescription>
            </Alert>

            <div className="flex flex-wrap items-center gap-2">
                <Input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search by name, Entity ID, or description…"
                    className="max-w-sm"
                    aria-label="Search actions"
                />
                <div className="ml-auto">
                    <Button onClick={() => setAddOpen(true)}>
                        <PlusIcon className="mr-2 size-4" aria-hidden />
                        Add action
                    </Button>
                </div>
            </div>

            {!actionsQuery.isLoading && filtered.length === 0 ? (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No actions yet</EmptyTitle>
                        <EmptyDescription>
                            {deferredSearch
                                ? 'No matches for the current search.'
                                : 'Add an action to start authorizing it in your policies.'}
                        </EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : (
                <>
                    <DataTable<EntityInstance>
                        columns={columns}
                        data={filtered}
                        serverSide
                        enableColumnResizing
                        loading={actionsQuery.isLoading}
                        skeletonCount={actionsQuery.perPage}
                    />
                    <DataTablePagination
                        page={actionsQuery.page}
                        pageSize={actionsQuery.perPage}
                        totalCount={total}
                        pageSizeOptions={PAGE_SIZE_OPTIONS}
                        onPageChange={actionsQuery.setPage}
                        onPageSizeChange={actionsQuery.setPerPage}
                    />
                </>
            )}

            <CreateActionDialog open={addOpen} environmentId={environmentId} onOpenChange={setAddOpen} onCreated={handleChanged} />

            <Dialog
                open={pendingDelete !== null}
                onOpenChange={open => {
                    if (!open && !deletingEntityId) setPendingDelete(null);
                }}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Remove action from Authorization?</DialogTitle>
                        <DialogDescription>
                            {pendingDelete
                                ? `"${pendingDeleteName}" (${pendingDeleteUid}) will be removed. Policies that reference this action will no longer match it.`
                                : ''}
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setPendingDelete(null)}
                            disabled={deletingEntityId !== undefined}
                        >
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            onClick={confirmDelete}
                            disabled={deletingEntityId !== undefined}
                            aria-label={pendingDelete ? `Confirm remove ${pendingDeleteName}` : 'Confirm remove'}
                        >
                            {deletingEntityId !== undefined ? 'Removing…' : 'Remove'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
