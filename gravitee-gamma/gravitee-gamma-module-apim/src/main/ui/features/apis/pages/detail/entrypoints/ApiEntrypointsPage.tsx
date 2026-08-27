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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, Skeleton } from '@gravitee/graphene-core';
import { CheckIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { ContextPathsCard } from './ContextPathsCard';
import { EntrypointsLanding } from './EntrypointsLanding';
import { ExposedEntrypointsCard } from './ExposedEntrypointsCard';
import { SwitchModeDialog } from './SwitchModeDialog';
import { TcpHostsCard } from './TcpHostsCard';
import type { ContextPathRow, VirtualHostRow } from './types';
import { validatePath } from './types';
import { VirtualHostsCard } from './VirtualHostsCard';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiEntrypoints } from '../../../hooks/useApiEntrypoints';
import type { ApiDetailDto, HttpListener, TcpListener } from '../../../types';
import type { TcpHostEntry } from '../../../types/apiCreation';
import { validateTcpHosts } from '../../../utils/apiCreationValidation';
import { hasTcpListeners } from '../../../utils/apiHttpProxy';

// ─── Helpers ──────────────────────────────────────────────────────────────────

function newId(): string {
    return Math.random().toString(36).slice(2, 10);
}

function getHttpListener(api: ApiDetailDto | null): HttpListener | undefined {
    return api?.listeners?.find(l => l.type === 'HTTP') as HttpListener | undefined;
}

function getTcpListener(api: ApiDetailDto | null): TcpListener | undefined {
    return api?.listeners?.find(l => l.type === 'TCP') as TcpListener | undefined;
}

function isVirtualHostMode(listener: HttpListener | undefined): boolean {
    return (listener?.hosts?.length ?? 0) > 0;
}

function listenerHasEntrypoints(listener: HttpListener | undefined): boolean {
    return (listener?.paths?.length ?? 0) > 0 || (listener?.hosts?.length ?? 0) > 0;
}

function hasDuplicates(rows: ContextPathRow[]): boolean {
    const paths = rows.map(r => r.path);
    return new Set(paths).size !== paths.length;
}

// ─── ApiEntrypointsPage ───────────────────────────────────────────────────────

