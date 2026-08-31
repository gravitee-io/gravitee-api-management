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
import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList, Input, Label } from '@gravitee/graphene-core';
import { useMemo } from 'react';

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
    const showSelect = shouldShowStringValueSelect(options, operator);
    const items = useMemo(() => {
        const list = options ?? [];
        if (pattern && !list.some(option => option.value === pattern)) {
            return [{ value: pattern, label: pattern }, ...list];
        }
        return list;
    }, [options, pattern]);
    const selected = items.find(option => option.value === pattern) ?? null;

    if (showSelect) {
        return (
            <div className="space-y-1.5">
                <Label htmlFor={id} className="text-xs">
                    Value
                </Label>
                <Combobox
                    items={items}
                    value={selected}
                    onValueChange={(option: AlertMetricValueOption | null) => onPatternChange(option?.value ?? '')}
                    itemToStringLabel={(option: AlertMetricValueOption) => option.label}
                >
                    <ComboboxInput id={id} placeholder="Select a value" />
                    <ComboboxContent>
                        <ComboboxEmpty>No matching values</ComboboxEmpty>
                        <ComboboxList>
                            {(option: AlertMetricValueOption) => (
                                <ComboboxItem key={option.value} value={option}>
                                    {option.label}
                                </ComboboxItem>
                            )}
                        </ComboboxList>
                    </ComboboxContent>
                </Combobox>
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
