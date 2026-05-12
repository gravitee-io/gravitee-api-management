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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input, Label, Switch } from '@gravitee/graphene-core';
import { CircleCheckIcon, GlobeIcon, InfoIcon, PlusIcon, ServerIcon, Trash2Icon, XIcon } from '@gravitee/graphene-core/icons';
import { forwardRef, useImperativeHandle, useRef } from 'react';

import { proxyContextPathInputPattern, proxyContextPathInputTitle } from '../../../../utils/fieldRegistry';
import type { ApiCreationState } from '../../types/models';

export type ConfigureProxyStepRef = {
    validate: () => boolean;
};

type VirtualHostRow = Readonly<{ host: string; path: string; overrideAccess: boolean }>;

type ConfigureProxyStepProps = Readonly<{
    proxy: ApiCreationState['proxy'];
    errors: Record<string, string>;
    serverPathError?: string;
    pathVerifyPending?: boolean;
    updateField: (path: string, value: unknown) => void;
}>;

export const ConfigureProxyStep = forwardRef<ConfigureProxyStepRef, ConfigureProxyStepProps>(function ConfigureProxyStep(
    { proxy, errors, serverPathError, pathVerifyPending, updateField },
    ref,
) {
    const contextPathInputRef = useRef<HTMLInputElement>(null);
    const rootRef = useRef<HTMLDivElement>(null);
    const targetUrlInputRef = useRef<HTMLInputElement>(null);

    useImperativeHandle(ref, () => ({
        validate(): boolean {
            if (!proxy.enableVirtualHosts) {
                const cp = contextPathInputRef.current;
                if (cp && !cp.checkValidity()) {
                    cp.reportValidity();
                    return false;
                }
            } else {
                for (const el of Array.from(rootRef.current?.querySelectorAll<HTMLInputElement>('[data-vhost-context-path]') ?? [])) {
                    if (!el.checkValidity()) {
                        el.reportValidity();
                        return false;
                    }
                }
            }
            const url = targetUrlInputRef.current;
            if (url && !url.checkValidity()) {
                url.reportValidity();
                return false;
            }
            return true;
        },
    }));

    const toggleLabel = proxy.enableVirtualHosts ? 'Disable virtual hosts' : 'Enable virtual hosts';
    const ToggleIcon = proxy.enableVirtualHosts ? XIcon : CircleCheckIcon;

    const contextPathError = errors['proxy.contextPath'];
    const virtualHostsError = errors['proxy.virtualHosts'];
    const targetUrlError = errors['proxy.targetUrl'];

    const handleVirtualHostChange = (idx: number, patch: Partial<VirtualHostRow>) => {
        const next = proxy.virtualHosts.map((row, i) => (i === idx ? { ...row, ...patch } : row));
        updateField('proxy.virtualHosts', next);
    };

    const handleAddVirtualHost = () => {
        updateField('proxy.virtualHosts', [...proxy.virtualHosts, { host: '', path: '/', overrideAccess: false }]);
    };

    const handleRemoveVirtualHost = (idx: number) => {
        const next = proxy.virtualHosts.length <= 1 ? proxy.virtualHosts : proxy.virtualHosts.filter((_, i) => i !== idx);
        updateField('proxy.virtualHosts', next.length ? next : [{ host: '', path: '/', overrideAccess: false }]);
    };

    return (
        <div ref={rootRef} className="space-y-6">
            <Card className="rounded-xl border">
                <div className="px-4 pt-6 pb-3">
                    <div className="flex items-center gap-2 text-base font-semibold">
                        <GlobeIcon className="size-4 text-primary" aria-hidden="true" />
                        Configure common entrypoints fields
                    </div>
                    <p className="mt-1 text-sm text-muted-foreground">How consumers will access this API through the gateway.</p>
                </div>

                <div className="px-4 pb-6">
                    <div className="rounded-xl border">
                        <div className="flex items-center justify-between border-b bg-muted/30 px-4 py-3">
                            <span className="text-sm font-semibold">Entrypoints context</span>
                            <button
                                type="button"
                                onClick={() => updateField('proxy.enableVirtualHosts', !proxy.enableVirtualHosts)}
                                className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground"
                            >
                                <ToggleIcon className="size-3.5" aria-hidden="true" />
                                {toggleLabel}
                            </button>
                        </div>

                        {!proxy.enableVirtualHosts ? (
                            <div className="space-y-2 p-4">
                                <Label htmlFor="context-path">
                                    Context Path <span className="text-destructive">*</span>
                                </Label>
                                <div className="flex items-center gap-2">
                                    <span className="whitespace-nowrap font-mono text-sm text-muted-foreground">
                                        https://gateway.company.com
                                    </span>
                                    <Input
                                        ref={contextPathInputRef}
                                        id="context-path"
                                        className="font-mono"
                                        placeholder="/your-api"
                                        pattern={proxyContextPathInputPattern}
                                        title={proxyContextPathInputTitle}
                                        aria-invalid={Boolean(contextPathError)}
                                        value={proxy.contextPath}
                                        onChange={e => updateField('proxy.contextPath', e.target.value)}
                                    />
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    Must start with a <code className="text-[11px]">/</code>. Default follows your API name; edit as needed.
                                </p>
                                {contextPathError ? <p className="text-xs text-destructive">{contextPathError}</p> : null}
                                {pathVerifyPending ? <p className="text-xs text-muted-foreground">Checking path availability…</p> : null}
                                {serverPathError && !proxy.enableVirtualHosts ? (
                                    <p className="text-xs text-destructive">{serverPathError}</p>
                                ) : null}
                            </div>
                        ) : (
                            <div className="space-y-3 p-4">
                                <div className="overflow-hidden rounded-lg border">
                                    <div className="grid grid-cols-3 gap-x-3 bg-muted/40 px-3 py-2.5 text-xs">
                                        <div>
                                            <p className="font-semibold text-foreground">Virtual Host</p>
                                            <p className="text-muted-foreground">
                                                Host that must be set in the HTTP request to access your entrypoint.
                                            </p>
                                        </div>
                                        <div>
                                            <p className="inline-flex items-center gap-1 font-semibold text-foreground">
                                                Context-path <InfoIcon className="size-3 text-muted-foreground" aria-hidden="true" />
                                            </p>
                                            <p className="text-muted-foreground">
                                                Must start with a <code className="text-[10px]">/</code> and can contain uppercase letters,
                                                numbers, dashes, or underscores.
                                            </p>
                                        </div>
                                        <div>
                                            <p className="font-semibold text-foreground">Override access</p>
                                            <p className="text-muted-foreground">
                                                Enable override on the access URL of your Portal using virtual host.
                                            </p>
                                        </div>
                                    </div>

                                    <div className="divide-y">
                                        {(proxy.virtualHosts.length
                                            ? proxy.virtualHosts
                                            : [{ host: '', path: '/', overrideAccess: false }]
                                        ).map((row, idx) => (
                                            <div key={idx} className="grid grid-cols-3 items-center gap-x-3 px-3 py-3">
                                                <Input
                                                    placeholder="Host *"
                                                    aria-invalid={Boolean(virtualHostsError)}
                                                    value={row.host}
                                                    onChange={e => handleVirtualHostChange(idx, { host: e.target.value })}
                                                />
                                                <Input
                                                    className="font-mono"
                                                    placeholder="/"
                                                    data-vhost-context-path
                                                    pattern={proxyContextPathInputPattern}
                                                    title={proxyContextPathInputTitle}
                                                    aria-invalid={Boolean(virtualHostsError)}
                                                    value={row.path}
                                                    onChange={e => handleVirtualHostChange(idx, { path: e.target.value })}
                                                />
                                                <div className="flex items-center justify-between gap-3">
                                                    <div className="flex items-center gap-2">
                                                        <Switch
                                                            checked={row.overrideAccess}
                                                            onCheckedChange={checked =>
                                                                handleVirtualHostChange(idx, { overrideAccess: Boolean(checked) })
                                                            }
                                                        />
                                                        <span className="text-xs text-muted-foreground">Enable</span>
                                                    </div>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-muted-foreground hover:text-destructive disabled:opacity-30"
                                                        onClick={() => handleRemoveVirtualHost(idx)}
                                                        disabled={proxy.virtualHosts.length <= 1}
                                                        aria-label="Remove virtual host"
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden="true" />
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <Button type="button" variant="outline" size="sm" onClick={handleAddVirtualHost}>
                                    <PlusIcon className="mr-1.5 size-4" aria-hidden="true" />
                                    Add context-path
                                </Button>

                                {virtualHostsError ? <p className="text-xs text-destructive">{virtualHostsError}</p> : null}
                                {pathVerifyPending ? <p className="text-xs text-muted-foreground">Checking virtual hosts…</p> : null}
                                {serverPathError && proxy.enableVirtualHosts ? (
                                    <p className="text-xs text-destructive">{serverPathError}</p>
                                ) : null}
                            </div>
                        )}
                    </div>
                </div>
            </Card>

            <Card className="rounded-xl">
                <CardHeader className="space-y-1 p-4 pb-3 sm:p-6 sm:pb-4">
                    <CardTitle className="flex items-center gap-2">
                        <ServerIcon className="size-4 text-primary" aria-hidden="true" />
                        Upstream URL
                    </CardTitle>
                    <CardDescription>Where the gateway forwards traffic to.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4 p-4 pt-0 sm:p-6 sm:pt-0">
                    <div className="space-y-2">
                        <Label htmlFor="target-url">
                            Target URL <span className="text-destructive">*</span>
                        </Label>
                        <div className="relative">
                            <GlobeIcon
                                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                                aria-hidden="true"
                            />
                            <Input
                                ref={targetUrlInputRef}
                                id="target-url"
                                type="url"
                                className="font-mono"
                                style={{ paddingLeft: '2.75rem' }}
                                placeholder="https://api.internal:8443/v1"
                                aria-invalid={Boolean(targetUrlError)}
                                value={proxy.targetUrl}
                                onChange={e => updateField('proxy.targetUrl', e.target.value)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">The backend URL that the gateway forwards traffic to.</p>
                        {targetUrlError ? <p className="text-xs text-destructive">{targetUrlError}</p> : null}
                    </div>

                    <div className="flex items-start gap-3 rounded-lg border border-amber-200/60 bg-amber-50/60 p-3">
                        <InfoIcon className="mt-0.5 size-4 shrink-0 text-amber-700" aria-hidden="true" />
                        <p className="text-xs leading-relaxed">
                            <span className="font-semibold text-foreground">Tip: use internal DNS for upstreams.</span>{' '}
                            <span className="text-muted-foreground">
                                It keeps upstreams stable across environments and avoids hard-coding IPs.
                            </span>
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
});
