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
import { PlusIcon, SparklesIcon } from '@gravitee/graphene-core/icons';
import type { ChipOption } from '../../shared/chip-option';
import { PolicyStatementCard } from './PolicyStatementCard';
import { createEmptyStatement, makeStatementId, type PolicyEffect, type PolicyStatement } from './statement-to-gapl';

export interface PolicyBuilderProps {
    readonly statements: readonly PolicyStatement[];
    readonly principalOptions: readonly ChipOption[];
    readonly actionOptions: readonly ChipOption[];
    readonly resourceOptions: readonly ChipOption[];
    readonly agentOptions: readonly ChipOption[];
    readonly resourceGroups: readonly { key: string; label: string }[];
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
    readonly onChange: (statements: readonly PolicyStatement[]) => void;
    readonly emptyPrincipalsHint?: string;
    readonly emptyActionsHint?: string;
    readonly emptyResourcesHint?: string;
}

export function PolicyBuilder({
    statements,
    principalOptions,
    actionOptions,
    resourceOptions,
    agentOptions,
    resourceGroups,
    conditionSnippets,
    onChange,
    emptyPrincipalsHint,
    emptyActionsHint,
    emptyResourcesHint,
}: PolicyBuilderProps) {
    const addStatement = (effect: PolicyEffect) => onChange([...statements, createEmptyStatement(effect)]);

    const updateStatement = (next: PolicyStatement) => onChange(statements.map(s => (s.id === next.id ? next : s)));

    const duplicateStatement = (index: number) => {
        const source = statements[index];
        const copy: PolicyStatement = {
            ...source,
            id: makeStatementId(),
        };
        const next = [...statements];
        next.splice(index + 1, 0, copy);
        onChange(next);
    };

    const removeStatement = (id: string) => onChange(statements.filter(s => s.id !== id));

    const moveUp = (index: number) => {
        if (index === 0) return;
        const next = [...statements];
        [next[index - 1], next[index]] = [next[index], next[index - 1]];
        onChange(next);
    };

    const moveDown = (index: number) => {
        if (index === statements.length - 1) return;
        const next = [...statements];
        [next[index], next[index + 1]] = [next[index + 1], next[index]];
        onChange(next);
    };

    return (
        <div className="flex flex-col gap-3">
            {statements.length === 0 && (
                <div className="rounded-lg border border-dashed bg-muted/20 p-6 text-center text-sm text-muted-foreground">
                    No statements yet. Add a permit or forbid statement below.
                </div>
            )}
            {statements.map((s, idx) => (
                <PolicyStatementCard
                    key={s.id}
                    index={idx}
                    statement={s}
                    principalOptions={principalOptions}
                    actionOptions={actionOptions}
                    resourceOptions={resourceOptions}
                    agentOptions={agentOptions}
                    resourceGroups={resourceGroups}
                    conditionSnippets={conditionSnippets}
                    onChange={updateStatement}
                    onDuplicate={() => duplicateStatement(idx)}
                    onDelete={() => removeStatement(s.id)}
                    canMoveUp={idx > 0}
                    canMoveDown={idx < statements.length - 1}
                    onMoveUp={() => moveUp(idx)}
                    onMoveDown={() => moveDown(idx)}
                    emptyPrincipalsHint={emptyPrincipalsHint}
                    emptyActionsHint={emptyActionsHint}
                    emptyResourcesHint={emptyResourcesHint}
                />
            ))}
            <div className="flex flex-col items-start gap-1.5 pt-1">
                <Button
                    type="button"
                    variant="outline"
                    size="xs"
                    onClick={() => addStatement('permit')}
                    className="border-success/40 text-success hover:bg-success/10 hover:text-success"
                >
                    <PlusIcon className="size-3 mr-1" />
                    Add permit statement
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    size="xs"
                    onClick={() => addStatement('forbid')}
                    className="border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"
                >
                    <PlusIcon className="size-3 mr-1" />
                    Add forbid statement
                </Button>
                <p className="inline-flex items-center gap-1 pt-1 text-xs text-muted-foreground">
                    <SparklesIcon className="size-3" aria-hidden />
                    forbid wins — anything matched by a forbid is denied even when also matched by a permit.
                </p>
            </div>
        </div>
    );
}
