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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import type { AlertMetricValueOption } from '../constants/alertConstants';
import { shouldShowStringValueSelect } from '../utils/alertMetricValues';

interface Props {
    id: string;
    operator: string | undefined;
    pattern: string | undefined;
    options: AlertMetricValueOption[] | undefined;
    onPatternChange: (pattern: string) => void;
    patternPlaceholder?: string;
}

export function StringValueField({ id, operator, pattern, options, onPatternChange, patternPlaceholder = 'e.g. API_KEY_MISSING' }: Props) {
    const [search, setSearch] = useState('');
    const showSelect = shouldShowStringValueSelect(options, operator);
    const items = useMemo(() => {
        const list = options ?? [];
        if (pattern && !list.some(option => option.value === pattern)) {
            return [{ value: pattern, label: pattern }, ...list];
        }
        return list;
    }, [options, pattern]);
    const filtered = useMemo(() => {
        const term = search.trim().toLowerCase();
        if (!term) {
            return items;
        }
        return items.filter(option => option.label.toLowerCase().includes(term) || option.value.toLowerCase().includes(term));
    }, [items, search]);

    if (showSelect) {
        return (
            <div className="space-y-1.5">
                <Label htmlFor={id} className="text-xs">
                    Value
                </Label>
                <Select value={pattern || undefined} onValueChange={onPatternChange}>
                    <SelectTrigger id={id}>
                        <SelectValue placeholder="Select a value" />
                    </SelectTrigger>
                    <SelectContent>
                        {items.length > 8 ? (
                            <div className="px-2 pb-2">
                                <Input
                                    value={search}
                                    placeholder="Search values"
                                    onChange={e => setSearch(e.target.value)}
                                    onKeyDown={e => e.stopPropagation()}
                                    onPointerDown={e => e.stopPropagation()}
                                />
                            </div>
                        ) : null}
                        {filtered.map(option => (
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
        <div className="space-y-1.5">
            <Label htmlFor={id} className="text-xs">
                Pattern
            </Label>
            <Input id={id} placeholder={patternPlaceholder} value={pattern ?? ''} onChange={e => onPatternChange(e.target.value)} />
        </div>
    );
}
