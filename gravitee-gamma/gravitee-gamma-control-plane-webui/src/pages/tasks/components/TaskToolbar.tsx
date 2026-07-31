/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';
import type { CSSProperties } from 'react';

import type { TaskSortOrder } from '../tasks.mapping';
import type { TaskCategory } from '../tasks.types';

export type TaskFilterValue = TaskCategory | 'all';

const CATEGORY_ORDER: readonly { key: TaskCategory; label: string }[] = [
    { key: 'SUBSCRIPTION', label: 'Subscription' },
    { key: 'API_REVIEW', label: 'API Review' },
    { key: 'CHANGES_REQUESTED', label: 'Changes' },
    { key: 'USER_REGISTRATION', label: 'Registration' },
    { key: 'API_PROMOTION', label: 'Promotion' },
];

const SORT_OPTIONS: readonly { value: TaskSortOrder; label: string }[] = [
    { value: 'newest', label: 'Newest first' },
    { value: 'oldest', label: 'Oldest first' },
];

const CHIP_BASE = 'h-7 rounded-full border px-3 text-xs font-medium transition-colors';
const CHIP_INACTIVE = `${CHIP_BASE} border-border text-muted-foreground hover:bg-muted hover:text-foreground`;

const ACTIVE_CHIP_STYLE: CSSProperties = {
    borderColor: 'var(--primary)',
    color: 'var(--primary)',
    backgroundColor: 'color-mix(in oklab, var(--primary) 10%, transparent)',
};

interface TaskToolbarProps {
    readonly counts: Record<TaskCategory, number>;
    readonly total: number;
    readonly filter: TaskFilterValue;
    readonly onFilterChange: (value: TaskFilterValue) => void;
    readonly sort: TaskSortOrder;
    readonly onSortChange: (value: TaskSortOrder) => void;
    readonly compact?: boolean;
}

export function TaskToolbar({ counts, total, filter, onFilterChange, sort, onSortChange, compact = false }: TaskToolbarProps) {
    const filterOptions: { key: TaskFilterValue; label: string; count: number }[] = [
        { key: 'all', label: 'All', count: total },
        ...CATEGORY_ORDER.map(category => ({ key: category.key, label: category.label, count: counts[category.key] })),
    ];

    if (compact) {
        return (
            <div className="flex flex-wrap items-center gap-3">
                <Select value={filter} onValueChange={value => onFilterChange(value as TaskFilterValue)}>
                    <SelectTrigger className="h-8 w-44" aria-label="Filter tasks by category">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {filterOptions.map(option => (
                            <SelectItem key={option.key} value={option.key}>
                                {option.label} ({option.count})
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <Select value={sort} onValueChange={value => onSortChange(value as TaskSortOrder)}>
                    <SelectTrigger className="h-8 w-36" aria-label="Sort tasks">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {SORT_OPTIONS.map(option => (
                            <SelectItem key={option.value} value={option.value}>
                                {option.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>
        );
    }

    return (
        <div className="flex flex-wrap items-center justify-between gap-3">
            <ToggleGroup
                type="single"
                value={filter}
                onValueChange={value => value && onFilterChange(value as TaskFilterValue)}
                spacing={2}
                aria-label="Filter tasks by category"
            >
                {filterOptions.map(option => {
                    const active = option.key === filter;
                    return (
                        <ToggleGroupItem
                            key={option.key}
                            value={option.key}
                            className={active ? CHIP_BASE : CHIP_INACTIVE}
                            style={active ? ACTIVE_CHIP_STYLE : undefined}
                        >
                            {option.label} ({option.count})
                        </ToggleGroupItem>
                    );
                })}
            </ToggleGroup>
            <ToggleGroup
                type="single"
                value={sort}
                onValueChange={value => value && onSortChange(value as TaskSortOrder)}
                spacing={2}
                aria-label="Sort tasks"
            >
                {SORT_OPTIONS.map(option => {
                    const active = option.value === sort;
                    return (
                        <ToggleGroupItem
                            key={option.value}
                            value={option.value}
                            className={active ? CHIP_BASE : CHIP_INACTIVE}
                            style={active ? ACTIVE_CHIP_STYLE : undefined}
                        >
                            {option.label}
                        </ToggleGroupItem>
                    );
                })}
            </ToggleGroup>
        </div>
    );
}
