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
import { Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { EditRulesetSheet } from '../components/rulesets/EditRulesetSheet';
import { FunctionList } from '../components/rulesets/FunctionList';
import { ImportFunctionSheet } from '../components/rulesets/ImportFunctionSheet';
import { ImportRulesetSheet } from '../components/rulesets/ImportRulesetSheet';
import { RulesetList } from '../components/rulesets/RulesetList';
import { useApiScorePermissions } from '../hooks/useApiScorePermissions';
import { useDeleteFunction, useImportFunction } from '../hooks/useFunctionMutations';
import { useDeleteRuleset, useImportRuleset, useUpdateRuleset } from '../hooks/useRulesetMutations';
import { useScoringFunctions } from '../hooks/useScoringFunctions';
import { useScoringRulesets } from '../hooks/useScoringRulesets';
import type { ImportFunctionRequest, ScoringFunction, ScoringRuleset } from '../types/apiScore';

type DeleteTarget = { kind: 'ruleset'; ruleset: ScoringRuleset } | { kind: 'function'; fn: ScoringFunction } | null;

export function ApiScoreRulesetsPage() {
    const { canCreate, canUpdate, canDelete } = useApiScorePermissions();
    const { data: rulesetsData, isLoading: isRulesetsLoading, isError: isRulesetsError } = useScoringRulesets();
    const { data: functionsData, isLoading: isFunctionsLoading, isError: isFunctionsError } = useScoringFunctions();

    const importRuleset = useImportRuleset();
    const updateRuleset = useUpdateRuleset();
    const deleteRuleset = useDeleteRuleset();
    const importFunction = useImportFunction();
    const deleteFunction = useDeleteFunction();

    const [importRulesetOpen, setImportRulesetOpen] = useState(false);
    const [importFunctionOpen, setImportFunctionOpen] = useState(false);
    const [editRuleset, setEditRuleset] = useState<ScoringRuleset | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<DeleteTarget>(null);
    const [pendingOverwrite, setPendingOverwrite] = useState<ImportFunctionRequest | null>(null);

    const rulesets = rulesetsData?.data ?? [];
    const functions = functionsData?.data ?? [];
    const isDeleting = deleteRuleset.isPending || deleteFunction.isPending;

    const submitFunctionImport = (request: ImportFunctionRequest) => {
        importFunction.mutate(request, {
            onSuccess: () => {
                notify.success('Function imported.');
                setImportFunctionOpen(false);
                setPendingOverwrite(null);
            },
            onError: error => notify.error(error, 'Unable to import function'),
        });
    };

    // Importing over an existing name replaces it — require an explicit confirmation first (classic parity).
    const handleFunctionImport = (request: ImportFunctionRequest) => {
        if (functions.some(fn => fn.name === request.name)) {
            setPendingOverwrite(request);
        } else {
            submitFunctionImport(request);
        }
    };

    const handleConfirmDelete = () => {
        if (!deleteTarget) return;
        if (deleteTarget.kind === 'ruleset') {
            deleteRuleset.mutate(deleteTarget.ruleset.id, {
                onSuccess: () => {
                    notify.success('Ruleset deleted.');
                    setDeleteTarget(null);
                },
                onError: error => notify.error(error, 'Unable to delete ruleset'),
            });
        } else {
            deleteFunction.mutate(deleteTarget.fn.name, {
                onSuccess: () => {
                    notify.success('Function deleted.');
                    setDeleteTarget(null);
                },
                onError: error => notify.error(error, 'Unable to delete function'),
            });
        }
    };

    return (
        <div className="space-y-4">
            <RulesetList
                rulesets={rulesets}
                isLoading={isRulesetsLoading}
                isError={isRulesetsError}
                canImport={canCreate}
                canEdit={canUpdate}
                canDelete={canDelete}
                onImport={() => setImportRulesetOpen(true)}
                onEdit={setEditRuleset}
                onDelete={ruleset => setDeleteTarget({ kind: 'ruleset', ruleset })}
            />

            <FunctionList
                functions={functions}
                isLoading={isFunctionsLoading}
                isError={isFunctionsError}
                canImport={canCreate}
                canDelete={canDelete}
                onImport={() => setImportFunctionOpen(true)}
                onDelete={fn => setDeleteTarget({ kind: 'function', fn })}
            />

            <ImportRulesetSheet
                open={importRulesetOpen}
                onOpenChange={setImportRulesetOpen}
                isSubmitting={importRuleset.isPending}
                onImport={request =>
                    importRuleset.mutate(request, {
                        onSuccess: () => {
                            notify.success('Ruleset imported.');
                            setImportRulesetOpen(false);
                        },
                        onError: error => notify.error(error, 'Unable to import ruleset'),
                    })
                }
            />

            <EditRulesetSheet
                ruleset={editRuleset}
                open={editRuleset !== null}
                onOpenChange={open => !open && setEditRuleset(null)}
                isSubmitting={updateRuleset.isPending}
                onSubmit={(rulesetId, request) =>
                    updateRuleset.mutate(
                        { rulesetId, request },
                        {
                            onSuccess: () => {
                                notify.success('Ruleset updated.');
                                setEditRuleset(null);
                            },
                            onError: error => notify.error(error, 'Unable to update ruleset'),
                        },
                    )
                }
            />

            <ImportFunctionSheet
                open={importFunctionOpen}
                onOpenChange={setImportFunctionOpen}
                existingNames={functions.map(fn => fn.name)}
                isSubmitting={importFunction.isPending}
                onImport={handleFunctionImport}
            />

            <ConfirmDialog
                open={deleteTarget !== null}
                onOpenChange={open => !open && setDeleteTarget(null)}
                title={deleteTarget?.kind === 'function' ? 'Delete this function?' : 'Delete this ruleset?'}
                description="This action cannot be undone."
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={isDeleting}
                icon={<Trash2Icon className="size-4" aria-hidden />}
                onConfirm={handleConfirmDelete}
            />

            <ConfirmDialog
                open={pendingOverwrite !== null}
                onOpenChange={open => !open && setPendingOverwrite(null)}
                title="Overwrite this function?"
                description={
                    pendingOverwrite
                        ? `A function named "${pendingOverwrite.name}" already exists. Importing will replace its content.`
                        : undefined
                }
                confirmLabel="Overwrite"
                pendingLabel="Overwriting…"
                destructive
                isPending={importFunction.isPending}
                onConfirm={() => pendingOverwrite && submitFunctionImport(pendingOverwrite)}
            />
        </div>
    );
}