export function ApiEntrypointsPage() {
    const { api, isLoading: apiLoading, permissionsReady } = useApiDetailContext();
    const isTcp = hasTcpListeners(api);

    // ── permission guard (mirrors legacy api-entrypoints.component.ts) ──
    // Requires both api-definition-u AND api-gateway_definition-u.
    // Kubernetes-managed APIs are always read-only regardless of user permissions.
    const canUpdateEntrypoints = useHasPermission({ allOf: ['api-definition-u', 'api-gateway_definition-u'] });
    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const isReadOnly = !permissionsReady || !canUpdateEntrypoints || isKubernetesManaged;

    // ── derive initial form state from api ──
    const [showConfig, setShowConfig] = useState(false);
    const [virtualHostMode, setVirtualHostMode] = useState(false);
    const [contextPaths, setContextPaths] = useState<ContextPathRow[]>([{ id: newId(), path: '/' }]);
    const [virtualHosts, setVirtualHosts] = useState<VirtualHostRow[]>([{ id: newId(), host: '', path: '/', overrideAccess: false }]);
    const [tcpHosts, setTcpHosts] = useState<TcpHostEntry[]>([{ id: newId(), host: '' }]);
    const [isDirty, setIsDirty] = useState(false);
    const [switchModeDialogOpen, setSwitchModeDialogOpen] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    const { exposedQuery, saveMutation } = useApiEntrypoints(showConfig && !isTcp);

    const initFromApi = useCallback(
        (apiData: ApiDetailDto | null) => {
            if (!apiData) return;

            if (hasTcpListeners(apiData)) {
                const tcpListener = getTcpListener(apiData);
                const hosts = tcpListener?.hosts ?? [];
                setTcpHosts(hosts.length > 0 ? hosts.map(host => ({ id: newId(), host })) : [{ id: newId(), host: '' }]);
                setIsDirty(false);
                setSaveError(null);
                return;
            }

            const listener = getHttpListener(apiData);

            if (isVirtualHostMode(listener)) {
                setVirtualHostMode(true);
                setVirtualHosts(
                    (listener?.hosts ?? []).map(h => ({
                        id: newId(),
                        host: h.host,
                        path: h.path,
                        overrideAccess: h.overrideAccess ?? false,
                    })),
                );
                setContextPaths((listener?.paths ?? []).map(p => ({ id: newId(), path: p.path })));
            } else {
                setVirtualHostMode(false);
                const paths = listener?.paths ?? [];
                setContextPaths(paths.length > 0 ? paths.map(p => ({ id: newId(), path: p.path })) : [{ id: newId(), path: '/' }]);
            }

            setShowConfig(listenerHasEntrypoints(listener));
            setIsDirty(false);
            setSaveError(null);
        },
        [], // state setters are stable
    );

    // Initialize form once api data arrives; re-init only when API identity changes.
    // Initialized to null (not api?.id) so the condition fires on first render even when
    // data is already cached (e.g. in tests or when React Query has a warm cache).
    const [seededApiId, setSeededApiId] = useState<string | null>(null);
    if (api && api.id !== seededApiId) {
        setSeededApiId(api.id ?? null);
        initFromApi(api);
    }

    // ── validation ──
    const contextPathErrors = contextPaths.map(r => validatePath(r.path));
    const contextPathHasDupes = hasDuplicates(contextPaths);
    const virtualHostErrors = virtualHosts.map(r => validatePath(r.path));

    const isContextPathValid = contextPathErrors.every(e => e === null) && !contextPathHasDupes;
    const isVirtualHostValid = virtualHostErrors.every(e => e === null);
    const isTcpHostsValid = validateTcpHosts(tcpHosts) === null;
    const isFormValid = isTcp ? isTcpHostsValid : virtualHostMode ? isVirtualHostValid : isContextPathValid;
    const canSave = isDirty && isFormValid && !saveMutation.isPending;

    // ── form mutations ──
    function markDirty() {
        setIsDirty(true);
        setSaveError(null);
    }

    function addContextPath() {
        setContextPaths(prev => [...prev, { id: newId(), path: '/' }]);
        markDirty();
    }

    function deleteContextPath(id: string) {
        if (contextPaths.length <= 1) return;
        setContextPaths(prev => prev.filter(r => r.id !== id));
        markDirty();
    }

    function updateContextPath(id: string, path: string) {
        setContextPaths(prev => prev.map(r => (r.id === id ? { ...r, path } : r)));
        markDirty();
    }

    function addVirtualHost() {
        setVirtualHosts(prev => [...prev, { id: newId(), host: '', path: '/', overrideAccess: false }]);
        markDirty();
    }

    function deleteVirtualHost(id: string) {
        setVirtualHosts(prev => prev.filter(r => r.id !== id));
        markDirty();
    }

    function updateVirtualHostField(id: string, field: 'host' | 'path' | 'overrideAccess', value: string | boolean) {
        setVirtualHosts(prev => prev.map(r => (r.id === id ? { ...r, [field]: value } : r)));
        markDirty();
    }

    function addTcpHost() {
        setTcpHosts(prev => [...prev, { id: newId(), host: '' }]);
        markDirty();
    }

    function deleteTcpHost(id: string) {
        if (tcpHosts.length <= 1) return;
        setTcpHosts(prev => prev.filter(r => r.id !== id));
        markDirty();
    }

    function updateTcpHost(id: string, host: string) {
        setTcpHosts(prev => prev.map(r => (r.id === id ? { ...r, host } : r)));
        markDirty();
    }

    function handleEnableVirtualHosts() {
        setVirtualHostMode(true);
        if (virtualHosts.length === 0) {
            setVirtualHosts([{ id: newId(), host: '', path: '/', overrideAccess: false }]);
        }
        markDirty();
    }

    function handleDisableVirtualHosts() {
        setSwitchModeDialogOpen(true);
    }

    function confirmSwitchToContextPath() {
        setSwitchModeDialogOpen(false);
        setVirtualHostMode(false);
        if (contextPaths.length === 0) {
            setContextPaths([{ id: newId(), path: '/' }]);
        }
        markDirty();
    }

    function handleAddFirstContextPath() {
        setContextPaths([{ id: newId(), path: '/' }]);
        setShowConfig(true);
        markDirty();
    }

    // ── discard ──
    function handleDiscard() {
        initFromApi(api);
    }

    // ── save ──
    function handleSave() {
        if (!api || !isFormValid) return;

        const updatedListeners: (HttpListener | TcpListener)[] = isTcp
            ? [
                  ...(api.listeners ?? []).filter(l => l.type !== 'TCP'),
                  { ...(getTcpListener(api) ?? { type: 'TCP' as const }), hosts: tcpHosts.map(r => r.host.trim()) },
              ]
            : (() => {
                  const existingListener = getHttpListener(api);
                  const updatedListener: HttpListener = {
                      ...(existingListener ?? { type: 'HTTP' }),
                      paths: virtualHostMode ? (existingListener?.paths ?? []) : contextPaths.map(r => ({ path: r.path })),
                      hosts: virtualHostMode
                          ? virtualHosts.map(r => ({ host: r.host, path: r.path, overrideAccess: r.overrideAccess }))
                          : [],
                  };
                  // Keep all listeners, replace/add the HTTP one
                  return [...(api.listeners ?? []).filter(l => l.type !== 'HTTP'), updatedListener];
              })();

        saveMutation.mutate(updatedListeners, {
            onSuccess: () => {
                setIsDirty(false);
                setSaveError(null);
            },
            onError: (err: unknown) => {
                const message = err instanceof Error ? err.message : 'Failed to save entrypoints.';
                setSaveError(message);
            },
        });
    }

    // ── render ──
    if (apiLoading) {
        return (
            <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1.5">
                        <Skeleton className="h-7 w-40 rounded" />
                        <Skeleton className="h-4 w-72 rounded" />
                    </div>
                    <Skeleton className="h-9 w-28 rounded-md" />
                </div>
                <Skeleton className="h-48 w-full rounded-xl" />
            </div>
        );
    }

    if (isTcp) {
        return (
            <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold tracking-tight">Entrypoints</h1>
                        <p className="text-sm text-muted-foreground">Configure the gateway hosts this TCP API listens on.</p>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                        {isDirty && !isReadOnly && (
                            <Button size="sm" variant="outline" onClick={handleDiscard} aria-label="Discard changes">
                                Discard
                            </Button>
                        )}
                        {!isReadOnly && (
                            <Button size="sm" onClick={handleSave} disabled={!canSave} className="gap-1.5" aria-label="Save changes">
                                <CheckIcon className="size-3.5" />
                                Save changes
                            </Button>
                        )}
                    </div>
                </div>

                {saveError && (
                    <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3">
                        <p className="text-sm text-destructive">{saveError}</p>
                    </div>
                )}

                {saveMutation.isSuccess && !isDirty && (
                    <div className="rounded-lg border border-success/30 bg-success/5 px-4 py-3">
                        <p className="text-sm text-success">Configuration successfully saved!</p>
                    </div>
                )}

                <TcpHostsCard
                    rows={tcpHosts}
                    onAdd={addTcpHost}
                    onDelete={deleteTcpHost}
                    onHostChange={updateTcpHost}
                    isReadOnly={isReadOnly}
                />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* ── Page header ── */}
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Entrypoints</h1>
                    <p className="text-sm text-muted-foreground">Configure how consumers reach this API through the gateway.</p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                    {isDirty && showConfig && !isReadOnly && (
                        <Button size="sm" variant="outline" onClick={handleDiscard} aria-label="Discard changes">
                            Discard
                        </Button>
                    )}
                    {showConfig && !isReadOnly && (
                        <Button size="sm" onClick={handleSave} disabled={!canSave} className="gap-1.5" aria-label="Save changes">
                            <CheckIcon className="size-3.5" />
                            Save changes
                        </Button>
                    )}
                    {!isReadOnly && !virtualHostMode && !showConfig && (
                        <Button size="sm" onClick={handleAddFirstContextPath} className="gap-1.5">
                            <PlusIcon className="size-3.5" />
                            Add context path
                        </Button>
                    )}
                </div>
            </div>

            {saveError && (
                <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3">
                    <p className="text-sm text-destructive">{saveError}</p>
                </div>
            )}

            {saveMutation.isSuccess && !isDirty && (
                <div className="rounded-lg border border-success/30 bg-success/5 px-4 py-3">
                    <p className="text-sm text-success">Configuration successfully saved!</p>
                </div>
            )}

            {/* ── Landing state ── */}
            {!showConfig && <EntrypointsLanding />}

            {/* ── Config state ── */}
            {showConfig && (
                <>
                    {!virtualHostMode && (
                        <ContextPathsCard
                            rows={contextPaths}
                            onAdd={addContextPath}
                            onDelete={deleteContextPath}
                            onPathChange={updateContextPath}
                            onToggleVirtualHosts={handleEnableVirtualHosts}
                            isReadOnly={isReadOnly}
                        />
                    )}

                    {virtualHostMode && (
                        <VirtualHostsCard
                            rows={virtualHosts}
                            onAdd={addVirtualHost}
                            onDelete={deleteVirtualHost}
                            onFieldChange={updateVirtualHostField}
                            onDisableVirtualHosts={handleDisableVirtualHosts}
                            isReadOnly={isReadOnly}
                        />
                    )}

                    <ExposedEntrypointsCard
                        entrypoints={exposedQuery.data ?? []}
                        isLoading={exposedQuery.isLoading}
                        virtualHostMode={virtualHostMode}
                    />
                </>
            )}

            {/* ── Switch mode confirmation dialog ── */}
            <SwitchModeDialog
                open={switchModeDialogOpen}
                onConfirm={confirmSwitchToContextPath}
                onCancel={() => setSwitchModeDialogOpen(false)}
            />
        </div>
    );
}
