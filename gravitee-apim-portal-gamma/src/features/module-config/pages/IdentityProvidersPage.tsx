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
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import { usePortals } from '../../portals/hooks/usePortals';
import { IdentityProvidersOnboardingBanner } from '../components/IdentityProvidersOnboardingBanner';
import { IdentityProvidersSummaryCards } from '../components/IdentityProvidersSummaryCards';
import { IdentityProvidersTable } from '../components/IdentityProvidersTable';
import { useTransversalIdentityProviders } from '../hooks/useTransversalIdentityProviders';
import {
    emptyIdpConfiguration,
    MODULE_CONFIG_SECTION_META,
    PORTAL_IDP_TYPE_LABELS,
    PORTAL_IDP_TYPES,
    type PortalIdentityProviderType,
    type PortalIdpConfiguration,
    type TransversalIdentityProvider,
    type TransversalIdentityProviderInput,
} from '../types';

interface IdpFormState {
    type: PortalIdentityProviderType;
    name: string;
    description: string;
    enabled: boolean;
    syncMappings: boolean;
    emailRequired: boolean;
    configuration: PortalIdpConfiguration;
    portalIds: string[];
}

function defaultFormState(defaultType: PortalIdentityProviderType = 'GRAVITEEIO_AM'): IdpFormState {
    return {
        type: defaultType,
        name: '',
        description: '',
        enabled: true,
        syncMappings: false,
        emailRequired: true,
        configuration: emptyIdpConfiguration(),
        portalIds: [],
    };
}

function formFromProvider(provider: TransversalIdentityProvider): IdpFormState {
    return {
        type: provider.type,
        name: provider.name,
        description: provider.description,
        enabled: provider.enabled,
        syncMappings: provider.syncMappings,
        emailRequired: provider.emailRequired,
        configuration: { ...provider.configuration },
        portalIds: [...provider.portalIds],
    };
}

