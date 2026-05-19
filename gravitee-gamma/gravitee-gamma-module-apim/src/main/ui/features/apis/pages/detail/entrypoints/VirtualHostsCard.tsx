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
import { Button, Card, CardContent, Input, Switch } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { InfoTooltip } from './InfoTooltip';
import type { VirtualHostRow } from './types';
import { validatePath } from './types';

interface VirtualHostsCardProps {
    rows: VirtualHostRow[];
    onAdd: () => void;
    onDelete: (id: string) => void;
    onFieldChange: (id: string, field: 'host' | 'path' | 'overrideAccess', value: string | boolean) => void;
    onDisableVirtualHosts: () => void;
    isReadOnly: boolean;
}

export function VirtualHostsCard({
    rows,
    onAdd,
    onDelete,
    onFieldChange,
    onDisableVirtualHosts,
    isReadOnly,
}: Readonly<VirtualHostsCardProps>) {
    return (
        <Card>
            <CardContent className="p-5 space-y-4">
                {/* Header row */}
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <div className="text-sm font-semibold text-foreground flex items-center">
                            Virtual hosts
                            <InfoTooltip text="Map Virtual host, Context path, and Override access per row — matching the management console listener configuration." />
                        </div>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                            Map Virtual host, Context path, and Override access per row — matching the management console listener
                            configuration.
                        </p>
                    </div>
                    {!isReadOnly && (
                        <div className="flex items-center gap-2 shrink-0">
                            <span className="text-xs text-muted-foreground whitespace-nowrap">Enable virtual hosts</span>
                            <Switch checked={true} onCheckedChange={onDisableVirtualHosts} aria-label="Enable virtual hosts" />
                        </div>
                    )}
                </div>

                {/* Column headers */}
                <div className="grid gap-2" style={{ gridTemplateColumns: '1fr 1fr auto auto' }}>
                    <div>
                        <p className="text-xs font-medium text-muted-foreground">Virtual host</p>
                        <p className="text-xs text-muted-foreground">
                            Host that must be set in the HTTP request to access this entrypoint.
                        </p>
                    </div>
                    <div>
                        <div className="text-xs font-medium text-muted-foreground flex items-center">
                            Context path
                            <InfoTooltip text="Path segment appended to the virtual host for this listener." />
                        </div>
                        <p className="text-xs text-muted-foreground">Path segment appended to the virtual host for this listener.</p>
                    </div>
                    <div>
                        <div className="text-xs font-medium text-muted-foreground flex items-center">
                            Override access
                            <InfoTooltip text="Portal access URL override for this virtual host." />
                        </div>
                        <p className="text-xs text-muted-foreground">Portal access URL override for this virtual host.</p>
                    </div>
                    <div />
                </div>

                {/* Host rows */}
                <div className="space-y-2">
                    {rows.map(row => {
                        const pathError = validatePath(row.path);
                        return (
                            <div key={row.id} className="grid items-start gap-2" style={{ gridTemplateColumns: '1fr 1fr auto auto' }}>
                                <Input
                                    value={row.host}
                                    onChange={e => onFieldChange(row.id, 'host', e.target.value)}
                                    placeholder="api.company.com"
                                    disabled={isReadOnly}
                                    aria-label="Virtual host"
                                />
                                <div className="space-y-1">
                                    <Input
                                        value={row.path}
                                        onChange={e => onFieldChange(row.id, 'path', e.target.value)}
                                        placeholder="/"
                                        disabled={isReadOnly}
                                        aria-label="Context path"
                                        aria-invalid={pathError !== null}
                                    />
                                    {pathError && <p className="text-xs text-destructive">{pathError}</p>}
                                </div>
                                <div className="flex items-center justify-center gap-1.5 pt-2">
                                    <Switch
                                        checked={row.overrideAccess}
                                        onCheckedChange={v => onFieldChange(row.id, 'overrideAccess', v)}
                                        disabled={isReadOnly}
                                        aria-label="Enable override access"
                                    />
                                    <span className="text-xs text-muted-foreground">Enable</span>
                                </div>
                                {!isReadOnly && (
                                    <button
                                        type="button"
                                        onClick={() => onDelete(row.id)}
                                        aria-label="Delete virtual host row"
                                        className="p-1.5 mt-1 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
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
                        Add virtual host row
                    </Button>
                )}

                <p className="text-xs text-muted-foreground flex items-start gap-1.5">
                    <span className="shrink-0">↕</span>
                    Switching off virtual hosts keeps separate context-path and virtual-host lists; host-specific values are not applied
                    until you enable virtual hosts again.
                </p>
            </CardContent>
        </Card>
    );
}
