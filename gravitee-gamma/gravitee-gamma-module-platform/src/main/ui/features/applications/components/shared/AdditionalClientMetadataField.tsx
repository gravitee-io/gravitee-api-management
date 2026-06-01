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
import { Button, cn, Input, Label } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState } from 'react';

import {
    additionalClientMetadataRecordsEqual,
    getDuplicateMetadataKeys,
    hasDuplicateMetadataRowKeys,
    rowsToAdditionalClientMetadata,
} from '../../utils/applicationCreateMapper';

interface MetadataRow {
    id: string;
    key: string;
    value: string;
}

export interface AdditionalClientMetadataFieldProps {
    readonly value: Record<string, string> | null;
    readonly onChange: (value: Record<string, string> | null) => void;
    readonly onDuplicateKeysChange?: (hasDuplicates: boolean) => void;
    readonly error?: string;
    readonly disabled?: boolean;
}

function createEmptyRow(): MetadataRow {
    return { id: crypto.randomUUID(), key: '', value: '' };
}

function recordToRows(record: Record<string, string> | null): MetadataRow[] {
    if (!record || Object.keys(record).length === 0) {
        return [createEmptyRow()];
    }

    return [...Object.entries(record).map(([key, value]) => ({ id: crypto.randomUUID(), key, value })), createEmptyRow()];
}

function ensureTrailingEmptyRow(rows: MetadataRow[]): MetadataRow[] {
    if (rows.length === 0) {
        return [createEmptyRow()];
    }

    const last = rows[rows.length - 1]!;
    if (last.key.trim() || last.value.trim()) {
        return [...rows, createEmptyRow()];
    }

    return rows;
}

export function AdditionalClientMetadataField({
    value,
    onChange,
    onDuplicateKeysChange,
    error,
    disabled,
}: AdditionalClientMetadataFieldProps) {
    const [rows, setRows] = useState<MetadataRow[]>(() => recordToRows(value));
    const lastEmittedValueRef = useRef(value);

    useEffect(() => {
        if (additionalClientMetadataRecordsEqual(value, lastEmittedValueRef.current)) {
            return;
        }

        const syncedRows = recordToRows(value);
        lastEmittedValueRef.current = value;
        setRows(syncedRows);
        onDuplicateKeysChange?.(hasDuplicateMetadataRowKeys(syncedRows));
    }, [value, onDuplicateKeysChange]);

    const duplicateKeys = useMemo(() => getDuplicateMetadataKeys(rows), [rows]);
    const hasDuplicates = duplicateKeys.size > 0;
    const displayError = error ?? (hasDuplicates ? 'Keys must be unique' : null);

    const updateRows = (nextRows: MetadataRow[]) => {
        const withTrailing = ensureTrailingEmptyRow(nextRows);
        const hasDuplicateKeys = hasDuplicateMetadataRowKeys(withTrailing);

        setRows(withTrailing);
        onDuplicateKeysChange?.(hasDuplicateKeys);

        const nextValue = rowsToAdditionalClientMetadata(withTrailing);
        lastEmittedValueRef.current = nextValue;
        onChange(nextValue);
    };

    const handleRowChange = (rowId: string, field: 'key' | 'value', fieldValue: string) => {
        const nextRows = rows.map(row => (row.id === rowId ? { ...row, [field]: fieldValue } : row));
        updateRows(nextRows);
    };

    const handleDeleteRow = (rowId: string) => {
        const nextRows = rows.filter(row => row.id !== rowId);
        updateRows(nextRows);
    };

    return (
        <div className="space-y-2">
            <Label>Additional Client Metadata (optional)</Label>
            <div className="overflow-x-auto rounded-lg border">
                <table className="w-full min-w-[28rem] text-sm">
                    <thead>
                        <tr className="border-b bg-muted/40 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            <th className="px-3 py-2">Key</th>
                            <th className="px-3 py-2">Value</th>
                            <th className="w-10 px-2 py-2" aria-hidden />
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, index) => {
                            const isTrailingEmpty = index === rows.length - 1 && !row.key.trim() && !row.value.trim();
                            const showDelete = !isTrailingEmpty && rows.length > 1;
                            const trimmedKey = row.key.trim();
                            const isDuplicateRow = Boolean(trimmedKey && duplicateKeys.has(trimmedKey));
                            const duplicateInputClass = 'border-destructive focus-visible:ring-destructive/30';

                            return (
                                <tr key={row.id} className={cn('border-b last:border-b-0', isDuplicateRow && 'bg-destructive/5')}>
                                    <td className="px-3 py-2 align-top">
                                        <Input
                                            value={row.key}
                                            onChange={event => handleRowChange(row.id, 'key', event.target.value)}
                                            placeholder="Name..."
                                            aria-label={`Metadata key ${index + 1}`}
                                            aria-invalid={isDuplicateRow}
                                            disabled={disabled}
                                            className={isDuplicateRow ? duplicateInputClass : undefined}
                                        />
                                    </td>
                                    <td className="px-3 py-2 align-top">
                                        <Input
                                            value={row.value}
                                            onChange={event => handleRowChange(row.id, 'value', event.target.value)}
                                            placeholder="Value..."
                                            aria-label={`Metadata value ${index + 1}`}
                                            disabled={disabled}
                                            className={isDuplicateRow ? duplicateInputClass : undefined}
                                        />
                                    </td>
                                    <td className="px-2 py-2 align-top">
                                        {showDelete && (
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                className="size-8 shrink-0"
                                                aria-label={`Remove metadata row ${index + 1}`}
                                                onClick={() => handleDeleteRow(row.id)}
                                                disabled={disabled}
                                            >
                                                <XIcon className="size-4" aria-hidden />
                                            </Button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {displayError ? <p className="text-xs text-destructive">{displayError}</p> : null}
        </div>
    );
}
