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

import { Button, Field, FieldLabel, Input } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { notify } from '../../../shared/notify';

export interface DictionaryPropertyRow {
    id: string;
    key: string;
    value: string;
}

export function propertiesRowsToRecord(rows: DictionaryPropertyRow[]): Record<string, string> {
    const record: Record<string, string> = {};
    for (const row of rows) {
        const key = row.key.trim();
        if (key) {
            record[key] = row.value;
        }
    }
    return record;
}

export function hasValidProperties(rows: DictionaryPropertyRow[]): boolean {
    return rows.some(row => row.key.trim().length > 0);
}

export function DictionaryPropertiesEditor({
    properties,
    onChange,
    disabled = false,
}: Readonly<{
    properties: DictionaryPropertyRow[];
    onChange: (next: DictionaryPropertyRow[]) => void;
    disabled?: boolean;
}>) {
    const [draftKey, setDraftKey] = useState('');
    const [draftValue, setDraftValue] = useState('');

    function updateRow(id: string, patch: Partial<Pick<DictionaryPropertyRow, 'key' | 'value'>>) {
        onChange(properties.map(row => (row.id === id ? { ...row, ...patch } : row)));
    }

    function removeRow(id: string) {
        onChange(properties.filter(row => row.id !== id));
    }

    function addProperty() {
        if (!draftKey.trim()) {
            notify.error('Property key cannot be empty');
            return;
        }
        const key = draftKey.trim();
        if (properties.some(row => row.key.trim() === key)) {
            notify.error(`Property key "${key}" already exists`);
            return;
        }
        const newProperty = { id: crypto.randomUUID(), key, value: draftValue };
        const updatedProperties = [...properties, newProperty];
        onChange(updatedProperties);
        setDraftKey('');
        setDraftValue('');
    }

    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel>
                Properties{' '}
                <span className="text-destructive" aria-hidden>
                    *
                </span>
            </FieldLabel>
            <p className="text-xs text-muted-foreground">Key-value pairs available to policies via dictionary EL.</p>

            <div className="space-y-2 rounded-md border p-3">
                {properties.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No properties yet. Add at least one key-value pair.</p>
                ) : (
                    properties.map(row => (
                        <div key={row.id} className="flex items-start gap-2">
                            <Input
                                aria-label={`Property key ${row.key || 'new'}`}
                                value={row.key}
                                onChange={e => updateRow(row.id, { key: e.target.value })}
                                placeholder="Key"
                                disabled={disabled}
                                className="flex-1 font-mono text-sm"
                            />
                            <Input
                                aria-label={`Property value for ${row.key || 'new'}`}
                                value={row.value}
                                onChange={e => updateRow(row.id, { value: e.target.value })}
                                placeholder="Value"
                                disabled={disabled}
                                className="flex-1"
                            />
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                aria-label={`Remove property ${row.key || ''}`.trim()}
                                onClick={() => removeRow(row.id)}
                                disabled={disabled}
                            >
                                <Trash2Icon className="size-4" aria-hidden />
                            </Button>
                        </div>
                    ))
                )}

                <div className="flex items-start gap-2 border-t pt-3">
                    <Input
                        aria-label="New property key"
                        value={draftKey}
                        onChange={e => setDraftKey(e.target.value)}
                        placeholder="New key"
                        disabled={disabled}
                        className="flex-1 font-mono text-sm"
                        onKeyDown={e => {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                                addProperty();
                            }
                        }}
                    />
                    <Input
                        aria-label="New property value"
                        value={draftValue}
                        onChange={e => setDraftValue(e.target.value)}
                        placeholder="New value"
                        disabled={disabled}
                        className="flex-1"
                        onKeyDown={e => {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                                addProperty();
                            }
                        }}
                    />
                    <Button type="button" variant="outline" onClick={addProperty} disabled={disabled} aria-label="Add property">
                        <PlusIcon className="size-4" aria-hidden />
                        Add
                    </Button>
                </div>
            </div>
        </Field>
    );
}
