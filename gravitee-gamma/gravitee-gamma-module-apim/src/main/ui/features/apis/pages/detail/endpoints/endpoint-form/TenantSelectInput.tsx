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
import { Badge, Checkbox, Input, Popover, PopoverContent, PopoverTrigger, Skeleton } from '@gravitee/graphene-core';
import { ChevronDownIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import type { Tenant } from '../../../../types';

interface TenantSelectInputProps {
    selectedKeys: string[];
    tenants: Tenant[];
    isLoading?: boolean;
    disabled?: boolean;
    onChange: (keys: string[]) => void;
}

export function TenantSelectInput({ selectedKeys, tenants, isLoading, disabled, onChange }: Readonly<TenantSelectInputProps>) {
    const [open, setOpen] = useState(false);
    const [filter, setFilter] = useState('');
    const [dropdownWidth, setDropdownWidth] = useState<number | undefined>();
    const triggerRef = useRef<HTMLButtonElement>(null);

    const selectedSet = new Set(selectedKeys);

    const handleOpenChange = (v: boolean) => {
        if (disabled) return;
        if (v && triggerRef.current) setDropdownWidth(triggerRef.current.offsetWidth);
        setOpen(v);
    };

    const toggle = (key: string) => {
        const next = new Set(selectedSet);
        if (next.has(key)) next.delete(key);
        else next.add(key);
        onChange([...next]);
    };

    const removeKey = (key: string) => onChange(selectedKeys.filter(k => k !== key));

    const visible = tenants.filter(t => {
        if (!filter.trim()) return true;
        const q = filter.toLowerCase();
        return t.name.toLowerCase().includes(q) || (t.description ?? '').toLowerCase().includes(q);
    });

    const selectedTenants = tenants.filter(t => selectedSet.has(t.key));

    if (isLoading) return <Skeleton className="h-9 w-full rounded-md" />;

    return (
        <Popover open={open && !disabled} onOpenChange={handleOpenChange}>
            <PopoverTrigger asChild>
                <button
                    ref={triggerRef}
                    type="button"
                    disabled={disabled}
                    className="flex min-h-9 w-full flex-wrap items-center gap-1.5 rounded-md border bg-muted/30 px-2 py-1.5 text-left text-sm disabled:cursor-not-allowed disabled:opacity-50"
                    aria-haspopup="listbox"
                    aria-expanded={open}
                >
                    {selectedTenants.length === 0 ? (
                        <span className="text-muted-foreground text-sm flex-1">{disabled ? '—' : 'Select tenants…'}</span>
                    ) : (
                        selectedTenants.map(t => (
                            <Badge key={t.key} variant="secondary" style={{ fontSize: '11px', gap: '2px' }}>
                                {t.name}
                                {!disabled && (
                                    <button
                                        type="button"
                                        onClick={e => {
                                            e.stopPropagation();
                                            removeKey(t.key);
                                        }}
                                        className="hover:text-destructive ml-0.5"
                                        aria-label={`Remove ${t.name}`}
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
                    <Input
                        className="border-0 shadow-none focus-visible:ring-0 h-auto px-0 text-sm"
                        placeholder="Filter tenants…"
                        value={filter}
                        onChange={e => setFilter(e.target.value)}
                    />
                </div>
                <div className="max-h-60 overflow-y-auto">
                    {visible.length === 0 ? (
                        <p className="px-3 py-4 text-center text-xs text-muted-foreground">No tenants available</p>
                    ) : (
                        visible.map(t => (
                            <label key={t.key} className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm hover:bg-muted">
                                <Checkbox checked={selectedSet.has(t.key)} onCheckedChange={() => toggle(t.key)} aria-label={t.name} />
                                <span className="flex-1 min-w-0">
                                    <span>{t.name}</span>
                                    {t.description && <em className="ml-1 text-muted-foreground text-xs">— {t.description}</em>}
                                </span>
                            </label>
                        ))
                    )}
                </div>
            </PopoverContent>
        </Popover>
    );
}
