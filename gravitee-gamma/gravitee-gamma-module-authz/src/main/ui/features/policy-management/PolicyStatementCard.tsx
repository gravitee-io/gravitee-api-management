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
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxGroup,
    ComboboxItem,
    ComboboxLabel,
    ComboboxList,
    Label,
    Textarea,
    ToggleGroup,
    ToggleGroupItem,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
    cn,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon, CopyIcon, PlusIcon, Trash2Icon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import type { ChipOption } from '../../shared/chip-option';
import { SLUG_REGEX, slugify, type InlineCreateConfig } from './inline-entity-create';
import type { ActionRef, PolicyEffect, PolicyStatement, PrincipalRef, ResourceRef } from './statement-to-gapl';

const GAPL_UID_PATTERN = /^([^:]+)::"(.+)"$/;

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
    readonly emptyPrincipalsHint?: string;
    readonly emptyActionsHint?: string;
    readonly emptyResourcesHint?: string;
    readonly principalCreate?: InlineCreateConfig;
    readonly resourceCreate?: InlineCreateConfig;
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
    principalCreate,
    resourceCreate,
}: PolicyStatementCardProps) {
    const [conditionOpen, setConditionOpen] = useState(Boolean((statement.condition ?? '').trim()));

    const setEffect = (effect: PolicyEffect) => onChange({ ...statement, effect });

    const effectBorderClass =
        statement.effect === 'permit' ? 'border-success/40 ring-1 ring-success/20' : 'border-destructive/40 ring-1 ring-destructive/20';

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
        <TooltipProvider>
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
                    <span className="text-xs text-muted-foreground">#{index + 1}</span>
                </div>

                <div className="flex items-center gap-1">
                    {canMoveUp && (
                        <Button type="button" variant="ghost" size="icon-sm" onClick={onMoveUp} aria-label="Move statement up">
                            <ChevronUpIcon className="size-3.5" aria-hidden />
                        </Button>
                    )}
                    {canMoveDown && (
                        <Button type="button" variant="ghost" size="icon-sm" onClick={onMoveDown} aria-label="Move statement down">
                            <ChevronDownIcon className="size-3.5" aria-hidden />
                        </Button>
                    )}
                    <Button type="button" variant="ghost" size="icon-sm" onClick={onDuplicate} aria-label="Duplicate statement">
                        <CopyIcon className="size-3.5" aria-hidden />
                    </Button>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        onClick={onDelete}
                        aria-label="Delete statement"
                        className="text-destructive hover:text-destructive"
                    >
                        <Trash2Icon className="size-3.5" aria-hidden />
                    </Button>
                </div>
            </div>

            <div className="mt-2 grid grid-cols-1 gap-2 md:grid-cols-3">
                <ChipField label="Principal">
                    <ChipMultiCombobox
                        placeholder="Add principal"
                        options={principalOptions}
                        selectedIds={statement.principals.map(p => p.id)}
                        onChange={syncPrincipals}
                        groupOrder={['User', 'Group', 'ServiceAccount', 'AgentIdentity']}
                        emptyHint={emptyPrincipalsHint}
                        createConfig={principalCreate}
                    />
                </ChipField>

                <ChipField label="Action">
                    <ChipMultiCombobox
                        placeholder="Add action"
                        options={actionOptions}
                        selectedIds={statement.actions.map(a => a.id)}
                        onChange={syncActions}
                        groupOrder={['Action']}
                        emptyHint={emptyActionsHint}
                    />
                </ChipField>

                <ChipField label="Resource">
                    <ChipMultiCombobox
                        placeholder="Add resource"
                        options={resourceOptions}
                        selectedIds={statement.resources.map(r => r.id)}
                        onChange={syncResources}
                        groupOrder={resourceGroups.map(g => g.key)}
                        emptyHint={emptyResourcesHint}
                        createConfig={resourceCreate}
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
                    <span className="text-xs normal-case tracking-normal text-muted-foreground/70">Optional</span>
                    {!conditionOpen && hasCondition ? (
                        <span className="ml-auto max-w-[60%] truncate font-mono text-xs normal-case tracking-normal text-foreground/70">
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
                                    className="rounded border border-dashed px-1.5 py-0.5 text-xs text-muted-foreground hover:border-foreground/40 hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                                >
                                    + {s.label}
                                </button>
                            ))}
                        </div>
                    </div>
                ) : null}
            </div>
        </div>
        </TooltipProvider>
    );
}

function ChipField({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <div className="space-y-1">
            <Label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</Label>
            {children}
        </div>
    );
}

interface ChipMultiComboboxProps {
    readonly placeholder: string;
    readonly options: readonly ChipOption[];
    readonly selectedIds: readonly string[];
    readonly onChange: (ids: string[]) => void;
    readonly groupOrder?: readonly string[];
    readonly emptyHint?: string;
    readonly createConfig?: InlineCreateConfig;
}

