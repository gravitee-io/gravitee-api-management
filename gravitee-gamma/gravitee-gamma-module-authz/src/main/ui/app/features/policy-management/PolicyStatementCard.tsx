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
    Label,
    Textarea,
    ToggleGroup,
    ToggleGroupItem,
    cn,
} from '@gravitee/graphene-core';
import { ChevronDownIcon, CopyIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { ChipCombobox, GAPL_UID_PATTERN } from '../../../components/ChipCombobox';
import type { ActionRef, PolicyEffect, PolicyStatement, PrincipalRef, ResourceRef } from './statement-to-gapl';

export interface ChipOption {
    readonly id: string;
    readonly label: string;
    readonly group: string;
    readonly description?: string;
}

export interface PolicyStatementCardProps {
    readonly index: number;
    readonly statement: PolicyStatement;
    readonly principalOptions: readonly ChipOption[];
    readonly actionOptions: readonly ChipOption[];
    readonly resourceOptions: readonly ChipOption[];
    readonly resourceGroups: readonly { key: string; label: string }[];
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
    readonly onChange: (next: PolicyStatement) => void;
    readonly onDuplicate: () => void;
    readonly onDelete: () => void;
    readonly canMoveUp?: boolean;
    readonly canMoveDown?: boolean;
    readonly onMoveUp?: () => void;
    readonly onMoveDown?: () => void;
    /**
     * Optional contextual empty-state hints displayed inside the chip
     * combobox when the corresponding option list is empty. Without these
     * the user sees only the generic "No results." copy and cannot tell
     * whether the picker is broken or simply has no data to draw from.
     */
    readonly emptyPrincipalsHint?: string;
    readonly emptyActionsHint?: string;
    readonly emptyResourcesHint?: string;
}

const DEFAULT_CONDITION_SNIPPETS: readonly { label: string; snippet: string }[] = [
    { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
    { label: 'Trusted device', snippet: 'context.device.trusted == true' },
    { label: 'Owner only', snippet: 'resource.owner == principal' },
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
];

export function PolicyStatementCard({
    index,
    statement,
    principalOptions,
    actionOptions,
    resourceOptions,
    resourceGroups,
    conditionSnippets,
    onChange,
    onDuplicate,
    onDelete,
    canMoveUp,
    canMoveDown,
    onMoveUp,
    onMoveDown,
    emptyPrincipalsHint,
    emptyActionsHint,
    emptyResourcesHint,
}: PolicyStatementCardProps) {
    const [conditionOpen, setConditionOpen] = useState(Boolean((statement.condition ?? '').trim()));

    const setEffect = (effect: PolicyEffect) => onChange({ ...statement, effect });

    const effectBorderClass =
        statement.effect === 'permit'
            ? 'border-success/40 ring-1 ring-success/20'
            : 'border-destructive/40 ring-1 ring-destructive/20';

    // Parse a canonical GAPL uid (`Type::"label"`) into its parts so we can
    // keep references to entities that no longer exist in the catalog —
    // otherwise a typo in Code view would silently drop the chip on every
    // round-trip, which is harder to debug than leaving a flagged ghost chip.
    const parseUid = (id: string): { kind: string; label: string } => {
        const m = id.match(GAPL_UID_PATTERN);
        return m ? { kind: m[1]!, label: m[2]! } : { kind: 'Unknown', label: id };
    };

    const syncPrincipals = (ids: string[]) => {
        const next = ids.map(id => {
            const opt = principalOptions.find(p => p.id === id);
            if (opt) return { id: opt.id, kind: opt.group, label: opt.label } as PrincipalRef;
            const { kind, label } = parseUid(id);
            return { id, kind, label } as PrincipalRef;
        });
        onChange({ ...statement, principals: next });
    };

    const syncActions = (ids: string[]) => {
        const next = ids.map(id => {
            const opt = actionOptions.find(a => a.id === id);
            if (opt) return { id: opt.id, label: opt.label, kind: opt.group } as ActionRef;
            const { kind, label } = parseUid(id);
            return { id, label, kind } as ActionRef;
        });
        onChange({ ...statement, actions: next });
    };

    const syncResources = (ids: string[]) => {
        const next = ids.map(id => {
            const opt = resourceOptions.find(r => r.id === id);
            if (opt) return { id: opt.id, kind: opt.group, label: opt.label } as ResourceRef;
            const { kind, label } = parseUid(id);
            return { id, kind, label } as ResourceRef;
        });
        onChange({ ...statement, resources: next });
    };

    const appendCondition = (snippet: string) => {
        const trimmed = (statement.condition || '').trim();
        const next = trimmed ? `${trimmed}\n&& ${snippet}` : snippet;
        onChange({ ...statement, condition: next });
    };

    const hasCondition = Boolean((statement.condition ?? '').trim());
    const snippets = conditionSnippets ?? DEFAULT_CONDITION_SNIPPETS;

    return (
        <div className={cn('rounded-lg border bg-card p-3 shadow-sm', effectBorderClass)}>
            <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                    <ToggleGroup
                        type="single"
                        variant="outline"
                        size="sm"
                        value={statement.effect}
                        onValueChange={value => value && setEffect(value as PolicyEffect)}
                        className="text-xs"
                    >
                        <ToggleGroupItem
                            value="permit"
                            className={cn(
                                statement.effect === 'permit' &&
                                    'bg-success/10 text-success border-success font-semibold hover:bg-success/10 hover:text-success',
                            )}
                        >
                            permit
                        </ToggleGroupItem>
                        <ToggleGroupItem
                            value="forbid"
                            className={cn(
                                statement.effect === 'forbid' &&
                                    'bg-destructive/10 text-destructive border-destructive font-semibold hover:bg-destructive/10 hover:text-destructive',
                            )}
                        >
                            forbid
                        </ToggleGroupItem>
                    </ToggleGroup>
                    <span className="text-xs text-muted-foreground">
                        #{index + 1}
                    </span>
                </div>

                <div className="flex items-center gap-1">
                    {/* Move up / down arrows */}
                    {canMoveUp && (
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={onMoveUp}
                            aria-label="Move statement up"
                            className="h-7 w-7 p-0"
                        >
                            ↑
                        </Button>
                    )}
                    {canMoveDown && (
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={onMoveDown}
                            aria-label="Move statement down"
                            className="h-7 w-7 p-0"
                        >
                            ↓
                        </Button>
                    )}
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={onDuplicate}
                        aria-label="Duplicate statement"
                        className="h-7 w-7 p-0"
                    >
                        <CopyIcon className="size-3.5" aria-hidden />
                    </Button>
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={onDelete}
                        aria-label="Delete statement"
                        className="h-7 w-7 p-0 text-destructive hover:text-destructive"
                    >
                        <Trash2Icon className="size-3.5" aria-hidden />
                    </Button>
                </div>
            </div>

            <div className="mt-2 grid grid-cols-1 gap-2 md:grid-cols-3">
                <ChipField label="Principal">
                    <ChipCombobox
                        placeholder="Add principal"
                        options={principalOptions}
                        selectedIds={statement.principals.map(p => p.id)}
                        onChange={syncPrincipals}
                        groupOrder={['User', 'Group', 'ServiceAccount', 'AgentIdentity']}
                        emptyHint={emptyPrincipalsHint}
                    />
                </ChipField>

                <ChipField label="Action">
                    <ChipCombobox
                        placeholder="Add action"
                        options={actionOptions}
                        selectedIds={statement.actions.map(a => a.id)}
                        onChange={syncActions}
                        groupOrder={['Action']}
                        emptyHint={emptyActionsHint}
                    />
                </ChipField>

                <ChipField label="Resource">
                    <ChipCombobox
                        placeholder="Add resource"
                        options={resourceOptions}
                        selectedIds={statement.resources.map(r => r.id)}
                        onChange={syncResources}
                        groupOrder={resourceGroups.map(g => g.key)}
                        emptyHint={emptyResourcesHint}
                    />
                </ChipField>
            </div>

            <div className="mt-2">
                <button
                    type="button"
                    onClick={() => setConditionOpen(o => !o)}
                    className="flex w-full items-center gap-1.5 rounded-md px-1 py-0.5 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground hover:text-foreground"
                >
                    <ChevronDownIcon className={cn('size-3 transition-transform', conditionOpen ? 'rotate-0' : '-rotate-90')} aria-hidden />
                    <span>Condition</span>
                    <span className="text-[10px] normal-case tracking-normal text-muted-foreground/70">
                        Optional
                    </span>
                    {!conditionOpen && hasCondition ? (
                        <span
                            className="ml-auto max-w-[60%] truncate font-mono text-xs normal-case tracking-normal text-foreground/70"
                        >
                            {statement.condition}
                        </span>
                    ) : null}
                </button>
                {conditionOpen ? (
                    <div className="mt-1.5 space-y-1.5">
                        <Textarea
                            value={statement.condition ?? ''}
                            onChange={e => onChange({ ...statement, condition: e.target.value })}
                            placeholder="e.g. context.time.hour >= 9 && context.time.hour < 17"
                            className="min-h-12 font-mono text-xs leading-relaxed"
                        />
                        <div className="flex flex-wrap gap-1">
                            {snippets.map(s => (
                                <button
                                    key={s.label}
                                    type="button"
                                    onClick={() => appendCondition(s.snippet)}
                                    className="rounded border border-dashed px-1.5 py-0.5 text-xs text-muted-foreground hover:border-foreground/40 hover:text-foreground"
                                >
                                    + {s.label}
                                </button>
                            ))}
                        </div>
                    </div>
                ) : null}
            </div>
        </div>
    );
}

function ChipField({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <div className="space-y-1">
            <Label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {label}
            </Label>
            {children}
        </div>
    );
}

