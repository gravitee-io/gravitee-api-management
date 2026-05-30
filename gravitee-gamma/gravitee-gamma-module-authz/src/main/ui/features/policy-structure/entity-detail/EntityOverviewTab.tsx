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
import { Badge, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';
import { inferAttrType } from '../../../shared/entity-gapl-shape';
import type { EntityInstance } from '../../../shared/entity.types';

function sourceLabel(source: EntityInstance['source']): string {
    if (source === 'apim') return 'APIM';
    if (source === 'gravitee-catalog') return 'Gravitee Catalog';
    return 'Local';
}

function renderValue(value: unknown): string {
    if (Array.isArray(value)) return value.join(', ');
    return String(value);
}

interface ProvenanceRow {
    readonly label: string;
    readonly value: string;
}

function provenanceRows(entity: EntityInstance): ProvenanceRow[] {
    const rows: ProvenanceRow[] = [{ label: 'Source', value: sourceLabel(entity.source) }];
    if (entity.importedAt) rows.push({ label: 'Imported at', value: entity.importedAt });
    if (entity.createdAt) rows.push({ label: 'Created', value: entity.createdAt });
    if (entity.updatedAt) rows.push({ label: 'Updated', value: entity.updatedAt });
    return rows;
}

export function EntityOverviewTab({ entity }: { entity: EntityInstance }) {
    const attrs = Object.entries(entity.attrs);
    return (
        <div className="flex flex-col gap-5">
            <section className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Attributes</h3>
                {attrs.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No attributes.</p>
                ) : (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Name</TableHead>
                                <TableHead>Type</TableHead>
                                <TableHead>Value</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {attrs.map(([key, value]) => (
                                <TableRow key={key}>
                                    <TableCell className="font-mono">{key}</TableCell>
                                    <TableCell>
                                        <Badge variant="outline" className="font-mono text-xs">
                                            {inferAttrType(value)}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="font-mono text-muted-foreground">{renderValue(value)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                )}
            </section>
            <section className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Provenance</h3>
                <Table>
                    <TableBody>
                        {provenanceRows(entity).map(row => (
                            <TableRow key={row.label}>
                                <TableCell className="w-40 text-muted-foreground">{row.label}</TableCell>
                                <TableCell className="font-mono">{row.value}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </section>
        </div>
    );
}
