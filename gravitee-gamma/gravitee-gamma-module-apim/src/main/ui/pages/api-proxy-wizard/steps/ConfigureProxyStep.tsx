import { Alert, AlertDescription, Button, Card, CardContent, cn } from '@gravitee/graphene-core';
import { CheckCircle2, Globe, Info, Plus, Trash2, X, Zap } from 'lucide-react';

import type { ProxyConfigurationModel, VirtualHostModel } from '../apiProxyWizardModels';

type Props = {
    readonly value: ProxyConfigurationModel;
    readonly onChange: (patch: Partial<ProxyConfigurationModel>) => void;
};

function updateVirtualHostRows(rows: readonly VirtualHostModel[], idx: number, patch: Partial<VirtualHostModel>): VirtualHostModel[] {
    return rows.map((r, i) => (i === idx ? { ...r, ...patch } : r));
}

export function ConfigureProxyStep({ value, onChange }: Props) {
    const entrypoints = value.entrypoints;
    const virtualHostsEnabled = entrypoints.type === 'virtual_hosts';
    const virtualHosts = virtualHostsEnabled ? entrypoints.virtualHosts : [{ host: '', path: '/', overrideAccess: false }];

    const setVirtualHostsEnabled = (enabled: boolean) => {
        if (enabled) {
            onChange({
                entrypoints: {
                    type: 'virtual_hosts',
                    virtualHosts: [{ host: '', path: '/', overrideAccess: false }],
                },
            });
        } else {
            onChange({
                entrypoints: {
                    type: 'context_path',
                    contextPath: entrypoints.type === 'context_path' ? entrypoints.contextPath : '',
                },
            });
        }
    };

    const contextPath = entrypoints.type === 'context_path' ? entrypoints.contextPath : '';

    const updateVirtualHost = (idx: number, patch: Partial<VirtualHostModel>) => {
        if (entrypoints.type !== 'virtual_hosts') return;
        onChange({
            entrypoints: {
                type: 'virtual_hosts',
                virtualHosts: updateVirtualHostRows(entrypoints.virtualHosts, idx, patch),
            },
        });
    };

    const addVirtualHost = () => {
        if (entrypoints.type !== 'virtual_hosts') return;
        onChange({
            entrypoints: {
                type: 'virtual_hosts',
                virtualHosts: [...entrypoints.virtualHosts, { host: '', path: '/', overrideAccess: false }],
            },
        });
    };

    const removeVirtualHost = (idx: number) => {
        if (entrypoints.type !== 'virtual_hosts') return;
        const next = entrypoints.virtualHosts.length <= 1 ? entrypoints.virtualHosts : entrypoints.virtualHosts.filter((_, i) => i !== idx);
        onChange({
            entrypoints: {
                type: 'virtual_hosts',
                virtualHosts: next,
            },
        });
    };

    return (
        <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
            <div className="space-y-6">
                <Alert className="rounded-xl border p-5 bg-muted/30">
                    <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                    <AlertDescription className="text-muted-foreground text-sm">
                        Configure how consumers reach your API and where the gateway forwards traffic.
                    </AlertDescription>
                </Alert>

                <Card className="rounded-xl border">
                    <CardContent className="p-6 space-y-5">
                        <div className="flex items-center gap-2">
                            <Globe className="size-4 text-primary" aria-hidden />
                            <div className="text-base font-semibold">Configure common entrypoints fields</div>
                        </div>
                        <div className="text-xs text-muted-foreground">How consumers will access this API through the gateway.</div>

                        <div className="rounded-xl border overflow-hidden">
                            <div className="flex items-center justify-between border-b px-4 py-3 bg-muted/30">
                                <span className="text-sm font-semibold">Entrypoints context</span>
                                <button
                                    type="button"
                                    onClick={() => setVirtualHostsEnabled(!virtualHostsEnabled)}
                                    className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors"
                                >
                                    {virtualHostsEnabled ? (
                                        <>
                                            <X className="size-3.5" aria-hidden />
                                            Disable virtual hosts
                                        </>
                                    ) : (
                                        <>
                                            <CheckCircle2 className="size-3.5" aria-hidden />
                                            Enable virtual hosts
                                        </>
                                    )}
                                </button>
                            </div>

                            {!virtualHostsEnabled ? (
                                <div className="p-4 space-y-2">
                                    <label className="text-sm font-medium">
                                        Context Path <span className="text-destructive">*</span>
                                    </label>
                                    <div className="flex items-center gap-2">
                                        <span className="text-sm text-muted-foreground font-mono whitespace-nowrap">https://gateway.company.com</span>
                                        <input
                                            value={contextPath}
                                            onChange={e =>
                                                onChange({
                                                    entrypoints: { type: 'context_path', contextPath: e.target.value },
                                                })
                                            }
                                            placeholder="/your-api"
                                            className={cn(
                                                'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none font-mono',
                                                'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                            )}
                                        />
                                    </div>
                                    <p className="text-xs text-muted-foreground">
                                        Must start with <code className="text-[11px]">/</code> and can contain uppercase letters, numbers, dash or underscore.
                                    </p>
                                </div>
                            ) : (
                                <div className="p-4 space-y-3">
                                    <div className="overflow-hidden rounded-lg border">
                                        <div className="grid grid-cols-[1.4fr_1.4fr_1fr_auto] gap-x-3 bg-muted/40 px-3 py-2.5 text-xs">
                                            <div>
                                                <p className="font-semibold text-foreground">Virtual Host</p>
                                                <p className="text-muted-foreground">Host that must match the HTTP request host header.</p>
                                            </div>
                                            <div>
                                                <p className="font-semibold text-foreground">Context-path</p>
                                                <p className="text-muted-foreground">
                                                    Must start with <code className="text-[10px]">/</code>.
                                                </p>
                                            </div>
                                            <div>
                                                <p className="font-semibold text-foreground">Override access</p>
                                                <p className="text-muted-foreground">Override portal access URL using this virtual host.</p>
                                            </div>
                                            <div />
                                        </div>
                                        <div className="divide-y">
                                            {virtualHosts.map((row, idx) => (
                                                <div key={idx} className="grid grid-cols-[1.4fr_1.4fr_1fr_auto] gap-x-3 items-center px-3 py-3">
                                                    <input
                                                        placeholder="Host *"
                                                        value={row.host}
                                                        onChange={e => updateVirtualHost(idx, { host: e.target.value })}
                                                        className={cn(
                                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                                        )}
                                                    />
                                                    <input
                                                        placeholder="/"
                                                        value={row.path}
                                                        onChange={e => updateVirtualHost(idx, { path: e.target.value })}
                                                        className={cn(
                                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none font-mono',
                                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                                        )}
                                                    />
                                                    <label className="inline-flex items-center gap-2 text-xs text-muted-foreground">
                                                        <input
                                                            type="checkbox"
                                                            checked={row.overrideAccess}
                                                            onChange={e => updateVirtualHost(idx, { overrideAccess: e.target.checked })}
                                                            className="size-4 rounded border"
                                                        />
                                                        Enable
                                                    </label>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="size-8 text-muted-foreground hover:text-destructive disabled:opacity-30"
                                                        onClick={() => removeVirtualHost(idx)}
                                                        disabled={virtualHosts.length <= 1}
                                                        aria-label="Remove virtual host"
                                                    >
                                                        <Trash2 className="size-4" aria-hidden />
                                                    </Button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>

                                    <Button type="button" variant="outline" size="sm" onClick={addVirtualHost}>
                                        <Plus className="size-4 mr-1.5" aria-hidden />
                                        Add context-path
                                    </Button>
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>

                <Card className="rounded-xl border">
                    <CardContent className="p-6 space-y-4">
                        <div className="flex items-center gap-2">
                            <Zap className="size-4 text-amber-500" aria-hidden />
                            <div className="text-base font-semibold">Upstream URL</div>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">
                                Target URL <span className="text-destructive">*</span>
                            </label>
                            <div className="relative">
                                <Globe className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
                                <input
                                    value={value.upstreamUrl}
                                    onChange={e => onChange({ upstreamUrl: e.target.value })}
                                    placeholder="https://api.internal:8443/v1"
                                    className={cn(
                                        'h-9 w-full rounded-md border bg-background px-9 text-sm outline-none font-mono',
                                        'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                    )}
                                />
                            </div>
                            <p className="text-xs text-muted-foreground">The backend URL that the gateway forwards traffic to.</p>
                        </div>
                    </CardContent>
                </Card>
            </div>

            <div className="space-y-4">
                <Card className="rounded-xl border-primary/20 bg-primary/5">
                    <CardContent className="p-6 space-y-3">
                        <p className="text-sm font-medium">Full Gateway URL</p>
                        <code className="block rounded-lg bg-background border px-3 py-2.5 text-xs font-mono break-all">
                            {previewGatewayUrl(value.entrypoints)}
                        </code>
                        <p className="text-xs text-muted-foreground leading-relaxed">
                            Consumers send requests to this URL; the gateway forwards them to your upstream service.
                        </p>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}

function previewGatewayUrl(entrypoints: ProxyConfigurationModel['entrypoints']): string {
    if (entrypoints.type === 'virtual_hosts') {
        const first = entrypoints.virtualHosts[0];
        const host = first?.host?.trim() || 'your-host.example.com';
        const path = first?.path?.trim() || '/';
        return `https://${host}${path === '/' ? '' : path}`;
    }
    const path = entrypoints.contextPath.trim() || '/...';
    return `https://gateway.company.com${path}`;
}

