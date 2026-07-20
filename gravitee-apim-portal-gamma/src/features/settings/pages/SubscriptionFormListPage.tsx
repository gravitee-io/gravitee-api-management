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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { ArrowRightIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { AddNamedItemDialog } from '../components/AddNamedItemDialog';
import { usePortal } from '../hooks/usePortal';
import { usePortalSubscriptionForms } from '../hooks/usePortalSubscriptionForms';
import { PORTAL_SETTINGS_SECTION_META, type SubscriptionForm } from '../types';

export function SubscriptionFormListPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const navigate = useNavigate();
    const { homePath, portalSettingsPath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading: portalLoading, missing } = usePortal(portalId);
    const { forms, loading: formsLoading, addForm, removeForm } = usePortalSubscriptionForms(portalId);

    const [addOpen, setAddOpen] = useState(false);
    const [formToDelete, setFormToDelete] = useState<SubscriptionForm | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    if (portalLoading || formsLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading subscription forms…</p>;
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

    const meta = PORTAL_SETTINGS_SECTION_META['subscription-form'];
    const formsBasePath = portalSettingsSectionPath(portal.id, 'subscription-forms');

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage one or more subscription forms for this portal. Add a tile to create an additional form.
                    </p>
                </div>
                <Button type="button" onClick={() => setAddOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Create form
                </Button>
            </div>

            <section aria-labelledby="subscription-forms-heading" className="space-y-3">
                <h2 id="subscription-forms-heading" className="text-base font-semibold tracking-tight">
                    Forms
                </h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {forms.map(form => (
                        <div key={form.id} className="relative h-full">
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="absolute right-2 top-2 z-10"
                                aria-label={`Delete ${form.name}`}
                                onClick={event => {
                                    event.preventDefault();
                                    event.stopPropagation();
                                    setFormToDelete(form);
                                }}
                            >
                                <Trash2Icon className="size-4" aria-hidden />
                            </Button>
                            <Link
                                to={`${formsBasePath}/${form.id}`}
                                className="block h-full rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                            >
                                <Card className="h-full min-h-44 transition-shadow duration-150 hover:shadow-md">
                                    <CardContent className="flex h-full min-h-44 flex-col gap-3 p-5">
                                        <h3 className="line-clamp-2 pr-8 text-sm font-semibold leading-tight">
                                            {form.name}
                                        </h3>
                                        <p className="line-clamp-3 flex-1 text-xs text-muted-foreground">
                                            {form.description || 'No description'}
                                        </p>
                                        <p className="text-xs text-muted-foreground">
                                            {form.fields.length} field{form.fields.length === 1 ? '' : 's'} ·{' '}
                                            {form.mappedApis.length} API
                                            {form.mappedApis.length === 1 ? '' : 's'}
                                        </p>
                                        <p className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground">
                                            Configure
                                            <ArrowRightIcon className="size-3" aria-hidden />
                                        </p>
                                    </CardContent>
                                </Card>
                            </Link>
                        </div>
                    ))}

                    <button
                        type="button"
                        onClick={() => setAddOpen(true)}
                        className="h-full w-full cursor-pointer rounded-xl text-left outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    >
                        <Card className="h-full min-h-44 border-dashed bg-muted/30 transition-shadow duration-150 hover:shadow-md">
                            <CardContent className="flex h-full min-h-44 flex-col gap-3 p-5">
                                <h3 className="line-clamp-2 text-sm font-semibold leading-tight">
                                    Add Subscription Form
                                </h3>
                                <p className="line-clamp-3 flex-1 text-xs text-muted-foreground">
                                    Create another subscription form tile for this portal.
                                </p>
                                <p className="mt-auto flex items-center gap-1 text-xs font-medium text-muted-foreground">
                                    <PlusIcon className="size-3" aria-hidden />
                                    Add tile
                                </p>
                            </CardContent>
                        </Card>
                    </button>
                </div>
            </section>

            <Button variant="outline" asChild>
                <Link to={portalSettingsPath(portal.id)}>Back to {portal.name}</Link>
            </Button>

            <AddNamedItemDialog
                open={addOpen}
                onOpenChange={setAddOpen}
                title="Add Subscription Form"
                description="Create another subscription form for multi-form setups on this portal."
                namePlaceholder="e.g. Enterprise Subscription Form"
                submitLabel="Add form"
                onAdd={({ name, description }) => {
                    void addForm({ name, description }).then(created => {
                        if (!created) {
                            return;
                        }
                        notify.success('Subscription form created.');
                        navigate(`${formsBasePath}/${created.id}`);
                    });
                }}
            />

            <ConfirmDialog
                open={formToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setFormToDelete(null);
                    }
                }}
                title="Delete Subscription Form?"
                description={
                    formToDelete
                        ? `This will permanently delete "${formToDelete.name}". This action cannot be undone.`
                        : undefined
                }
                confirmLabel="Delete"
                pendingLabel="Deleting"
                destructive
                isPending={isDeleting}
                onConfirm={() => {
                    if (!formToDelete) {
                        return;
                    }
                    setIsDeleting(true);
                    void removeForm(formToDelete.id)
                        .then(() => {
                            notify.success('Subscription form deleted.');
                            setFormToDelete(null);
                        })
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}
