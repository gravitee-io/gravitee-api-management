import {
    Badge,
    Button,
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    Label,
    Popover,
    PopoverContent,
    PopoverTrigger,
    Textarea,
    cn,
} from '@gravitee/graphene-core';
import { Check, ChevronDown, Copy, Trash2 } from 'lucide-react';
import { useId, useState } from 'react';
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
        statement.effect === 'permit' ? 'border-emerald-500/40 ring-1 ring-emerald-500/20' : 'border-red-500/40 ring-1 ring-red-500/20';

    const syncPrincipals = (ids: string[]) => {
        const next = ids
            .map(id => principalOptions.find(p => p.id === id))
            .filter((p): p is ChipOption => p !== undefined)
            .map(p => ({ id: p.id, kind: p.group, label: p.label }) as PrincipalRef);
        onChange({ ...statement, principals: next });
    };

    const syncActions = (ids: string[]) => {
        const next = ids
            .map(id => actionOptions.find(a => a.id === id))
            .filter((a): a is ChipOption => a !== undefined)
            // The chip option's `group` carries the source namespace (e.g.
            // 'Action') so the serialiser can preserve casing on round-trip.
            .map(a => ({ id: a.id, label: a.label, kind: a.group }) as ActionRef);
        onChange({ ...statement, actions: next });
    };

    const syncResources = (ids: string[]) => {
        const next = ids
            .map(id => resourceOptions.find(r => r.id === id))
            .filter((r): r is ChipOption => r !== undefined)
            .map(r => ({ id: r.id, kind: r.group, label: r.label }) as ResourceRef);
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
                    <div className="inline-flex overflow-hidden rounded-md border text-xs">
                        <button
                            type="button"
                            onClick={() => setEffect('permit')}
                            className={cn(
                                'px-2.5 py-1 transition',
                                statement.effect === 'permit'
                                    ? 'bg-emerald-500/15 font-medium text-emerald-700 dark:text-emerald-300'
                                    : 'text-muted-foreground hover:bg-muted',
                            )}
                        >
                            permit
                        </button>
                        <button
                            type="button"
                            onClick={() => setEffect('forbid')}
                            className={cn(
                                'border-l px-2.5 py-1 transition',
                                statement.effect === 'forbid'
                                    ? 'bg-red-500/15 font-medium text-red-700 dark:text-red-300'
                                    : 'text-muted-foreground hover:bg-muted',
                            )}
                        >
                            forbid
                        </button>
                    </div>
                    <span className="text-muted-foreground" style={{ fontSize: '11px' }}>
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
                        <Copy className="size-3.5" />
                    </Button>
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={onDelete}
                        aria-label="Delete statement"
                        className="h-7 w-7 p-0 text-destructive hover:text-destructive"
                    >
                        <Trash2 className="size-3.5" />
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
                    className="flex w-full items-center gap-1.5 rounded-md px-1 py-0.5 text-left font-medium uppercase tracking-wide text-muted-foreground hover:text-foreground"
                    style={{ fontSize: '11px' }}
                >
                    <ChevronDown className={cn('size-3 transition-transform', conditionOpen ? 'rotate-0' : '-rotate-90')} />
                    <span>Condition</span>
                    <span className="normal-case tracking-normal text-muted-foreground/70" style={{ fontSize: '10px' }}>
                        Optional
                    </span>
                    {!conditionOpen && hasCondition ? (
                        <span
                            className="ml-auto max-w-[60%] truncate font-mono normal-case tracking-normal text-foreground/70"
                            style={{ fontSize: '11px' }}
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
                            className="min-h-[48px] font-mono leading-relaxed"
                            style={{ fontSize: '12px' }}
                        />
                        <div className="flex flex-wrap gap-1">
                            {snippets.map(s => (
                                <button
                                    key={s.label}
                                    type="button"
                                    onClick={() => appendCondition(s.snippet)}
                                    className="rounded border border-dashed px-1.5 py-0.5 text-muted-foreground hover:border-foreground/40 hover:text-foreground"
                                    style={{ fontSize: '11px' }}
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
            <Label className="font-medium uppercase tracking-wide text-muted-foreground" style={{ fontSize: '11px' }}>
                {label}
            </Label>
            {children}
        </div>
    );
}

/** Multi-select chip combobox using Popover + Command from graphene-core */
function ChipCombobox({
    placeholder,
    options,
    selectedIds,
    onChange,
    groupOrder,
    emptyHint,
}: {
    placeholder: string;
    options: readonly ChipOption[];
    selectedIds: readonly string[];
    onChange: (ids: string[]) => void;
    groupOrder?: readonly string[];
    emptyHint?: string;
}) {
    const [open, setOpen] = useState(false);
    const listboxId = useId();

    const selectedSet = new Set(selectedIds);

    const toggle = (id: string) => {
        if (selectedSet.has(id)) {
            onChange(selectedIds.filter(x => x !== id));
        } else {
            onChange([...selectedIds, id]);
        }
    };

    const selectedOptions = selectedIds.map(id => options.find(o => o.id === id)).filter((o): o is ChipOption => o !== undefined);

    // Group options
    const grouped: Array<{ group: string; items: ChipOption[] }> = [];
    const seenGroups = new Set<string>();
    const orderedGroups = groupOrder ?? [...new Set(options.map(o => o.group))];

    for (const group of orderedGroups) {
        const items = options.filter(o => o.group === group);
        if (items.length > 0) {
            grouped.push({ group, items });
            seenGroups.add(group);
        }
    }
    // Add remaining groups not in orderedGroups
    for (const o of options) {
        if (!seenGroups.has(o.group)) {
            const items = options.filter(x => x.group === o.group);
            grouped.push({ group: o.group, items });
            seenGroups.add(o.group);
        }
    }

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <button
                    type="button"
                    role="combobox"
                    aria-expanded={open}
                    aria-controls={listboxId}
                    aria-haspopup="listbox"
                    className={cn(
                        'flex min-h-[36px] w-full flex-wrap items-center gap-1 rounded-md border border-input bg-background px-2 py-1.5 text-sm shadow-sm transition-colors hover:bg-muted/30 focus:outline-none focus:ring-1 focus:ring-ring',
                    )}
                    aria-label={placeholder}
                >
                    {selectedOptions.length === 0 ? (
                        <span className="text-muted-foreground">{placeholder}</span>
                    ) : (
                        selectedOptions.map(o => (
                            <Badge key={o.id} variant="secondary" className="gap-1 pr-1">
                                {o.label}
                                <button
                                    type="button"
                                    aria-label={`Remove ${o.label}`}
                                    className="cursor-pointer rounded-full opacity-60 hover:opacity-100"
                                    onClick={e => {
                                        e.stopPropagation();
                                        toggle(o.id);
                                    }}
                                    onKeyDown={e => {
                                        if (e.key === 'Enter' || e.key === ' ') {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            toggle(o.id);
                                        }
                                    }}
                                >
                                    ×
                                </button>
                            </Badge>
                        ))
                    )}
                    <ChevronDown className="ml-auto size-3.5 shrink-0 text-muted-foreground" />
                </button>
            </PopoverTrigger>
            <PopoverContent
                className="w-[280px] p-0"
                align="start"
                id={listboxId}
                role="listbox"
                // PolicyEditor sheet is a PortalModal at z-index 10000; without
                // explicit z-index here Radix's portal lands behind the modal and
                // the dropdown is unclickable.
                style={{ zIndex: 10001 }}
            >
                <Command>
                    <CommandInput placeholder={`Search ${placeholder.toLowerCase()}…`} />
                    <CommandList>
                        {/*
                         * When the option list is empty (e.g. an empty schema
                         * with no actions, or no entities yet) we surface the
                         * caller-supplied hint instead of the generic
                         * "No results" copy — otherwise users can't tell
                         * whether the picker is broken or simply has no data.
                         */}
                        <CommandEmpty>
                            {options.length === 0 && emptyHint ? (
                                <span className="block px-2 py-1.5 text-sm text-muted-foreground">{emptyHint}</span>
                            ) : (
                                'No results.'
                            )}
                        </CommandEmpty>
                        {grouped.map(({ group, items }) => (
                            <CommandGroup key={group} heading={group}>
                                {items.map(item => (
                                    <CommandItem
                                        key={item.id}
                                        value={`${group} ${item.label}`}
                                        onSelect={() => toggle(item.id)}
                                        className="flex items-center gap-2"
                                    >
                                        <Check className={cn('size-3.5', selectedSet.has(item.id) ? 'opacity-100' : 'opacity-0')} />
                                        <div className="min-w-0 flex-1">
                                            <p className="truncate text-sm">{item.label}</p>
                                            {item.description ? (
                                                <p className="truncate text-muted-foreground" style={{ fontSize: '11px' }}>
                                                    {item.description}
                                                </p>
                                            ) : null}
                                        </div>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        ))}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}
