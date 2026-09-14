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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, DataTableEmptyState, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify';
import { FunctionAccordion } from '../components/FunctionAccordion';
import { RulesetAccordion } from '../components/RulesetAccordion';
import { useDeleteScoringFunction, useScoringFunctions } from '../hooks/useScoringFunctions';
import { useDeleteScoringRuleset, useScoringRulesets } from '../hooks/useScoringRulesets';
import type { ScoringFunction, ScoringRuleset } from '../types/rulesets';

const RULESETS_ERROR = 'Rulesets error!';
const FUNCTIONS_ERROR = 'Functions error!';

type PendingDelete = { kind: 'ruleset'; ruleset: ScoringRuleset } | { kind: 'function'; fn: ScoringFunction } | null;

function ScoringEmptyState({ title }: Readonly<{ title: string }>) {
    return (
        <div className="rounded-lg border">
            <DataTableEmptyState variant="first-use" icon={<SearchIcon className="size-8" aria-hidden />} title={title} description="" />
        </div>
    );
}

export function ApiScoreRulesetsPage() {
    const rulesetsQuery = useScoringRulesets();
    const functionsQuery = useScoringFunctions();
    const deleteRuleset = useDeleteScoringRuleset();
    const deleteFunction = useDeleteScoringFunction();
    const [pendingDelete, setPendingDelete] = useState<PendingDelete>(null);

    useEffect(() => {
        if (!rulesetsQuery.isError) return;
        notify.error(rulesetsQuery.error, RULESETS_ERROR);
    }, [rulesetsQuery.error, rulesetsQuery.isError]);

    useEffect(() => {
        if (!functionsQuery.isError) return;
        notify.error(functionsQuery.error, FUNCTIONS_ERROR);
    }, [functionsQuery.error, functionsQuery.isError]);

    async function confirmDelete() {
        if (pendingDelete === null) return;
        try {
            if (pendingDelete.kind === 'ruleset') {
                await deleteRuleset.mutateAsync(pendingDelete.ruleset.id);
                notify.success('Ruleset successfully deleted!');
            } else {
                await deleteFunction.mutateAsync(pendingDelete.fn.name);
                notify.success('Function successfully deleted!');
            }
            setPendingDelete(null);
        } catch (error) {
            notify.error(error, pendingDelete.kind === 'ruleset' ? 'Ruleset deletion error!' : 'Function deletion error!');
        }
    }

    const isDeletePending = deleteRuleset.isPending || deleteFunction.isPending;

    return (
        <div className="space-y-6" data-testid="api-score-rulesets-page">
            <Card data-testid="rulesets-card">
                <CardHeader>
                    <div className="flex items-start justify-between gap-4">
                        <div className="space-y-1.5">
                            <CardTitle>Rulesets</CardTitle>
                            <CardDescription>
                                Custom rulesets allow you to enforce your organization&apos;s API design, quality and security standards.
                            </CardDescription>
                        </div>
                        <Button asChild size="sm" className="shrink-0">
                            <Link to="import" relative="path" data-testid="import-ruleset-link">
                                <PlusIcon className="size-4" aria-hidden />
                                Import
                            </Link>
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    {rulesetsQuery.isLoading ? <Skeleton className="h-24 w-full rounded-lg" /> : null}
                    {!rulesetsQuery.isLoading && !rulesetsQuery.isError && rulesetsQuery.rulesets.length === 0 ? (
                        <ScoringEmptyState title="No ruleset, yet" />
                    ) : null}
                    {!rulesetsQuery.isLoading && rulesetsQuery.rulesets.length > 0 ? (
                        <RulesetAccordion
                            rulesets={rulesetsQuery.rulesets}
                            onDelete={ruleset => setPendingDelete({ kind: 'ruleset', ruleset })}
                        />
                    ) : null}
                </CardContent>
            </Card>

            <Card data-testid="functions-card">
                <CardHeader>
                    <div className="flex items-start justify-between gap-4">
                        <div className="space-y-1.5">
                            <CardTitle>Functions</CardTitle>
                            <CardDescription>
                                Custom functions let you define specific logic or operations that extend the rulesets.
                            </CardDescription>
                        </div>
                        <Button asChild size="sm" className="shrink-0">
                            <Link to="import-function" relative="path" data-testid="import-function-link">
                                <PlusIcon className="size-4" aria-hidden />
                                Import
                            </Link>
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    {functionsQuery.isLoading ? <Skeleton className="h-24 w-full rounded-lg" /> : null}
                    {!functionsQuery.isLoading && !functionsQuery.isError && functionsQuery.functions.length === 0 ? (
                        <ScoringEmptyState title="No custom function, yet" />
                    ) : null}
                    {!functionsQuery.isLoading && functionsQuery.functions.length > 0 ? (
                        <FunctionAccordion
                            functions={functionsQuery.functions}
                            onDelete={fn => setPendingDelete({ kind: 'function', fn })}
                        />
                    ) : null}
                </CardContent>
            </Card>

            <ConfirmDialog
                open={pendingDelete?.kind === 'ruleset'}
                onOpenChange={open => {
                    if (!open) setPendingDelete(null);
                }}
                title="Delete this ruleset"
                description="Please note that once your ruleset is deleted, it cannot be restored."
                confirmLabel="Delete ruleset"
                destructive
                isPending={isDeletePending}
                onConfirm={() => void confirmDelete()}
            />
            <ConfirmDialog
                open={pendingDelete?.kind === 'function'}
                onOpenChange={open => {
                    if (!open) setPendingDelete(null);
                }}
                title="Delete this function"
                description="Please note that once your function is deleted, it cannot be restored."
                confirmLabel="Delete function"
                destructive
                isPending={isDeletePending}
                onConfirm={() => void confirmDelete()}
            />
        </div>
    );
}
