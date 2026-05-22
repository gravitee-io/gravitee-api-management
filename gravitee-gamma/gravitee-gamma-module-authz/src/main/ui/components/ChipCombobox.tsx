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
    Badge,
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    Popover,
    PopoverContent,
    PopoverTrigger,
    cn,
} from '@gravitee/graphene-core';
import { CheckIcon, ChevronDownIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useId, useMemo, useState } from 'react';

export const GAPL_UID_PATTERN = /^([^:]+)::"(.+)"$/;

export interface ChipComboboxOption {
    readonly id: string;
    readonly label: string;
    readonly group: string;
    readonly description?: string;
}

export interface ChipComboboxProps {
    readonly placeholder: string;
    readonly options: readonly ChipComboboxOption[];
    readonly selectedIds: readonly string[];
    readonly onChange: (ids: string[]) => void;
    readonly groupOrder?: readonly string[];
    readonly emptyHint?: string;
    readonly ghostLabelPattern?: RegExp;
}

export function ChipCombobox({
    placeholder,
    options,
    selectedIds,
    onChange,
    groupOrder,
    emptyHint,
    ghostLabelPattern = GAPL_UID_PATTERN,
}: ChipComboboxProps) {
    const [open, setOpen] = useState(false);
    const listboxId = useId();

    const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

    const toggle = (id: string) => {
        if (selectedSet.has(id)) {
            onChange(selectedIds.filter(x => x !== id));
        } else {
            onChange([...selectedIds, id]);
        }
    };

    const selectedChips = useMemo(
        () =>
            selectedIds.map(id => {
                const opt = options.find(o => o.id === id);
                if (opt) return { id: opt.id, label: opt.label, ghost: false };
                const m = id.match(ghostLabelPattern);
                return { id, label: m ? m[2]! : id, ghost: true };
            }),
        [selectedIds, options, ghostLabelPattern],
    );

    const grouped = useMemo(() => {
        const out: Array<{ group: string; items: ChipComboboxOption[] }> = [];
        const seen = new Set<string>();
        const ordered = groupOrder ?? [...new Set(options.map(o => o.group))];
        for (const group of ordered) {
            const items = options.filter(o => o.group === group);
            if (items.length > 0) {
                out.push({ group, items });
                seen.add(group);
            }
        }
        for (const o of options) {
            if (!seen.has(o.group)) {
                out.push({ group: o.group, items: options.filter(x => x.group === o.group) });
                seen.add(o.group);
            }
        }
        return out;
    }, [options, groupOrder]);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <button
                    type="button"
                    role="combobox"
                    aria-expanded={open}
                    aria-controls={listboxId}
                    aria-haspopup="listbox"
                    className="flex min-h-9 w-full flex-wrap items-center gap-1 rounded-md border border-input bg-background px-2 py-1.5 text-sm shadow-sm transition-colors hover:bg-muted/30 focus:outline-none focus:ring-1 focus:ring-ring"
                    aria-label={placeholder}
                >
                    {selectedChips.length === 0 ? (
                        <span className="text-muted-foreground">{placeholder}</span>
                    ) : (
                        selectedChips.map(c => (
                            <Badge
                                key={c.id}
                                variant={c.ghost ? 'outline' : 'secondary'}
                                title={c.ghost ? `${c.id} — not in catalog` : undefined}
                                className={cn('gap-1 pr-1', c.ghost && 'border-dashed border-warning bg-warning/10 text-warning')}
                            >
                                {c.ghost ? <TriangleAlertIcon className="size-3" aria-hidden /> : null}
                                {c.label}
                                <span
                                    role="button"
                                    tabIndex={0}
                                    aria-label={`Remove ${c.label}`}
                                    className="cursor-pointer rounded-full opacity-60 hover:opacity-100"
                                    onClick={e => {
                                        e.stopPropagation();
                                        toggle(c.id);
                                    }}
                                    onKeyDown={e => {
                                        if (e.key === 'Enter' || e.key === ' ') {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            toggle(c.id);
                                        }
                                    }}
                                >
                                    ×
                                </span>
                            </Badge>
                        ))
                    )}
                    <ChevronDownIcon className="ml-auto size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                </button>
            </PopoverTrigger>
            <PopoverContent
                className="w-[var(--radix-popper-anchor-width)] min-w-60 p-0"
                align="start"
                id={listboxId}
                role="listbox"
                style={{ zIndex: 60 }}
            >
                <Command>
                    <CommandInput placeholder={`Search ${placeholder.toLowerCase()}…`} />
                    <CommandList
                        className="max-h-64 [&]:!overflow-y-auto"
                        onWheel={e => {
                            e.currentTarget.scrollTop += e.deltaY;
                            e.stopPropagation();
                        }}
                    >
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
                                        <CheckIcon
                                            className={cn('size-3.5', selectedSet.has(item.id) ? 'opacity-100' : 'opacity-0')}
                                            aria-hidden
                                        />
                                        <div className="min-w-0 flex-1">
                                            <p className="truncate text-sm">{item.label}</p>
                                            {item.description ? (
                                                <p className="truncate text-xs text-muted-foreground">{item.description}</p>
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