export function IdentityProvidersPage() {
    const { portals, loading: portalsLoading } = usePortals();
    const {
        providers,
        loading: providersLoading,
        addProvider,
        updateProvider,
        removeProvider,
        toggleEnabled,
    } = useTransversalIdentityProviders();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [createDefaultType, setCreateDefaultType] = useState<PortalIdentityProviderType>('GRAVITEEIO_AM');
    const [editingProvider, setEditingProvider] = useState<TransversalIdentityProvider | null>(null);
    const [providerToDelete, setProviderToDelete] = useState<TransversalIdentityProvider | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const portalNameById = useMemo(() => {
        const map = new Map<string, string>();
        for (const portal of portals) {
            map.set(portal.id, portal.name);
        }
        return map;
    }, [portals]);

    const openCreateDialog = (defaultType: PortalIdentityProviderType = 'GRAVITEEIO_AM') => {
        setEditingProvider(null);
        setCreateDefaultType(defaultType);
        setDialogOpen(true);
    };

    if (portalsLoading || providersLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading identity providers…</p>;
    }

    const meta = MODULE_CONFIG_SECTION_META['identity-providers'];

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={() => openCreateDialog()}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add Identity Provider
                </Button>
            </div>

            <IdentityProvidersOnboardingBanner onGetStarted={openCreateDialog} />

            <IdentityProvidersSummaryCards totalProviders={providers.length} />

            <IdentityProvidersTable
                providers={providers}
                portalNameById={portalNameById}
                onEdit={provider => {
                    setEditingProvider(provider);
                    setDialogOpen(true);
                }}
                onToggleEnabled={provider => {
                    void toggleEnabled(provider.id, !provider.enabled).then(() =>
                        notify.success(
                            provider.enabled
                                ? 'Identity provider disabled.'
                                : 'Identity provider enabled.',
                        ),
                    );
                }}
                onDelete={setProviderToDelete}
            />

            <TransversalIdpDialog
                open={dialogOpen}
                onOpenChange={open => {
                    setDialogOpen(open);
                    if (!open) {
                        setEditingProvider(null);
                    }
                }}
                provider={editingProvider}
                defaultType={createDefaultType}
                portals={portals.map(portal => ({ id: portal.id, name: portal.name }))}
                onSubmit={values => {
                    if (editingProvider) {
                        void updateProvider(editingProvider.id, {
                            name: values.name,
                            description: values.description,
                            enabled: values.enabled,
                            syncMappings: values.syncMappings,
                            emailRequired: values.emailRequired,
                            configuration: values.configuration,
                            portalIds: values.portalIds,
                        }).then(() => notify.success('Identity provider updated.'));
                    } else {
                        const input: TransversalIdentityProviderInput = {
                            type: values.type,
                            name: values.name,
                            description: values.description,
                            enabled: values.enabled,
                            syncMappings: values.syncMappings,
                            emailRequired: values.emailRequired,
                            configuration: values.configuration,
                            portalIds: values.portalIds,
                        };
                        void addProvider(input).then(() => notify.success('Identity provider added.'));
                    }
                }}
            />

            <ConfirmDialog
                open={providerToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setProviderToDelete(null);
                    }
                }}
                title="Delete identity provider?"
                description={
                    providerToDelete
                        ? `This will permanently delete "${providerToDelete.name}". This action cannot be undone.`
                        : undefined
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={isDeleting}
                onConfirm={() => {
                    if (!providerToDelete) {
                        return;
                    }
                    setIsDeleting(true);
                    void removeProvider(providerToDelete.id)
                        .then(() => {
                            notify.success('Identity provider deleted.');
                            setProviderToDelete(null);
                        })
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}

function TransversalIdpDialog({
    open,
    onOpenChange,
    provider,
    defaultType,
    portals,
    onSubmit,
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly provider: TransversalIdentityProvider | null;
    readonly defaultType: PortalIdentityProviderType;
    readonly portals: readonly { id: string; name: string }[];
    readonly onSubmit: (values: IdpFormState) => void;
}) {
    const [form, setForm] = useState<IdpFormState>(() => defaultFormState(defaultType));
    const isEdit = provider !== null;

    useEffect(() => {
        if (!open) {
            return;
        }
        setForm(provider ? formFromProvider(provider) : defaultFormState(defaultType));
    }, [open, provider, defaultType]);

    const updateConfiguration = (patch: Partial<PortalIdpConfiguration>) => {
        setForm(current => ({ ...current, configuration: { ...current.configuration, ...patch } }));
    };

    const togglePortal = (portalId: string, checked: boolean) => {
        setForm(current => ({
            ...current,
            portalIds: checked
                ? [...current.portalIds, portalId]
                : current.portalIds.filter(id => id !== portalId),
        }));
    };

    const canSubmit =
        form.name.trim().length >= 2 &&
        form.configuration.clientId.trim().length > 0 &&
        form.configuration.clientSecret.trim().length > 0;

    const handleSubmit = () => {
        if (!canSubmit) {
            return;
        }
        onSubmit({
            ...form,
            name: form.name.trim(),
            description: form.description.trim(),
        });
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className="flex max-h-[min(90vh,56rem)] flex-col overflow-hidden"
                style={{ width: 'min(92vw, 42rem)', maxWidth: 'min(92vw, 42rem)' }}
            >
                <DialogHeader>
                    <DialogTitle>{isEdit ? 'Edit identity provider' : 'Add identity provider'}</DialogTitle>
                    <DialogDescription>
                        Configure a transversal provider and assign it to one or more portals.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="transversal-idp-form"
                    className="min-h-0 flex-1 space-y-4 overflow-x-hidden overflow-y-auto py-2 pr-1"
                    onSubmit={event => {
                        event.preventDefault();
                        handleSubmit();
                    }}
                >
                    {!isEdit && (
                        <Field>
                            <FieldLabel htmlFor="tidp-type">Type</FieldLabel>
                            <select
                                id="tidp-type"
                                value={form.type}
                                onChange={event =>
                                    setForm(current => ({
                                        ...current,
                                        type: event.target.value as PortalIdentityProviderType,
                                    }))
                                }
                                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                            >
                                {PORTAL_IDP_TYPES.map(type => (
                                    <option key={type} value={type}>
                                        {PORTAL_IDP_TYPE_LABELS[type]}
                                    </option>
                                ))}
                            </select>
                        </Field>
                    )}

                    <Field>
                        <FieldLabel htmlFor="tidp-name">Name</FieldLabel>
                        <Input
                            id="tidp-name"
                            value={form.name}
                            onChange={event => setForm(current => ({ ...current, name: event.target.value }))}
                            placeholder="e.g. Corporate SSO"
                            required
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="tidp-description">Description</FieldLabel>
                        <Input
                            id="tidp-description"
                            value={form.description}
                            onChange={event =>
                                setForm(current => ({ ...current, description: event.target.value }))
                            }
                            placeholder="Optional description"
                        />
                    </Field>

                    <label className="flex items-center gap-2 text-sm">
                        <Checkbox
                            checked={form.enabled}
                            onCheckedChange={checked =>
                                setForm(current => ({ ...current, enabled: checked === true }))
                            }
                        />
                        Enabled
                    </label>

                    <div className="space-y-2">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            Assign to portals
                        </p>
                        {portals.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No portals available yet.</p>
                        ) : (
                            portals.map(portal => (
                                <label key={portal.id} className="flex items-center gap-2 text-sm">
                                    <Checkbox
                                        checked={form.portalIds.includes(portal.id)}
                                        onCheckedChange={checked => togglePortal(portal.id, checked === true)}
                                    />
                                    {portal.name}
                                </label>
                            ))
                        )}
                    </div>

                    <div className="space-y-3 border-t border-border/60 pt-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            Configuration
                        </p>
                        <Field>
                            <FieldLabel htmlFor="tidp-client-id">Client ID</FieldLabel>
                            <Input
                                id="tidp-client-id"
                                value={form.configuration.clientId}
                                onChange={event => updateConfiguration({ clientId: event.target.value })}
                                required
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="tidp-client-secret">Client secret</FieldLabel>
                            <Input
                                id="tidp-client-secret"
                                type="password"
                                value={form.configuration.clientSecret}
                                onChange={event => updateConfiguration({ clientSecret: event.target.value })}
                                required
                            />
                        </Field>
                        {(form.type === 'GRAVITEEIO_AM' || form.type === 'OIDC') && (
                            <>
                                <Field>
                                    <FieldLabel htmlFor="tidp-server-url">Server URL</FieldLabel>
                                    <Input
                                        id="tidp-server-url"
                                        value={form.configuration.serverURL}
                                        onChange={event => updateConfiguration({ serverURL: event.target.value })}
                                        placeholder="https://am.example.com"
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="tidp-domain">Domain</FieldLabel>
                                    <Input
                                        id="tidp-domain"
                                        value={form.configuration.domain}
                                        onChange={event => updateConfiguration({ domain: event.target.value })}
                                    />
                                </Field>
                            </>
                        )}
                    </div>
                </form>

                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="submit" form="transversal-idp-form" disabled={!canSubmit}>
                        {isEdit ? 'Save' : 'Add'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
