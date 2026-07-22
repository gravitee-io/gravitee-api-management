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

import { Field, FieldLabel, Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { DictionaryTriggerUnit } from '../types/dictionary';

export const TRIGGER_UNIT_OPTIONS: Array<{ value: DictionaryTriggerUnit; label: string }> = [
    { value: 'SECONDS', label: 'Seconds' },
    { value: 'MINUTES', label: 'Minutes' },
    { value: 'HOURS', label: 'Hours' },
];

export interface DictionaryTriggerFormValue {
    rate: string;
    unit: DictionaryTriggerUnit;
}

export function DictionaryTriggerFields({
    value,
    onChange,
    disabled,
}: Readonly<{
    value: DictionaryTriggerFormValue;
    onChange: (next: DictionaryTriggerFormValue) => void;
    disabled?: boolean;
}>) {
    const rateNumber = Number(value.rate);
    const rateInvalid = value.rate.trim() !== '' && (!Number.isFinite(rateNumber) || rateNumber <= 0);

    return (
        <section className="space-y-4 rounded-lg border p-4">
            <div className="space-y-1">
                <h3 className="text-sm font-semibold">Trigger</h3>
                <p className="text-xs text-muted-foreground">How often the provider is polled to refresh properties.</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel htmlFor="dictionary-trigger-rate">
                        Interval{' '}
                        <span className="text-destructive" aria-hidden>
                            *
                        </span>
                    </FieldLabel>
                    <Input
                        id="dictionary-trigger-rate"
                        type="number"
                        min={1}
                        step={1}
                        value={value.rate}
                        onChange={e => onChange({ ...value, rate: e.target.value })}
                        disabled={disabled}
                        required
                        aria-invalid={rateInvalid}
                        aria-describedby={rateInvalid ? 'dictionary-trigger-rate-error' : undefined}
                    />
                    {rateInvalid ? (
                        <p id="dictionary-trigger-rate-error" className="text-sm text-destructive" role="alert">
                            Interval must be greater than 0
                        </p>
                    ) : null}
                </Field>

                <Field orientation="vertical" className="gap-1.5">
                    <FieldLabel htmlFor="dictionary-trigger-unit">
                        Time Unit{' '}
                        <span className="text-destructive" aria-hidden>
                            *
                        </span>
                    </FieldLabel>
                    <Select
                        value={value.unit}
                        onValueChange={unit => onChange({ ...value, unit: unit as DictionaryTriggerUnit })}
                        disabled={disabled}
                    >
                        <SelectTrigger id="dictionary-trigger-unit">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {TRIGGER_UNIT_OPTIONS.map(option => (
                                <SelectItem key={option.value} value={option.value}>
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </Field>
            </div>
        </section>
    );
}

export function isTriggerFormValid(value: DictionaryTriggerFormValue): boolean {
    const rateNumber = Number(value.rate);
    return value.rate.trim() !== '' && Number.isFinite(rateNumber) && rateNumber > 0;
}
