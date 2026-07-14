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
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Input,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, MoreHorizontalIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { getPortal } from '../../portals/storage/portals.storage';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { CreatePortalTenantDialog } from '../components/CreatePortalTenantDialog';
import { DeletePortalTenantDialog } from '../components/DeletePortalTenantDialog';
import { usePortalTenants } from '../hooks/usePortalTenants';
import { DEFAULT_PORTAL_TENANT_FEATURES } from '../types/portal-tenant.types';
import { createTenantId } from '../utils/tenant-hrid';

export function PortalTenantsPage() {
    const { portalId } = useParams<{ portalId: string }>();
    const navigate = useNavigate();
    const { homePath, portalTenantDetailPath } = usePortalsNavigation();
    const { tenants, loading, createTenant, deleteTenant } = usePortalTenants(portalId);
    const [portalName, setPortalName] = useState<string>('');
    const [portalMissing, setPortalMissing] = useState(false);
    const [query, setQuery] = useState('');
    const [createOpen, setCreateOpen] = useState(false);
    const [isCreating, setIsCreating] = useState(false);
    const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => {
        if (!portalId) {
            return;
        }

        void getPortal(portalId).then(portal => {
            if (!portal) {
                setPortalMissing(true);
                return;
            }
            setPortalName(portal.name);
        });
    }, [portalId]);

    const filteredTenants = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return tenants;
        }

        return tenants.filter(
            tenant =>
                tenant.name.toLowerCase().includes(normalized)
                || tenant.hrid.toLowerCase().includes(normalized),
        );
    }, [query, tenants]);

    const deleteTarget = tenants.find(tenant => tenant.id === deleteTargetId) ?? null;

    const handleCreate = async (input: { name: string; description?: string; hrid: string }) => {
        if (!portalId) {
            return;
        }

        setIsCreating(true);
        try {
            const now = new Date().toISOString();
            const tenant = await createTenant({
                id: createTenantId(),
                portalId,
                name: input.name,
                hrid: input.hrid,
                description: input.description,
                allowedApiIds: [],
                apiAccessMode: 'all',
                features: DEFAULT_PORTAL_TENANT_FEATURES,
                createdAt: now,
                updatedAt: now,
            });
            setCreateOpen(false);
            navigate(portalTenantDetailPath(portalId, tenant.id));
        } finally {
            setIsCreating(false);
        }
    };

    if (portalMissing) {
        return (
            <NotFoundPage
                homePath={homePath}
                homeLabel="Back to dashboards"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
                className="min-h-screen"
            />
        );
    }

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Link to={homePath} className="inline-flex items-center gap-1 hover:text-foreground">
                    <ArrowLeftIcon className="size-4" aria-hidden="true" />
                    Developer Portals
                </Link>
                <span aria-hidden="true">/</span>
                <span className="text-foreground">{portalName || '…'}</span>
            </div>

            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">Tenants</h1>
                    <p className="text-sm text-muted-foreground">
                        Segregate portal users into isolated groups. Each tenant shares access to specific APIs,
                        applications, and portal features.
                    </p>
                </div>
                <Button onClick={() => setCreateOpen(true)}>+ Create tenant</Button>
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
                        Create tenants to give each customer their own slice of this portal.
                    </p>
                    <Button className="mt-6" onClick={() => setCreateOpen(true)}>
                        + Create tenant
                    </Button>
                </div>
            ) : (
                <>
                    <Input
                        placeholder="Search tenants…"
                        value={query}
                        onChange={event => setQuery(event.target.value)}
                        aria-label="Search tenants"
                        className="max-w-sm"
                    />

                    <div className="overflow-hidden rounded-lg border">
                        <table className="w-full text-sm">
                            <thead className="bg-muted/40 text-left">
                                <tr>
                                    <th className="px-4 py-3 font-medium">Name</th>
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
                                                to={portalTenantDetailPath(portalId ?? '', tenant.id)}
                                                className="font-medium hover:underline"
                                            >
                                                {tenant.name}
                                            </Link>
                                            {tenant.description && (
                                                <p className="text-muted-foreground">{tenant.description}</p>
                                            )}
                                        </td>
                                        <td className="px-4 py-3">{tenant.userCount}</td>
                                        <td className="px-4 py-3">{tenant.apiCount}</td>
                                        <td className="px-4 py-3">{tenant.appCount}</td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <Button variant="link" className="h-auto p-0" asChild>
                                                    <Link to={portalTenantDetailPath(portalId ?? '', tenant.id)}>
                                                        Manage
                                                    </Link>
                                                </Button>
                                                <DropdownMenu>
                                                    <DropdownMenuTrigger asChild>
                                                        <Button variant="ghost" size="icon" className="size-8">
                                                            <MoreHorizontalIcon className="size-4" aria-hidden="true" />
                                                            <span className="sr-only">More actions</span>
                                                        </Button>
                                                    </DropdownMenuTrigger>
                                                    <DropdownMenuContent align="end">
                                                        <DropdownMenuItem
                                                            className="text-destructive"
                                                            onClick={() => setDeleteTargetId(tenant.id)}
                                                        >
                                                            Delete
                                                        </DropdownMenuItem>
                                                    </DropdownMenuContent>
                                                </DropdownMenu>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <CreatePortalTenantDialog
                open={createOpen}
                isPending={isCreating}
                onOpenChange={setCreateOpen}
                onCreate={input => void handleCreate(input)}
            />

            <DeletePortalTenantDialog
                tenant={deleteTarget}
                open={deleteTarget !== null}
                isPending={isDeleting}
                onOpenChange={open => {
                    if (!open) {
                        setDeleteTargetId(null);
                    }
                }}
                onConfirm={() => {
                    if (!deleteTargetId) {
                        return;
                    }

                    setIsDeleting(true);
                    void deleteTenant(deleteTargetId)
                        .then(() => setDeleteTargetId(null))
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}
