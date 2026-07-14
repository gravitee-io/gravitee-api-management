/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Button, Input } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { getApiById } from '../../editor/services/api.service';
import { getPublishedApiNavItems } from '../../../blocks/ApiCatalogBlock/catalog-utils';
import { getNavItems } from '../../portals/storage/navigation-items.storage';
import type { PortalNavigationApi } from '../../portals/types';
import { notify } from '../../../shared/notify/notify';
import type { PortalTenant } from '../types/portal-tenant.types';

interface PublishedApiRow {
    navItem: PortalNavigationApi;
    version: string;
    path: string;
    isPrivate: boolean;
}

interface TenantApiAccessTabProps {
    readonly portalId: string;
    readonly tenant: PortalTenant;
    readonly onSave: (patch: Pick<PortalTenant, 'apiAccessMode' | 'allowedApiIds'>) => Promise<void>;
}

export function TenantApiAccessTab({ portalId, tenant, onSave }: TenantApiAccessTabProps) {
    const [apiAccessMode, setApiAccessMode] = useState(tenant.apiAccessMode);
    const [allowedApiIds, setAllowedApiIds] = useState<Set<string>>(new Set(tenant.allowedApiIds));
    const [publishedApis, setPublishedApis] = useState<PublishedApiRow[]>([]);
    const [query, setQuery] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        setApiAccessMode(tenant.apiAccessMode);
        setAllowedApiIds(new Set(tenant.allowedApiIds));
    }, [tenant]);

    useEffect(() => {
        void (async () => {
            const navItems = await getNavItems(portalId);
            const apiNavItems = getPublishedApiNavItems(navItems);
            const rows = await Promise.all(
                apiNavItems.map(async navItem => {
                    const api = await getApiById(navItem.apiId);
                    return {
                        navItem,
                        version: api?.version ?? '—',
                        path: api?.entrypoints?.[0] ?? `/${navItem.slug}`,
                        isPrivate: api?._public === false,
                    };
                }),
            );
            setPublishedApis(rows);
        })();
    }, [portalId]);

    const filteredApis = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return publishedApis;
        }

        return publishedApis.filter(
            row =>
                row.navItem.title.toLowerCase().includes(normalized)
                || row.navItem.apiId.toLowerCase().includes(normalized),
        );
    }, [publishedApis, query]);

    const toggleApi = (apiId: string) => {
        setAllowedApiIds(previous => {
            const next = new Set(previous);
            if (next.has(apiId)) {
                next.delete(apiId);
            } else {
                next.add(apiId);
            }
            return next;
        });
    };

    const selectAll = () => {
        setAllowedApiIds(new Set(publishedApis.map(row => row.navItem.apiId)));
    };

    const clearAll = () => {
        setAllowedApiIds(new Set());
    };

    const handleSave = async () => {
        setIsSaving(true);
        try {
            await onSave({
                apiAccessMode,
                allowedApiIds: [...allowedApiIds],
            });
            notify.success('API access updated');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h2 className="text-lg font-semibold">API access</h2>
                <p className="text-sm text-muted-foreground">
                    Choose which APIs users in this tenant can discover and subscribe to.
                </p>
            </div>

            <fieldset className="space-y-2">
                <label className="flex items-center gap-2 text-sm">
                    <input
                        type="radio"
                        name="api-access-mode"
                        checked={apiAccessMode === 'all'}
                        onChange={() => setApiAccessMode('all')}
                    />
                    All published APIs
                </label>
                <label className="flex items-center gap-2 text-sm">
                    <input
                        type="radio"
                        name="api-access-mode"
                        checked={apiAccessMode === 'selected'}
                        onChange={() => setApiAccessMode('selected')}
                    />
                    Selected APIs only
                </label>
            </fieldset>

            {apiAccessMode === 'selected' && (
                <div className="space-y-4 rounded-lg border p-4">
                    <div className="flex flex-wrap items-center gap-3">
                        <Input
                            placeholder="Filter APIs…"
                            value={query}
                            onChange={event => setQuery(event.target.value)}
                            aria-label="Filter APIs"
                            className="max-w-sm"
                        />
                        <Button variant="outline" size="sm" onClick={selectAll}>
                            Select all
                        </Button>
                        <Button variant="outline" size="sm" onClick={clearAll}>
                            Clear
                        </Button>
                    </div>

                    <div className="space-y-2">
                        {filteredApis.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No published APIs in this portal.</p>
                        ) : (
                            filteredApis.map(row => (
                                <label
                                    key={row.navItem.id}
                                    className="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 hover:bg-muted/50"
                                >
                                    <input
                                        type="checkbox"
                                        checked={allowedApiIds.has(row.navItem.apiId)}
                                        onChange={() => toggleApi(row.navItem.apiId)}
                                    />
                                    <span className="flex-1 text-sm">
                                        <span className="font-medium">{row.navItem.title}</span>
                                        <span className="ml-2 text-muted-foreground">{row.version}</span>
                                        <span className="ml-2 text-muted-foreground">Published</span>
                                        <span className="ml-2 font-mono text-xs text-muted-foreground">{row.path}</span>
                                        {row.isPrivate && (
                                            <span className="ml-2 rounded bg-muted px-1.5 py-0.5 text-xs">Private</span>
                                        )}
                                    </span>
                                </label>
                            ))
                        )}
                    </div>
                </div>
            )}

            <div className="flex justify-end">
                <Button onClick={() => void handleSave()} disabled={isSaving}>
                    {isSaving ? 'Saving…' : 'Save changes'}
                </Button>
            </div>
        </div>
    );
}
