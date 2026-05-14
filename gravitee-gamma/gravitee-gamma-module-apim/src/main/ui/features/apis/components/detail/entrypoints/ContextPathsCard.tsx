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
import { Button, Card, CardContent, Input, Label, Switch, cn } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { InfoTooltip } from './InfoTooltip';
import type { ContextPathRow } from './types';
import { validatePath } from './types';

function hasDuplicates(rows: ContextPathRow[]): boolean {
    const paths = rows.map(r => r.path);
    return new Set(paths).size !== paths.length;
}

interface ContextPathsCardProps {
    rows: ContextPathRow[];
    onAdd: () => void;
    onDelete: (id: string) => void;
    onPathChange: (id: string, path: string) => void;
    onToggleVirtualHosts: () => void;
    isReadOnly: boolean;
}

export function ContextPathsCard({
    rows,
    onAdd,
    onDelete,
    onPathChange,
    onToggleVirtualHosts,
    isReadOnly,
}: Readonly<ContextPathsCardProps>) {
    const hasDuplicatePaths = hasDuplicates(rows);
    const canDelete = rows.length > 1;

    return (
        <Card>
            <CardContent className="p-5 space-y-4">
                {/* Header row */}
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-foreground flex items-center">
                            Entrypoint context-paths
                            <InfoTooltip text="Each row defines a context path that routes requests to this API through the gateway." />
                        </p>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                            Each row is served under the shared gateway host. Add or remove context paths as needed.
                        </p>
                    </div>
                    {!isReadOnly && (
                        <div className="flex items-center gap-2 shrink-0">
                            <span className="text-xs text-muted-foreground whitespace-nowrap">Enable virtual hosts</span>
                            <Switch checked={false} onCheckedChange={onToggleVirtualHosts} aria-label="Enable virtual hosts" />
                        </div>
                    )}
                </div>

                {/* Column header */}
                <div>
                    <Label className="text-xs font-medium text-muted-foreground">Context path</Label>
                    <p className="text-xs text-muted-foreground mt-0.5">Must start with /. Requests are routed under the gateway host.</p>
                </div>

                {/* Path rows */}
                <div className="space-y-2">
                    {rows.map(row => {
                        const pathError = validatePath(row.path);
                        const isDuplicate = hasDuplicatePaths && rows.filter(r => r.path === row.path).length > 1;
                        const error = isDuplicate ? 'Duplicate context path is not allowed.' : pathError;

                        return (
                            <div key={row.id} className="flex items-start gap-2">
                                <div className="flex-1 space-y-1">
                                    <Input
                                        value={row.path}
                                        onChange={e => onPathChange(row.id, e.target.value)}
                                        placeholder="/"
                                        disabled={isReadOnly}
                                        aria-invalid={error !== null}
                                        aria-label="Context path"
                                    />
                                    {error && <p className="text-xs text-destructive">{error}</p>}
                                </div>
                                {!isReadOnly && (
                                    <button
                                        type="button"
                                        onClick={() => onDelete(row.id)}
                                        disabled={!canDelete}
                                        aria-label="Delete context path"
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
                        Add context path
                    </Button>
                )}
            </CardContent>
        </Card>
    );
}
