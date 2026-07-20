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
import { Button, Card, CardContent, Switch } from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { AddNamedItemDialog } from '../components/AddNamedItemDialog';
import { MapApisDialog } from '../components/ApiMappingPanel';
import { usePortal } from '../hooks/usePortal';
import { usePortalCategories } from '../hooks/usePortalCategories';
import { PORTAL_SETTINGS_SECTION_META } from '../types';
import type { MappedApi, PortalCategory } from '../types';

export function CategoriesPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { homePath, portalSettingsPath } = usePortalsNavigation();
    const { portal, loading: portalLoading, missing } = usePortal(portalId);
    const {
        categories,
        loading: categoriesLoading,
        addCategory,
        removeCategory,
        toggleEnabled,
        updateMappedApis,
    } = usePortalCategories(portalId);

    const [addOpen, setAddOpen] = useState(false);
    const [categoryToDelete, setCategoryToDelete] = useState<PortalCategory | null>(null);
    const [categoryToMap, setCategoryToMap] = useState<PortalCategory | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    if (portalLoading || categoriesLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading categories…</p>;
    }

    if (missing || !portal) {
        return (
            <NotFoundPage
                homePath={homePath}
                homeLabel="Back to portals"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
            />
        );
    }

    const meta = PORTAL_SETTINGS_SECTION_META.categories;

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={() => setAddOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Create category
                </Button>
            </div>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[36rem] border-collapse text-left text-sm">
                            <caption className="sr-only">Portal categories</caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Name
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Description
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actions
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Map APIs
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {categories.length === 0 ? (
                                    <tr>
                                        <td colSpan={4} className="px-5 py-10 text-center text-muted-foreground">
                                            No categories yet. Create a category to organize APIs in the catalog.
                                        </td>
                                    </tr>
                                ) : (
                                    categories.map(category => (
                                        <tr key={category.id} className="border-b border-border/60 last:border-b-0">
                                            <td className="px-5 py-4 align-middle font-medium">{category.name}</td>
                                            <td className="max-w-md px-5 py-4 align-middle text-muted-foreground">
                                                {category.description || '—'}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <div className="flex flex-wrap items-center gap-3">
                                                    <Switch
                                                        checked={category.enabled}
                                                        onCheckedChange={checked =>
                                                            void toggleEnabled(category.id, checked === true)
                                                        }
                                                        aria-label={`${category.enabled ? 'Disable' : 'Enable'} ${category.name}`}
                                                    />
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="sm"
                                                        aria-label={`Delete ${category.name}`}
                                                        onClick={() => setCategoryToDelete(category)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                </div>
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Button
                                                    type="button"
                                                    variant="outline"
                                                    size="sm"
                                                    onClick={() => setCategoryToMap(category)}
                                                >
                                                    Map APIs
                                                    {category.mappedApis.length > 0
                                                        ? ` (${category.mappedApis.length})`
                                                        : ''}
                                                </Button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            <Button variant="outline" asChild>
                <Link to={portalSettingsPath(portal.id)}>Back to {portal.name}</Link>
            </Button>

            <AddNamedItemDialog
                open={addOpen}
                onOpenChange={setAddOpen}
                title="Create category"
                description="Add a category with a name and description, then map APIs to it."
                namePlaceholder="e.g. Payments"
                submitLabel="Create category"
                onAdd={({ name, description }) => {
                    void addCategory({ name, description }).then(() => {
                        notify.success('Category created.');
                    });
                }}
            />

            <MapApisDialog
                open={categoryToMap !== null}
                onOpenChange={open => {
                    if (!open) {
                        setCategoryToMap(null);
                    }
                }}
                mappedApis={categoryToMap?.mappedApis ?? []}
                description="Search for APIs and select which ones belong to this category."
                onChange={(mappedApis: readonly MappedApi[]) => {
                    if (!categoryToMap) {
                        return;
                    }
                    void updateMappedApis(categoryToMap.id, mappedApis).then(() => {
                        notify.success('API mapping updated.');
                        setCategoryToMap(null);
                    });
                }}
            />

            <ConfirmDialog
                open={categoryToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setCategoryToDelete(null);
                    }
                }}
                title="Delete category?"
                description={
                    categoryToDelete
                        ? `This will permanently delete "${categoryToDelete.name}". This action cannot be undone.`
                        : undefined
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={isDeleting}
                onConfirm={() => {
                    if (!categoryToDelete) {
                        return;
                    }
                    setIsDeleting(true);
                    void removeCategory(categoryToDelete.id)
                        .then(() => {
                            notify.success('Category deleted.');
                            setCategoryToDelete(null);
                        })
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}
