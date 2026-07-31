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

import { Button, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { CreateDictionarySheet } from '../features/dictionaries/components/CreateDictionarySheet';
import { DictionariesTable } from '../features/dictionaries/components/DictionariesTable';
import { DictionaryDeleteSheet } from '../features/dictionaries/components/DictionaryDeleteSheet';
import { EditDictionarySheet } from '../features/dictionaries/components/EditDictionarySheet';
import { useCreateDictionary, useDeleteDictionary, useUpdateDictionary } from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionaries } from '../features/dictionaries/hooks/useEnvironmentDictionaries';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import type { DictionaryListItem, NewDictionaryPayload, UpdateDictionaryPayload } from '../features/dictionaries/types/dictionary';
import { notify } from '../shared/notify';

type SheetState =
    | { type: 'closed' }
    | { type: 'create' }
    | { type: 'edit'; dictionaryId: string }
    | { type: 'delete'; dictionary: DictionaryListItem };

export function DictionariesPage() {
    const navigate = useNavigate();
    const { canCreate, canUpdate, canDelete } = useDictionaryPermissions();
    const { data: dictionaries = [], isLoading, isError } = useEnvironmentDictionaries();
    const createMutation = useCreateDictionary();
    const updateMutation = useUpdateDictionary();
    const deleteMutation = useDeleteDictionary();

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });
    const editDictionaryId = sheet.type === 'edit' ? sheet.dictionaryId : undefined;
    const { data: editDictionary, isLoading: editLoading } = useEnvironmentDictionary(editDictionaryId);

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    async function handleCreate(data: NewDictionaryPayload) {
        const created = await createMutation.mutateAsync(data);
        notify.success('Dictionary created successfully');
        closeSheet();
        navigate(created.id);
    }

    function handleOpen(dictionary: DictionaryListItem) {
        navigate(dictionary.id);
    }

    async function handleEdit(data: UpdateDictionaryPayload) {
        if (sheet.type !== 'edit') return;
        await updateMutation.mutateAsync({ dictionaryId: sheet.dictionaryId, data });
        notify.success('Dictionary updated successfully');
        closeSheet();
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.dictionary.id);
            notify.success('Dictionary deleted successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete dictionary');
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Dictionaries</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage environment lookup data that API policies can reference at runtime.
                    </p>
                </div>
                {canCreate && (
                    <Button className="shrink-0" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add Dictionary
                    </Button>
                )}
            </div>

            {isLoading ? (
                <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 w-full rounded-md" />
                    ))}
                </div>
            ) : isError ? (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Failed to load dictionaries. Please refresh and try again.</p>
                </div>
            ) : (
                <DictionariesTable
                    dictionaries={dictionaries}
                    canEdit={canUpdate}
                    canDelete={canDelete}
                    onOpen={handleOpen}
                    onEdit={d => setSheet({ type: 'edit', dictionaryId: d.id })}
                    onDelete={d => setSheet({ type: 'delete', dictionary: d })}
                />
            )}

            <CreateDictionarySheet
                open={sheet.type === 'create'}
                onClose={closeSheet}
                onSubmit={handleCreate}
                isSaving={createMutation.isPending}
            />

            <EditDictionarySheet
                open={sheet.type === 'edit'}
                dictionary={editDictionary}
                isLoading={editLoading}
                onClose={closeSheet}
                onSubmit={handleEdit}
                isSaving={updateMutation.isPending}
            />

            <DictionaryDeleteSheet
                open={sheet.type === 'delete'}
                dictionary={sheet.type === 'delete' ? sheet.dictionary : undefined}
                onClose={closeSheet}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
        </div>
    );
}
