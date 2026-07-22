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
    Card,
    CardContent,
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
    Switch,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { usePortal } from '../hooks/usePortal';
import { usePortalIdentityProviders } from '../hooks/usePortalIdentityProviders';
import {
    emptyIdpConfiguration,
    PORTAL_IDP_TYPE_LABELS,
    PORTAL_IDP_TYPES,
    PORTAL_SETTINGS_SECTION_META,
    type PortalIdentityProvider,
    type PortalIdentityProviderInput,
    type PortalIdentityProviderType,
    type PortalIdpConfiguration,
} from '../types';

interface IdpFormState {
    type: PortalIdentityProviderType;
    name: string;
    description: string;
    enabled: boolean;
    syncMappings: boolean;
    emailRequired: boolean;
    configuration: PortalIdpConfiguration;
}

function defaultFormState(): IdpFormState {
    return {
        type: 'GRAVITEEIO_AM',
        name: '',
        description: '',
        enabled: true,
        syncMappings: false,
        emailRequired: true,
        configuration: emptyIdpConfiguration(),
    };
}

function formFromProvider(provider: PortalIdentityProvider): IdpFormState {
    return {
        type: provider.type,
        name: provider.name,
        description: provider.description,
        enabled: provider.enabled,
        syncMappings: provider.syncMappings,
        emailRequired: provider.emailRequired,
        configuration: { ...provider.configuration },
    };
}

