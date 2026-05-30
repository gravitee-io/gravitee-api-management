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
    Button,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    cn,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';
import { ATTR_TYPES, ATTR_TYPE_LABELS, coerce, policyFnHint, validateKey, type AttrType } from '../../shared/attribute-codec';

export interface AttributeRow {
    readonly id: string;
    key: string;
    type: AttrType;
    /** Raw editor input: string for scalar types, string[] for sets. */
    raw: string | string[];
}

export interface AttributeEditorProps {
    readonly value: AttributeRow[];
    readonly onChange: (rows: AttributeRow[]) => void;
    readonly readOnly?: boolean;
    readonly keySuggestions: readonly string[];
}

let rowSeq = 0;
export function newAttributeRow(): AttributeRow {
    rowSeq += 1;
    return { id: `attr-${rowSeq}-${Date.now()}`, key: '', type: 'string', raw: '' };
}

export function AttributeEditor({ value, onChange, readOnly = false, keySuggestions }: AttributeEditorProps) {
    const listId = useId();

    function patch(id: string, next: Partial<AttributeRow>) {
        onChange(value.map(r => (r.id === id ? { ...r, ...next } : r)));
    }
    function remove(id: string) {
        onChange(value.filter(r => r.id !== id));
    }

    if (readOnly) {
        return (
            <div className="flex flex-col gap-2">
                {value.length === 0 && <p className="text-xs text-muted-foreground">No attributes.</p>}
                {value.map(r => (
                    <div key={r.id} className="flex items-center gap-2 text-sm">
                        <span className="font-mono">{r.key}</span>
                        <Badge variant="outline" className="font-mono text-xs">
                            {ATTR_TYPE_LABELS[r.type]}
                        </Badge>
                        <span className="truncate text-muted-foreground">{Array.isArray(r.raw) ? r.raw.join(', ') : r.raw}</span>
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-3">
            <datalist id={listId}>
                {keySuggestions.map(k => (
                    <option key={k} value={k} />
                ))}
            </datalist>
            {value.map(r => {
                const keyError = validateKey(
                    r.key,
                    value.filter(o => o.id !== r.id).map(o => o.key),
                );
                const coerced = coerce(r.type, r.raw);
                const valueError = !coerced.ok ? coerced.error : null;
                const warning = coerced.ok ? coerced.warning : undefined;
                const hint = policyFnHint(r.type);
                return (
                    <div key={r.id} className="flex flex-col gap-1 rounded-md border p-2">
                        <div className="flex flex-wrap items-start gap-2">
                            <Input
                                value={r.key}
                                onChange={e => patch(r.id, { key: e.target.value })}
                                placeholder="key"
                                aria-label="Attribute key"
                                list={listId}
                                aria-invalid={keyError !== null}
                                className="w-40 font-mono"
                            />
                            <Select value={r.type} onValueChange={t => patch(r.id, { type: t as AttrType })}>
                                <SelectTrigger aria-label="Attribute type" className="w-36">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {ATTR_TYPES.map(t => (
                                        <SelectItem key={t} value={t}>
                                            {ATTR_TYPE_LABELS[t]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <AttributeValueInput row={r} onRawChange={raw => patch(r.id, { raw })} invalid={valueError !== null} />
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => remove(r.id)}
                                aria-label={`Remove ${r.key || 'attribute'}`}
                                title="Remove"
                            >
                                <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                            </Button>
                        </div>
                        {keyError && <p className="text-xs text-destructive">{keyError}</p>}
                        {valueError && <p className="text-xs text-destructive">{valueError}</p>}
                        {warning && <p className="text-xs text-warning">{warning}</p>}
                        {hint && !valueError && (
                            <p className="text-xs text-muted-foreground">
                                Stored as text; reference with <span className="font-mono">{hint}</span> in policies.
                            </p>
                        )}
                    </div>
                );
            })}
            <div>
                <Button type="button" variant="outline" size="sm" onClick={() => onChange([...value, newAttributeRow()])}>
                    <PlusIcon className="mr-2 size-4" aria-hidden />
                    Add attribute
                </Button>
            </div>
        </div>
    );
}

function AttributeValueInput({
    row,
    onRawChange,
    invalid,
}: {
    row: AttributeRow;
    onRawChange: (raw: string | string[]) => void;
    invalid: boolean;
}) {
    if (row.type === 'boolean') {
        const checked = row.raw === 'true' || row.raw === true.toString();
        return (
            <div className="flex h-9 items-center gap-2">
                <Switch checked={checked} onCheckedChange={c => onRawChange(c ? 'true' : 'false')} aria-label="Attribute value" />
                <span className="text-sm text-muted-foreground">{checked ? 'true' : 'false'}</span>
            </div>
        );
    }
    if (row.type === 'set') {
        const text = Array.isArray(row.raw) ? row.raw.join(', ') : row.raw;
        return (
            <Input
                value={text}
                onChange={e => onRawChange(e.target.value.split(',').map(s => s.trim()))}
                placeholder="comma,separated"
                aria-label="Attribute value"
                aria-invalid={invalid}
                className={cn('min-w-40 flex-1 font-mono')}
            />
        );
    }
    return (
        <Input
            value={Array.isArray(row.raw) ? row.raw.join(',') : row.raw}
            onChange={e => onRawChange(e.target.value)}
            placeholder="value"
            aria-label="Attribute value"
            aria-invalid={invalid}
            className="min-w-40 flex-1 font-mono"
        />
    );
}
