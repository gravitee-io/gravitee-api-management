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
import { Button, Input, Label } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { rowsToAdditionalClientMetadata } from '../../utils/applicationCreateMapper';

interface MetadataRow {
    key: string;
    value: string;
}

interface AdditionalClientMetadataFieldProps {
    readonly value: Record<string, string> | null;
    readonly onChange: (value: Record<string, string> | null) => void;
}

function createEmptyRow(): MetadataRow {
    return { key: '', value: '' };
}

function recordToRows(record: Record<string, string> | null): MetadataRow[] {
    if (!record || Object.keys(record).length === 0) {
        return [createEmptyRow()];
    }

    return [...Object.entries(record).map(([key, value]) => ({ key, value })), createEmptyRow()];
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

export function AdditionalClientMetadataField({ value, onChange }: AdditionalClientMetadataFieldProps) {
    const [rows, setRows] = useState<MetadataRow[]>(() => recordToRows(value));

    const updateRows = (nextRows: MetadataRow[]) => {
        const withTrailing = ensureTrailingEmptyRow(nextRows);
        setRows(withTrailing);
        onChange(rowsToAdditionalClientMetadata(withTrailing));
    };

    const handleRowChange = (index: number, field: 'key' | 'value', fieldValue: string) => {
        const nextRows = rows.map((row, rowIndex) => (rowIndex === index ? { ...row, [field]: fieldValue } : row));
        updateRows(nextRows);
    };

    const handleDeleteRow = (index: number) => {
        const nextRows = rows.filter((_, rowIndex) => rowIndex !== index);
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

                            return (
                                <tr key={index} className="border-b last:border-b-0">
                                    <td className="px-3 py-2 align-top">
                                        <Input
                                            value={row.key}
                                            onChange={event => handleRowChange(index, 'key', event.target.value)}
                                            placeholder="Name..."
                                            aria-label={`Metadata key ${index + 1}`}
                                        />
                                    </td>
                                    <td className="px-3 py-2 align-top">
                                        <Input
                                            value={row.value}
                                            onChange={event => handleRowChange(index, 'value', event.target.value)}
                                            placeholder="Value..."
                                            aria-label={`Metadata value ${index + 1}`}
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
                                                onClick={() => handleDeleteRow(index)}
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
        </div>
    );
}
