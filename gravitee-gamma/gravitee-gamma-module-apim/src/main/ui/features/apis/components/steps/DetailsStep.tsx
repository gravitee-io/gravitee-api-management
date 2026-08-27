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
import { cn, Input, Label, Textarea } from '@gravitee/graphene-core';
import { CableIcon, CircleCheckIcon, GlobeIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import type { CSSProperties } from 'react';

import { policyStudioKeys } from '../../hooks/usePolicyStudioData';
import { listEntrypointPlugins } from '../../services/policyStudioService';
import { useApiCreation } from '../../store/apiCreationStore';
import type { ApiProtocol } from '../../types/apiCreation';

interface ProxyKindOption {
    id: ApiProtocol;
    /** Entrypoint plugin id backing this option (classic console `ConnectorPlugin.id`), used to check `deployed`. */
    pluginId: string;
    label: string;
    description: string;
    Icon: LucideIcon;
}

const PROXY_KIND_OPTIONS: ProxyKindOption[] = [
    {
        id: 'HTTP',
        pluginId: 'http-proxy',
        label: 'HTTP Proxy',
        description: 'Expose an HTTP backend through context paths or virtual hosts.',
        Icon: GlobeIcon,
    },
    {
        id: 'TCP',
        pluginId: 'tcp-proxy',
        label: 'TCP Proxy',
        description: 'Expose a TCP backend through host-based listeners. Only a keyless plan is supported.',
        Icon: CableIcon,
    },
];

export function DetailsStep() {
    const { state, dispatch } = useApiCreation();
    const { form, validationErrors: errors } = state;

    const { data: entrypointPlugins, isLoading: entrypointPluginsLoading } = useQuery({
        queryKey: policyStudioKeys.entrypoints(),
        queryFn: listEntrypointPlugins,
        staleTime: 300_000,
    });

    /**
     * Fails closed once the plugin list has loaded: a type is only selectable if its plugin
     * is present AND deployed. While the query is still loading, every option stays enabled
     * so HTTP (the default) isn't disabled during the initial fetch; a query error also fails
     * closed since `entrypointPlugins` stays undefined but `isLoading` settles to false.
     */
    function isPluginUnavailable(pluginId: string): boolean {
        if (entrypointPluginsLoading) return false;
        return entrypointPlugins?.find(p => p.id === pluginId)?.deployed !== true;
    }

    function update(patch: Partial<typeof form>) {
        dispatch({ type: 'UPDATE_FORM', patch });
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">API Details</h2>
                <p className="text-sm text-muted-foreground">Name and describe your API proxy.</p>
            </div>

            <div className="space-y-5">
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label htmlFor="details-api-name">
                            API Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="details-api-name"
                            placeholder="e.g. Payment Service API"
                            value={form.apiName}
                            onChange={e => update({ apiName: e.target.value })}
                            aria-invalid={Boolean(errors['apiName'])}
                        />
                        {errors['apiName'] && <p className="text-xs text-destructive">{errors['apiName']}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="details-api-version">
                            Version <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="details-api-version"
                            placeholder="e.g. 1.0.0"
                            value={form.apiVersion}
                            onChange={e => update({ apiVersion: e.target.value })}
                            aria-invalid={Boolean(errors['apiVersion'])}
                        />
                        {errors['apiVersion'] && <p className="text-xs text-destructive">{errors['apiVersion']}</p>}
                    </div>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="details-description">Description</Label>
                    <Textarea
                        id="details-description"
                        placeholder="Describe what this API does and who should use it."
                        value={form.apiDescription}
                        onChange={e => update({ apiDescription: e.target.value })}
                        maxLength={250}
                        rows={3}
                        style={{ fieldSizing: 'fixed' } as unknown as CSSProperties}
                    />
                    <p className="text-xs text-muted-foreground text-right">{form.apiDescription.length}/250</p>
                </div>

                <div className="space-y-3">
                    <div className="space-y-1">
                        <p className="text-sm font-medium">Select API Type</p>
                        <p className="text-xs text-muted-foreground">Choose how this proxy listens and forwards traffic.</p>
                    </div>
                    <div className="grid grid-cols-2 gap-3" role="radiogroup" aria-label="API type">
                        {PROXY_KIND_OPTIONS.map(opt => {
                            const Icon = opt.Icon;
                            const selected = form.protocol === opt.id;
                            const unavailable = isPluginUnavailable(opt.pluginId);
                            return (
                                <button
                                    key={opt.id}
                                    type="button"
                                    role="radio"
                                    aria-checked={selected}
                                    disabled={unavailable}
                                    onClick={() => !unavailable && update({ protocol: opt.id })}
                                    className={cn(
                                        'flex items-start gap-3 rounded-xl border-2 p-4 text-left transition-all',
                                        unavailable
                                            ? 'opacity-50 cursor-not-allowed border-border'
                                            : selected
                                              ? 'border-primary bg-primary/5 ring-2 ring-primary/30 ring-offset-1'
                                              : 'border-border hover:border-foreground/20',
                                    )}
                                >
                                    <div
                                        className={cn(
                                            'size-10 rounded-lg flex items-center justify-center shrink-0',
                                            selected ? 'bg-primary/10' : 'bg-muted',
                                        )}
                                    >
                                        <Icon className="size-5" aria-hidden />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 flex-wrap">
                                            <span className="text-sm font-medium">{opt.label}</span>
                                            {opt.id === 'HTTP' && !unavailable && (
                                                <span className="rounded-full px-2 py-0.5 text-xs text-muted-foreground bg-muted">
                                                    Default
                                                </span>
                                            )}
                                            {unavailable && (
                                                <span className="rounded-full px-2 py-0.5 text-xs text-muted-foreground bg-muted">
                                                    Not available
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-xs text-muted-foreground mt-1">{opt.description}</p>
                                    </div>
                                    {selected && !unavailable && (
                                        <CircleCheckIcon className="size-5 text-primary shrink-0 mt-0.5" aria-hidden />
                                    )}
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}
