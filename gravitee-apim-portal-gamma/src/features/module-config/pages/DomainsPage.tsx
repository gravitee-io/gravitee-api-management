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
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import { usePortals } from '../../portals/hooks/usePortals';
import { usePortalDomains } from '../hooks/usePortalDomains';
import { DOMAIN_STATUS_LABELS, MODULE_CONFIG_SECTION_META, type PortalDomain } from '../types';

function statusBadgeClass(status: PortalDomain['status']): string {
    if (status === 'Active') {
        return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400';
    }
    if (status === 'Failed') {
        return 'bg-destructive/15 text-destructive';
    }
    return 'bg-muted text-muted-foreground';
}

export function DomainsPage() {
    const { portals, loading: portalsLoading } = usePortals();
    const { domains, loading: domainsLoading, addDomain, removeDomain } = usePortalDomains();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [domainToDelete, setDomainToDelete] = useState<PortalDomain | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const portalNameById = useMemo(() => {
        const map = new Map<string, string>();
        for (const portal of portals) {
            map.set(portal.id, portal.name);
        }
        return map;
    }, [portals]);

    if (portalsLoading || domainsLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading domains…</p>;
    }

    const meta = MODULE_CONFIG_SECTION_META.domains;

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">{meta.title}</h1>
                    <p className="text-sm text-muted-foreground">{meta.description}</p>
                </div>
                <Button type="button" onClick={() => setDialogOpen(true)} disabled={portals.length === 0}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add domain
                </Button>
            </div>

            <Card>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[40rem] border-collapse text-left text-sm">
                            <caption className="sr-only">Custom portal domains</caption>
                            <thead className="border-b border-border/70 bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
                                <tr>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Hostname
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Portal
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Status
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Primary
                                    </th>
                                    <th scope="col" className="px-5 py-3 font-medium">
                                        Actions
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {domains.length === 0 ? (
                                    <tr>
                                        <td colSpan={5} className="px-5 py-10 text-center text-muted-foreground">
                                            No custom domains yet. Add a hostname and link it to a portal.
                                        </td>
                                    </tr>
                                ) : (
                                    domains.map(domain => (
                                        <tr key={domain.id} className="border-b border-border/60 last:border-b-0">
                                            <td className="px-5 py-4 align-middle font-medium">{domain.hostname}</td>
                                            <td className="px-5 py-4 align-middle text-muted-foreground">
                                                {portalNameById.get(domain.portalId) ?? domain.portalId}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <span
                                                    className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${statusBadgeClass(domain.status)}`}
                                                >
                                                    {DOMAIN_STATUS_LABELS[domain.status]}
                                                </span>
                                            </td>
                                            <td className="px-5 py-4 align-middle text-muted-foreground">
                                                {domain.primary ? 'Yes' : '—'}
                                            </td>
                                            <td className="px-5 py-4 align-middle">
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="sm"
                                                    aria-label={`Delete ${domain.hostname}`}
                                                    onClick={() => setDomainToDelete(domain)}
                                                >
                                                    <Trash2Icon className="size-4" aria-hidden />
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

            <AddDomainDialog
                open={dialogOpen}
                onOpenChange={setDialogOpen}
                portals={portals.map(portal => ({ id: portal.id, name: portal.name }))}
                onAdd={input => {
                    void addDomain(input).then(() => notify.success('Domain added.'));
                }}
            />

            <ConfirmDialog
                open={domainToDelete !== null}
                onOpenChange={open => {
                    if (!open) {
                        setDomainToDelete(null);
                    }
                }}
                title="Delete domain?"
                description={
                    domainToDelete
                        ? `This will permanently remove "${domainToDelete.hostname}".`
                        : undefined
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={isDeleting}
                onConfirm={() => {
                    if (!domainToDelete) {
                        return;
                    }
                    setIsDeleting(true);
                    void removeDomain(domainToDelete.id)
                        .then(() => {
                            notify.success('Domain deleted.');
                            setDomainToDelete(null);
                        })
                        .finally(() => setIsDeleting(false));
                }}
            />
        </div>
    );
}

function AddDomainDialog({
    open,
    onOpenChange,
    portals,
    onAdd,
}: {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly portals: readonly { id: string; name: string }[];
    readonly onAdd: (input: { hostname: string; portalId: string; primary: boolean }) => void;
}) {
    const [hostname, setHostname] = useState('');
    const [portalId, setPortalId] = useState('');
    const [primary, setPrimary] = useState(false);

    useEffect(() => {
        if (!open) {
            setHostname('');
            setPortalId(portals[0]?.id ?? '');
            setPrimary(false);
            return;
        }
        setPortalId(current => current || portals[0]?.id || '');
    }, [open, portals]);

    const canSubmit = hostname.trim().length > 0 && portalId.length > 0;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 28rem)' }}>
                <DialogHeader>
                    <DialogTitle>Add custom domain</DialogTitle>
                    <DialogDescription>
                        Point a hostname at a portal. DNS and SSL verification are mocked in this POC.
                    </DialogDescription>
                </DialogHeader>

                <form
                    className="space-y-4 py-2"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit) {
                            return;
                        }
                        onAdd({
                            hostname: hostname.trim().toLowerCase(),
                            portalId,
                            primary,
                        });
                        onOpenChange(false);
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="domain-hostname">Hostname</FieldLabel>
                        <Input
                            id="domain-hostname"
                            value={hostname}
                            onChange={event => setHostname(event.target.value)}
                            placeholder="developers.example.com"
                            autoFocus
                            required
                        />
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="domain-portal">Portal</FieldLabel>
                        <select
                            id="domain-portal"
                            value={portalId}
                            onChange={event => setPortalId(event.target.value)}
                            className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                            required
                        >
                            {portals.map(portal => (
                                <option key={portal.id} value={portal.id}>
                                    {portal.name}
                                </option>
                            ))}
                        </select>
                    </Field>
                    <label className="flex items-center gap-2 text-sm">
                        <Checkbox
                            checked={primary}
                            onCheckedChange={checked => setPrimary(checked === true)}
                        />
                        Set as primary domain for this portal
                    </label>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!canSubmit}>
                            Add domain
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
