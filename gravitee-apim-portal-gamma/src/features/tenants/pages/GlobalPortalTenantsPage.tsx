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
import { Button, Input, Skeleton } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { useGlobalPortalTenants } from '../hooks/useGlobalPortalTenants';
import { usePortalsNavigation } from '../../portals/config/navigation';

export function GlobalPortalTenantsPage() {
    const navigate = useNavigate();
    const { homePath, portalTenantDetailPath, portalTenantsPath, portalViewPath } = usePortalsNavigation();
    const { tenants, loading } = useGlobalPortalTenants();
    const [query, setQuery] = useState('');

    const filteredTenants = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return tenants;
        }

        return tenants.filter(
            tenant =>
                tenant.name.toLowerCase().includes(normalized)
                || tenant.hrid.toLowerCase().includes(normalized)
                || tenant.portalName.toLowerCase().includes(normalized),
        );
    }, [query, tenants]);

    const openPreview = (portalId: string, tenantId: string) => {
        navigate(portalViewPath(portalId, { asTenant: tenantId }));
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Tenants</h1>
                <p className="max-w-2xl text-sm text-muted-foreground">
                    All tenants across your developer portals. Each tenant isolates users, API access, and portal
                    features for a downstream customer.
                </p>
            </div>

            {loading ? (
                <div className="space-y-3" aria-busy="true">
                    <Skeleton className="h-10 w-full max-w-sm" />
                    <Skeleton className="h-48 w-full" />
                </div>
            ) : tenants.length === 0 ? (
                <div className="rounded-lg border border-dashed px-6 py-16 text-center">
                    <h2 className="text-lg font-semibold">No tenants yet</h2>
                    <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">
                        Open a portal from the dashboard and create tenants to segregate your customers.
                    </p>
                    <Button className="mt-6" asChild>
                        <Link to={homePath}>Go to portals</Link>
                    </Button>
                </div>
            ) : (
                <>
                    <Input
                        placeholder="Search tenants or portals…"
                        value={query}
                        onChange={event => setQuery(event.target.value)}
                        aria-label="Search tenants"
                        className="max-w-sm"
                    />

                    <div className="overflow-hidden rounded-lg border">
                        <table className="w-full text-sm">
                            <thead className="bg-muted/40 text-left">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Tenant</th>
                                    <th className="px-4 py-3 font-medium">Portal</th>
                                    <th className="px-4 py-3 font-medium">Users</th>
                                    <th className="px-4 py-3 font-medium">APIs</th>
                                    <th className="px-4 py-3 font-medium">Apps</th>
                                    <th className="px-4 py-3 font-medium">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredTenants.map(tenant => (
                                    <tr key={tenant.id} className="border-t">
                                        <td className="px-4 py-3">
                                            <Link
                                                to={portalTenantDetailPath(tenant.portalId, tenant.id)}
                                                className="font-medium hover:underline"
                                            >
                                                {tenant.name}
                                            </Link>
                                            {tenant.description && (
                                                <p className="text-muted-foreground">{tenant.description}</p>
                                            )}
                                        </td>
                                        <td className="px-4 py-3">
                                            <Link
                                                to={portalTenantsPath(tenant.portalId)}
                                                className="hover:underline"
                                            >
                                                {tenant.portalName}
                                            </Link>
                                        </td>
                                        <td className="px-4 py-3">{tenant.userCount}</td>
                                        <td className="px-4 py-3">{tenant.apiCount}</td>
                                        <td className="px-4 py-3">{tenant.appCount}</td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <Button variant="link" className="h-auto p-0" asChild>
                                                    <Link to={portalTenantDetailPath(tenant.portalId, tenant.id)}>
                                                        Manage
                                                    </Link>
                                                </Button>
                                                <span className="text-muted-foreground">·</span>
                                                <Button
                                                    variant="link"
                                                    className="h-auto p-0"
                                                    onClick={() => openPreview(tenant.portalId, tenant.id)}
                                                >
                                                    Preview
                                                </Button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </div>
    );
}