export function IdpConfigurationPage() {
    const { portalId = '' } = useParams<{ portalId: string }>();
    const { homePath } = usePortalsNavigation();
    const { portal, loading: portalLoading, missing } = usePortal(portalId);
    const {
        providers,
        loading: providersLoading,
        addProvider,
        updateProvider,
        removeProvider,
        toggleEnabled,
    } = usePortalIdentityProviders(portalId);

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingProvider, setEditingProvider] = useState<PortalIdentityProvider | null>(null);
    const [providerToDelete, setProviderToDelete] = useState<PortalIdentityProvider | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    if (portalLoading || providersLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading identity providers…</p>;
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

    const meta = PORTAL_SETTINGS_SECTION_META['idp-configuration'];

    const openCreate = () => {
        setEditingProvider(null);
        setDialogOpen(true);
    };

    const openEdit = (provider: PortalIdentityProvider) => {
        setEditingProvider(provider);
        setDialogOpen(true);
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={openCreate}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add identity provider
                </Button>
            </div>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[48rem] border-collapse text-left text-sm">
                            <caption className="sr-only">Identity providers</caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Type
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Name
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Description
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Sync
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Available on portal
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {providers.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} className="px-5 py-10 text-center text-muted-foreground">
                                            No identity providers yet. Add Google, GitHub, Gravitee AM, or
                                            OpenID Connect.
                                        </td>
                                    </tr>
                                ) : (
                                    providers.map(provider => (
                                        <tr key={provider.id} className="border-b border-border/60 last:border-b-0">
                                            <td className="px-5 py-4 align-middle">
                                                {PORTAL_IDP_TYPE_LABELS[provider.type]}
                                            </td>
                                            <td className="px-5 py-4 align-middle font-medium">{provider.name}</td>
                                            <td className="max-w-xs px-5 py-4 align-middle text-muted-foreground">
                                                {provider.description || '—'}
                                            </td>
                                            <td className="px-5 py-4 align-middle text-muted-foreground">
                                                {provider.syncMappings ? 'Every login' : 'First login'}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Switch
                                                    checked={provider.enabled}
                                                    onCheckedChange={checked =>
                                                        void toggleEnabled(provider.id, checked === true)
                                                    }
                                                    aria-label={`${provider.enabled ? 'Disable' : 'Enable'} ${provider.name}`}
                                                />
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <Button
                                                        type="button"
                                                        variant="outline"
                                                        size="sm"
                                                        aria-label={`Edit ${provider.name}`}
                                                        onClick={() => openEdit(provider)}
                                                    >
                                                        Edit
                                                    </Button>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="sm"
                                                        aria-label={`Delete ${provider.name}`}
                                                        onClick={() => setProviderToDelete(provider)}
                                                    >
                                                        <Trash2Icon className="size-4" aria-hidden />
                                                    </Button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            <IdpConfigDialog
                open={dialogOpen}
                onOpenChange={open => {
                    setDialogOpen(open);
                    if (!open) {
                        setEditingProvider(null);
                    }
                }}
                provider={editingProvider}
                onSubmit={values => {
                    if (editingProvider) {
                        void updateProvider(editingProvider.id, {
                            name: values.name,
                            description: values.description,
                            enabled: values.enabled,
                            syncMappings: values.syncMappings,
                            emailRequired: values.emailRequired,
                            configuration: values.configuration,
                        }).then(() => {
                            notify.success('Identity provider updated.');
                        });
                    } else {
                        const input: PortalIdentityProviderInput = {
                            type: values.type,
                            name: values.name,
                            description: values.description,
                            enabled: values.enabled,
                            syncMappings: values.syncMappings,
                            emailRequired: values.emailRequired,
                            configuration: values.configuration,
                        };
                        void addProvider(input).then(() => {
                            notify.success('Identity provider added.');
                        });
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

function IdpConfigDialog({
    open,
    onOpenChange,
    provider,
    onSubmit,
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly provider: PortalIdentityProvider | null;
    readonly onSubmit: (values: IdpFormState) => void;
}) {
    const [form, setForm] = useState<IdpFormState>(defaultFormState);
    const isEdit = provider !== null;

    useEffect(() => {
        if (!open) {
            return;
        }
        setForm(provider ? formFromProvider(provider) : defaultFormState());
    }, [open, provider]);

    const updateConfiguration = (patch: Partial<PortalIdpConfiguration>) => {
        setForm(current => ({ ...current, configuration: { ...current.configuration, ...patch } }));
    };

    const canSubmit =
        form.name.trim().length >= 2 &&
        form.configuration.clientId.trim().length > 0 &&
        form.configuration.clientSecret.trim().length > 0 &&
        (form.type !== 'GRAVITEEIO_AM' ||
            (form.configuration.serverURL.trim().length > 0 && form.configuration.domain.trim().length > 0)) &&
        (form.type !== 'OIDC' ||
            (form.configuration.authorizeEndpoint.trim().length > 0 &&
                form.configuration.tokenEndpoint.trim().length > 0 &&
                form.configuration.userInfoEndpoint.trim().length > 0));

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
            <DialogContent className="flex max-h-[90vh] flex-col" style={{ width: 'min(92vw, 32rem)' }}>
                <DialogHeader>
                    <DialogTitle>{isEdit ? 'Edit identity provider' : 'Add identity provider'}</DialogTitle>
                    <DialogDescription>
                        Configure external authentication similar to Organization → Authentication in APIM
                        Console.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="idp-config-form"
                    className="min-h-0 flex-1 space-y-4 overflow-y-auto py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        handleSubmit();
                    }}
                >
                    {!isEdit && (
                        <Field>
                            <FieldLabel htmlFor="idp-type">Type</FieldLabel>
                            <select
                                id="idp-type"
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
                        <FieldLabel htmlFor="idp-name">Name</FieldLabel>
                        <Input
                            id="idp-name"
                            value={form.name}
                            onChange={event => setForm(current => ({ ...current, name: event.target.value }))}
                            placeholder="e.g. Corporate SSO"
                            required
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="idp-description">Description</FieldLabel>
                        <Input
                            id="idp-description"
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
                        Allow portal authentication to use this identity provider
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <Checkbox
                            checked={form.emailRequired}
                            onCheckedChange={checked =>
                                setForm(current => ({ ...current, emailRequired: checked === true }))
                            }
                        />
                        Email required
                    </label>

                    <label className="flex items-center gap-2 text-sm">
                        <Checkbox
                            checked={form.syncMappings}
                            onCheckedChange={checked =>
                                setForm(current => ({ ...current, syncMappings: checked === true }))
                            }
                        />
                        Sync group/role mappings on every login
                    </label>

                    <div className="space-y-3 border-t border-border/60 pt-4">
                        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                            Configuration
                        </p>
                        <Field>
                            <FieldLabel htmlFor="idp-client-id">Client ID</FieldLabel>
                            <Input
                                id="idp-client-id"
                                value={form.configuration.clientId}
                                onChange={event => updateConfiguration({ clientId: event.target.value })}
                                required
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="idp-client-secret">Client Secret</FieldLabel>
                            <Input
                                id="idp-client-secret"
                                type="password"
                                value={form.configuration.clientSecret}
                                onChange={event => updateConfiguration({ clientSecret: event.target.value })}
                                required
                            />
                        </Field>

                        {form.type === 'GRAVITEEIO_AM' && (
                            <>
                                <Field>
                                    <FieldLabel htmlFor="idp-server-url">Server URL</FieldLabel>
                                    <Input
                                        id="idp-server-url"
                                        value={form.configuration.serverURL}
                                        onChange={event => updateConfiguration({ serverURL: event.target.value })}
                                        placeholder="https://am.example.com"
                                        required
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-domain">Domain</FieldLabel>
                                    <Input
                                        id="idp-domain"
                                        value={form.configuration.domain}
                                        onChange={event => updateConfiguration({ domain: event.target.value })}
                                        required
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-scopes">Scopes</FieldLabel>
                                    <Input
                                        id="idp-scopes"
                                        value={form.configuration.scopes}
                                        onChange={event => updateConfiguration({ scopes: event.target.value })}
                                        placeholder="openid,profile,email"
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-color">Button color</FieldLabel>
                                    <Input
                                        id="idp-color"
                                        value={form.configuration.color}
                                        onChange={event => updateConfiguration({ color: event.target.value })}
                                        placeholder="#1B4F72"
                                    />
                                </Field>
                            </>
                        )}

                        {form.type === 'OIDC' && (
                            <>
                                <Field>
                                    <FieldLabel htmlFor="idp-authorize">Authorize endpoint</FieldLabel>
                                    <Input
                                        id="idp-authorize"
                                        value={form.configuration.authorizeEndpoint}
                                        onChange={event =>
                                            updateConfiguration({ authorizeEndpoint: event.target.value })
                                        }
                                        required
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-token">Token endpoint</FieldLabel>
                                    <Input
                                        id="idp-token"
                                        value={form.configuration.tokenEndpoint}
                                        onChange={event =>
                                            updateConfiguration({ tokenEndpoint: event.target.value })
                                        }
                                        required
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-userinfo">User info endpoint</FieldLabel>
                                    <Input
                                        id="idp-userinfo"
                                        value={form.configuration.userInfoEndpoint}
                                        onChange={event =>
                                            updateConfiguration({ userInfoEndpoint: event.target.value })
                                        }
                                        required
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-logout">Logout endpoint</FieldLabel>
                                    <Input
                                        id="idp-logout"
                                        value={form.configuration.userLogoutEndpoint}
                                        onChange={event =>
                                            updateConfiguration({ userLogoutEndpoint: event.target.value })
                                        }
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-introspect">Token introspection endpoint</FieldLabel>
                                    <Input
                                        id="idp-introspect"
                                        value={form.configuration.tokenIntrospectionEndpoint}
                                        onChange={event =>
                                            updateConfiguration({
                                                tokenIntrospectionEndpoint: event.target.value,
                                            })
                                        }
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-oidc-scopes">Scopes</FieldLabel>
                                    <Input
                                        id="idp-oidc-scopes"
                                        value={form.configuration.scopes}
                                        onChange={event => updateConfiguration({ scopes: event.target.value })}
                                        placeholder="openid,profile,email"
                                    />
                                </Field>
                                <Field>
                                    <FieldLabel htmlFor="idp-oidc-color">Button color</FieldLabel>
                                    <Input
                                        id="idp-oidc-color"
                                        value={form.configuration.color}
                                        onChange={event => updateConfiguration({ color: event.target.value })}
                                        placeholder="#1B4F72"
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
                    <Button type="submit" form="idp-config-form" disabled={!canSubmit}>
                        {isEdit ? 'Save' : 'Add identity provider'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