function ChipMultiCombobox({ placeholder, options, selectedIds, onChange, groupOrder, emptyHint, createConfig }: ChipMultiComboboxProps) {
    const anchorRef = useComboboxAnchor();
    const [query, setQuery] = useState('');

    const matchesQuery = (option: ChipOption, needle: string): boolean => {
        if (!needle) return true;
        return (
            option.label.toLowerCase().includes(needle) ||
            option.id.toLowerCase().includes(needle) ||
            (option.description?.toLowerCase().includes(needle) ?? false)
        );
    };

    const grouped = useMemo(() => {
        const needle = query.trim().toLowerCase();
        const matching = needle ? options.filter(o => matchesQuery(o, needle)) : options;
        const ordered = groupOrder ?? [...new Set(matching.map(o => o.group))];
        const seen = new Set<string>();
        const groups: Array<{ key: string; items: ChipOption[] }> = [];
        for (const group of ordered) {
            const items = matching.filter(o => o.group === group);
            if (items.length > 0) {
                groups.push({ key: group, items });
                seen.add(group);
            }
        }
        for (const o of matching) {
            if (!seen.has(o.group)) {
                seen.add(o.group);
                groups.push({ key: o.group, items: matching.filter(x => x.group === o.group) });
            }
        }
        return groups;
    }, [options, groupOrder, query]);

    const hasVisibleItems = grouped.some(g => g.items.length > 0);

    const labelOf = (id: string): { label: string; ghost: boolean } => {
        const opt = options.find(o => o.id === id);
        if (opt) return { label: opt.label, ghost: false };
        const m = id.match(GAPL_UID_PATTERN);
        return { label: m ? m[2]! : id, ghost: true };
    };

    const values = selectedIds as string[];

    return (
        <Combobox
            multiple
            value={values}
            onValueChange={next => onChange(next as string[])}
            inputValue={query}
            onInputValueChange={setQuery}
            autoComplete="none"
        >
            <ComboboxChips ref={anchorRef}>
                {values.map(id => {
                    const { label, ghost } = labelOf(id);
                    const chip = (
                        <ComboboxChip
                            key={id}
                            removeAriaLabel={`Remove ${label}`}
                            className={cn(ghost && 'border border-dashed border-warning bg-warning/10 text-warning')}
                        >
                            {ghost ? <TriangleAlertIcon className="size-3" aria-hidden /> : null}
                            {label}
                        </ComboboxChip>
                    );
                    if (!ghost) return chip;
                    return (
                        <Tooltip key={id}>
                            <TooltipTrigger asChild>{chip}</TooltipTrigger>
                            <TooltipContent>Defined only in this policy. Add it under Entities to reuse it.</TooltipContent>
                        </Tooltip>
                    );
                })}
                <ComboboxChipsInput placeholder={values.length === 0 ? placeholder : 'Type to filter…'} aria-label={placeholder} />
            </ComboboxChips>
            <ComboboxContent anchor={anchorRef} className="max-h-72 min-w-60" style={{ pointerEvents: 'auto' }}>
                <ComboboxList>
                    {!hasVisibleItems && (
                        <ComboboxEmpty>
                            {options.length === 0 && emptyHint ? emptyHint : query.trim() ? 'No matches.' : 'No options.'}
                        </ComboboxEmpty>
                    )}
                    {grouped.map(group => (
                        <ComboboxGroup key={group.key}>
                            <ComboboxLabel>{group.key}</ComboboxLabel>
                            {group.items.map(item => (
                                <ComboboxItem key={item.id} value={item.id}>
                                    <div className="min-w-0 flex-1">
                                        <p className="truncate text-sm">{item.label}</p>
                                        {item.description ? (
                                            <p className="truncate text-xs text-muted-foreground">{item.description}</p>
                                        ) : null}
                                    </div>
                                </ComboboxItem>
                            ))}
                        </ComboboxGroup>
                    ))}
                </ComboboxList>
                {createConfig ? (
                    <InlineCreatePanel
                        createConfig={createConfig}
                        query={query}
                        existingLabels={options}
                        onCreated={opt => {
                            if (!values.includes(opt.id)) onChange([...values, opt.id]);
                            setQuery('');
                        }}
                    />
                ) : null}
            </ComboboxContent>
        </Combobox>
    );
}

interface InlineCreatePanelProps {
    readonly createConfig: InlineCreateConfig;
    readonly query: string;
    readonly existingLabels: readonly ChipOption[];
    readonly onCreated: (option: ChipOption) => void;
}

function InlineCreatePanel({ createConfig, query, existingLabels, onCreated }: InlineCreatePanelProps) {
    const [canonical, setCanonical] = useState(createConfig.defaultCanonical);

    const displayName = query.trim();
    const slug = slugify(displayName);
    const exactMatch = displayName !== '' && existingLabels.some(o => o.label.toLowerCase() === displayName.toLowerCase());

    if (displayName === '' || exactMatch) return null;

    const slugInvalid = !slug || !SLUG_REGEX.test(slug);

    const runCreate = () => {
        if (slugInvalid) return;
        onCreated(createConfig.create({ canonicalPrefix: canonical, slug, displayName }));
    };

    return (
        <div className="border-t px-2 py-2">
            <p className="px-1 pb-1 text-xs font-medium text-muted-foreground">Add to this policy only</p>
            <div className="flex flex-wrap gap-1 px-1">
                {createConfig.presets.map(preset => (
                    <button
                        key={preset.canonical}
                        type="button"
                        onMouseDown={e => e.preventDefault()}
                        onClick={() => setCanonical(preset.canonical)}
                        className={cn(
                            'rounded border px-1.5 py-0.5 text-xs',
                            canonical === preset.canonical
                                ? 'border-primary bg-primary/10 text-primary'
                                : 'border-dashed text-muted-foreground hover:border-foreground/40 hover:text-foreground',
                        )}
                    >
                        {preset.label}
                    </button>
                ))}
            </div>
            <Button
                type="button"
                variant="outline"
                size="xs"
                onMouseDown={e => e.preventDefault()}
                onClick={runCreate}
                disabled={slugInvalid}
                className="mt-1.5 w-full justify-start"
            >
                <PlusIcon className="mr-1 size-3" aria-hidden />
                <span className="truncate">
                    Add <span className="font-mono">{canonical}.{slug || '…'}</span>
                </span>
            </Button>
        </div>
    );
}
