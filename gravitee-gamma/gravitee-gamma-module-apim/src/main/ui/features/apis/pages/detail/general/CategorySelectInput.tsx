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
import { Badge, Checkbox, Popover, PopoverContent, PopoverTrigger, Skeleton } from '@gravitee/graphene-core';
import { ChevronDownIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import type { EnvCategory } from '../../../types';

interface CategorySelectInputProps {
    id?: string;
    selectedKeys: string[];
    categories: EnvCategory[];
    isLoading?: boolean;
    disabled?: boolean;
    onChange: (keys: string[]) => void;
}

export function CategorySelectInput({ id, selectedKeys, categories, isLoading, disabled, onChange }: CategorySelectInputProps) {
    const [open, setOpen] = useState(false);
    const [filter, setFilter] = useState('');
    const [dropdownWidth, setDropdownWidth] = useState<number | undefined>();
    const triggerRef = useRef<HTMLButtonElement>(null);

    const handleOpenChange = (v: boolean) => {
        if (disabled) return;
        if (v && triggerRef.current) {
            setDropdownWidth(triggerRef.current.offsetWidth);
        }
        setOpen(v);
    };

    const selectedSet = new Set(selectedKeys);

    const visible = categories.filter(c => {
        if (!filter.trim()) return true;
        const q = filter.toLowerCase();
        return c.name.toLowerCase().includes(q) || (c.description ?? '').toLowerCase().includes(q);
    });

    const toggle = (key: string) => {
        const next = new Set(selectedSet);
        if (next.has(key)) next.delete(key);
        else next.add(key);
        onChange([...next]);
    };

    const removeKey = (key: string) => {
        onChange(selectedKeys.filter(k => k !== key));
    };

    const selectedCategories = categories.filter(c => selectedSet.has(c.key));

    if (isLoading) {
        return <Skeleton className="h-9 w-full rounded-md" />;
    }

    return (
        <Popover open={open && !disabled} onOpenChange={handleOpenChange}>
            <PopoverTrigger asChild>
                <button
                    ref={triggerRef}
                    id={id}
                    type="button"
                    disabled={disabled}
                    className="flex min-h-9 w-full flex-wrap items-center gap-1.5 rounded-md border bg-muted/30 px-2 py-1.5 text-left text-sm disabled:cursor-not-allowed disabled:opacity-50"
                    aria-haspopup="listbox"
                    aria-expanded={open}
                >
                    {selectedCategories.length === 0 ? (
                        <span className="text-muted-foreground text-sm flex-1">{disabled ? '' : 'Select categories…'}</span>
                    ) : (
                        selectedCategories.map(c => (
                            <Badge key={c.key} variant="secondary" style={{ fontSize: '11px', gap: '2px' }}>
                                {c.name}
                                {!disabled && (
                                    <button
                                        type="button"
                                        onClick={e => {
                                            e.stopPropagation();
                                            removeKey(c.key);
                                        }}
                                        className="hover:text-destructive ml-0.5"
                                        aria-label={`Remove ${c.name}`}
                                    >
                                        <XIcon className="size-3" />
                                    </button>
                                )}
                            </Badge>
                        ))
                    )}
                    {!disabled && <ChevronDownIcon className="ml-auto size-4 shrink-0 text-muted-foreground" aria-hidden />}
                </button>
            </PopoverTrigger>
            <PopoverContent className="p-0" style={{ width: dropdownWidth }} align="start">
                <div className="p-2 border-b">
                    <input
                        className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                        placeholder="Filter categories…"
                        value={filter}
                        onChange={e => setFilter(e.target.value)}
                    />
                </div>
                <div className="max-h-60 overflow-y-auto">
                    {visible.length === 0 ? (
                        <p className="px-3 py-4 text-center text-xs text-muted-foreground">No categories available</p>
                    ) : (
                        visible.map(c => (
                            <label key={c.key} className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm hover:bg-muted">
                                <Checkbox checked={selectedSet.has(c.key)} onCheckedChange={() => toggle(c.key)} aria-label={c.name} />
                                <span className="flex-1 min-w-0">
                                    <span>{c.name}</span>
                                    {c.description && <em className="ml-1 text-muted-foreground text-xs">- {c.description}</em>}
                                </span>
                            </label>
                        ))
                    )}
                </div>
            </PopoverContent>
        </Popover>
    );
}
