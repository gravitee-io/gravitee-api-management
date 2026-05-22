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
import { Plus, Sparkles } from 'lucide-react';
import { PolicyStatementCard, type ChipOption } from './PolicyStatementCard';
import { createEmptyStatement, type PolicyEffect, type PolicyStatement } from './statement-to-gapl';

export interface PolicyBuilderProps {
    readonly policyName: string;
    readonly target?: { id: string; label: string } | null;
    readonly statements: readonly PolicyStatement[];
    readonly principalOptions: readonly ChipOption[];
    readonly actionOptions: readonly ChipOption[];
    readonly resourceOptions: readonly ChipOption[];
    readonly resourceGroups: readonly { key: string; label: string }[];
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
    readonly onChange: (statements: readonly PolicyStatement[]) => void;
    /** Optional contextual empty-state hints forwarded to the chip pickers. */
    readonly emptyPrincipalsHint?: string;
    readonly emptyActionsHint?: string;
    readonly emptyResourcesHint?: string;
}

export function PolicyBuilder({
    policyName: _policyName,
    target: _target,
    statements,
    principalOptions,
    actionOptions,
    resourceOptions,
    resourceGroups,
    conditionSnippets,
    onChange,
    emptyPrincipalsHint,
    emptyActionsHint,
    emptyResourcesHint,
}: PolicyBuilderProps) {
    // GAPL generation is owned by the parent (PolicyEditorSheet) which memoises
    // `generatedCode` from (name, statements, target) and pushes it into the
    // code buffer when needed. PolicyBuilder is now a pure controlled list of
    // statements — it just calls `onChange` and never touches the GAPL string.
    // The unused `policyName` / `target` props are preserved to keep the prop
    // contract stable for callers and tests; they are intentionally read with
    // an underscore prefix to make the no-op explicit.
    const regenerate = (next: readonly PolicyStatement[]) => onChange(next);

    const addStatement = (effect: PolicyEffect) => regenerate([...statements, createEmptyStatement(effect)]);

    const updateStatement = (next: PolicyStatement) => regenerate(statements.map(s => (s.id === next.id ? next : s)));

    const duplicateStatement = (index: number) => {
        const source = statements[index];
        const copy: PolicyStatement = {
            ...source,
            // eslint-disable-next-line react-hooks/purity -- event handler, not render path
            id: `stmt-${Math.random().toString(36).slice(2, 9)}`,
        };
        const next = [...statements];
        next.splice(index + 1, 0, copy);
        regenerate(next);
    };

    const removeStatement = (id: string) => regenerate(statements.filter(s => s.id !== id));

    const moveUp = (index: number) => {
        if (index === 0) return;
        const next = [...statements];
        [next[index - 1], next[index]] = [next[index], next[index - 1]];
        regenerate(next);
    };

    const moveDown = (index: number) => {
        if (index === statements.length - 1) return;
        const next = [...statements];
        [next[index], next[index + 1]] = [next[index + 1], next[index]];
        regenerate(next);
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
            <div className="flex items-center gap-2 pt-1">
                <Button type="button" variant="outline" size="sm" onClick={() => addStatement('permit')}>
                    <Plus className="size-3.5 mr-1.5" />
                    Add permit statement
                </Button>
                <Button type="button" variant="outline" size="sm" onClick={() => addStatement('forbid')}>
                    <Plus className="size-3.5 mr-1.5" />
                    Add forbid statement
                </Button>
                <span className="ml-auto inline-flex items-center gap-1 text-muted-foreground" style={{ fontSize: '11px' }}>
                    <Sparkles className="size-3" />
                    Deny-by-default: anything not permitted is denied.
                </span>
            </div>
        </div>
    );
}
