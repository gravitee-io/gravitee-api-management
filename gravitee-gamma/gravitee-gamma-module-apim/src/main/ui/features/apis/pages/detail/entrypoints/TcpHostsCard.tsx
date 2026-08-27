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
import { Button, Card, CardContent, Input, Label, cn } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { InfoTooltip } from './InfoTooltip';
import type { TcpHostEntry } from '../../../types/apiCreation';
import { validateDuplicateHost } from '../../../utils/duplicateDialogValidation';

function findRowError(rows: TcpHostEntry[], row: TcpHostEntry): string | null {
    const formatError = validateDuplicateHost(row.host);
    if (formatError) return formatError;
    const isDuplicate = rows.filter(r => r.host.trim() === row.host.trim()).length > 1;
    return isDuplicate ? 'Duplicated hosts not allowed' : null;
}

interface TcpHostsCardProps {
    rows: TcpHostEntry[];
    onAdd: () => void;
    onDelete: (id: string) => void;
    onHostChange: (id: string, host: string) => void;
    isReadOnly: boolean;
}

export function TcpHostsCard({ rows, onAdd, onDelete, onHostChange, isReadOnly }: Readonly<TcpHostsCardProps>) {
    const canDelete = rows.length > 1;

    return (
        <Card>
            <CardContent className="p-5 space-y-4">
                <div>
                    <div className="text-sm font-semibold text-foreground flex items-center">
                        Entrypoint hosts
                        <InfoTooltip text="Hostnames clients use to reach this TCP API on the gateway's TCP port. Matched against SNI." />
                    </div>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                        Each row is a hostname the gateway matches for this API. Add or remove hosts as needed.
                    </p>
                </div>

                <div>
                    <Label className="text-xs font-medium text-muted-foreground">Host</Label>
                </div>

                <div className="space-y-2">
                    {rows.map(row => {
                        const error = findRowError(rows, row);
                        return (
                            <div key={row.id} className="flex items-start gap-2">
                                <div className="flex-1 space-y-1">
                                    <Input
                                        value={row.host}
                                        onChange={e => onHostChange(row.id, e.target.value)}
                                        placeholder="db.example.com"
                                        disabled={isReadOnly}
                                        aria-invalid={error !== null}
                                        aria-label="Host"
                                    />
                                    {error && <p className="text-xs text-destructive">{error}</p>}
                                </div>
                                {!isReadOnly && (
                                    <button
                                        type="button"
                                        onClick={() => onDelete(row.id)}
                                        disabled={!canDelete}
                                        aria-label="Delete host"
                                        className={cn(
                                            'mt-1 p-1.5 rounded-md transition-colors',
                                            canDelete
                                                ? 'text-muted-foreground hover:text-destructive hover:bg-destructive/10'
                                                : 'text-muted-foreground/30 cursor-not-allowed',
                                        )}
                                    >
                                        <Trash2Icon className="size-4" />
                                    </button>
                                )}
                            </div>
                        );
                    })}
                </div>

                {!isReadOnly && (
                    <Button variant="outline" size="sm" onClick={onAdd} className="gap-1.5">
                        <PlusIcon className="size-3.5" />
                        Add host
                    </Button>
                )}
            </CardContent>
        </Card>
    );
}
