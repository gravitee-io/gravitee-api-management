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
import { Button, Input, Label, Switch } from '@gravitee/graphene-core';
import { PlusIcon, ServerIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { useGatewayPrefix } from '../../hooks/useGatewayPrefix';
import { useVerifyContextPath } from '../../hooks/useVerifyContextPath';
import { useApiCreation } from '../../store/apiCreationStore';
import type { VirtualHostEntry } from '../../types/apiCreation';

export function EntrypointsStep() {
    const { state, dispatch } = useApiCreation();
    const { form, validationErrors: errors } = state;
    const gatewayPrefix = useGatewayPrefix();
    useVerifyContextPath();

    function update(patch: Partial<typeof form>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    function updateVirtualHost(index: number, patch: Omit<Partial<VirtualHostEntry>, 'id'>) {
        dispatch({ type: 'UPDATE_VIRTUAL_HOST', index, patch });
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">Entrypoints & Backend</h2>
                <p className="text-sm text-muted-foreground">Define how clients reach the gateway and where requests are forwarded.</p>
            </div>

            <div className="space-y-5">
                {/* Context path / virtual hosts */}
                <div className="space-y-4 rounded-xl border bg-muted/30 p-4">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium">Gateway path</p>
                            <p className="text-xs text-muted-foreground">The path clients use to reach this API on the gateway.</p>
                        </div>
                        <div className="flex items-center gap-2">
                            <span className="text-xs text-muted-foreground">Virtual hosts</span>
                            <Switch
                                checked={form.virtualHostsEnabled}
                                onCheckedChange={enabled => update({ virtualHostsEnabled: enabled })}
                                size="sm"
                                aria-label="Enable virtual hosts"
                            />
                        </div>
                    </div>

                    {form.virtualHostsEnabled ? (
                        <div className="space-y-3">
                            {form.virtualHosts.map((vh, index) => (
                                <div key={vh.id} className="grid grid-cols-2 gap-3 items-end">
                                    <div className="space-y-1.5">
                                        <Label htmlFor={`vh-host-${index}`} className="text-xs">
                                            Host
                                        </Label>
                                        <Input
                                            id={`vh-host-${index}`}
                                            placeholder="e.g. api.example.com"
                                            value={vh.host}
                                            onChange={e => updateVirtualHost(index, { host: e.target.value })}
                                        />
                                    </div>
                                    <div className="flex items-end gap-2">
                                        <div className="flex-1 space-y-1.5">
                                            <Label htmlFor={`vh-path-${index}`} className="text-xs">
                                                Path
                                            </Label>
                                            <Input
                                                id={`vh-path-${index}`}
                                                placeholder="/"
                                                value={vh.path}
                                                onChange={e => updateVirtualHost(index, { path: e.target.value })}
                                            />
                                        </div>
                                        {form.virtualHosts.length > 1 && (
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                onClick={() => dispatch({ type: 'REMOVE_VIRTUAL_HOST', index })}
                                                aria-label="Remove virtual host"
                                                className="shrink-0"
                                            >
                                                <Trash2Icon className="size-4 text-muted-foreground" aria-hidden />
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            ))}
                            {errors['virtualHosts'] && <p className="text-xs text-destructive">{errors['virtualHosts']}</p>}
                            <Button variant="outline" size="sm" onClick={() => dispatch({ type: 'ADD_VIRTUAL_HOST' })} className="w-full">
                                <PlusIcon className="size-4" aria-hidden />
                                Add virtual host
                            </Button>
                        </div>
                    ) : (
                        <div className="space-y-2">
                            <Label htmlFor="context-path">
                                Context path <span className="text-destructive">*</span>
                            </Label>
                            <div
                                className="flex items-stretch rounded-md border overflow-hidden"
                                style={errors['contextPath'] ? { borderColor: 'var(--color-destructive)' } : undefined}
                            >
                                <span
                                    className="flex items-center px-3 text-sm font-mono text-muted-foreground whitespace-nowrap border-r bg-muted/30 select-none"
                                    aria-hidden
                                >
                                    {gatewayPrefix}
                                </span>
                                <Input
                                    id="context-path"
                                    placeholder="/my-api"
                                    value={form.contextPath}
                                    onChange={e => update({ contextPath: e.target.value })}
                                    aria-invalid={Boolean(errors['contextPath'])}
                                    style={{ border: 'none', borderRadius: 0, boxShadow: 'none', flex: 1, fontFamily: 'monospace' }}
                                />
                            </div>
                            {errors['contextPath'] && <p className="text-xs text-destructive">{errors['contextPath']}</p>}
                            <p className="text-xs text-muted-foreground">Path prefix clients will use to access this API.</p>
                        </div>
                    )}
                </div>

                {/* Target URL */}
                <div className="space-y-2">
                    <Label htmlFor="target-url">
                        Target URL <span className="text-destructive">*</span>
                    </Label>
                    <div className="relative">
                        <ServerIcon
                            className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none"
                            aria-hidden
                        />
                        <Input
                            id="target-url"
                            placeholder="https://api.internal.example.com"
                            value={form.targetUrl}
                            onChange={e => update({ targetUrl: e.target.value })}
                            aria-invalid={Boolean(errors['targetUrl'])}
                            style={{ paddingLeft: '2.5rem' }}
                        />
                    </div>
                    {errors['targetUrl'] && <p className="text-xs text-destructive">{errors['targetUrl']}</p>}
                    <p className="text-xs text-muted-foreground">The upstream backend the gateway will forward requests to.</p>
                </div>
            </div>
        </div>
    );
}
