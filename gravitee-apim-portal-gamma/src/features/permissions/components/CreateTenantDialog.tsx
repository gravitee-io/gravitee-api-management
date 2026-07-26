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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import {
    PORTAL_TENANT_MANAGEMENT_MODE_LABELS,
    type PortalTenantManagementMode,
} from '../../tenants/types/portal-tenant.types';
import type { CreateTenantValues } from '../hooks/usePermissionsDirectory';

const MANAGEMENT_MODES: readonly PortalTenantManagementMode[] = ['DELEGATED', 'CENTRAL', 'SELF_MANAGED'];

interface CreateTenantDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (values: CreateTenantValues) => void;
}

export function CreateTenantDialog({ open, onOpenChange, onSubmit }: CreateTenantDialogProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [managementMode, setManagementMode] = useState<PortalTenantManagementMode>('DELEGATED');

    useEffect(() => {
        if (open) {
            setName('');
            setDescription('');
            setManagementMode('DELEGATED');
        }
    }, [open]);

    const canSubmit = name.trim().length >= 2;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create tenant</DialogTitle>
                    <DialogDescription>
                        Tenants are environment-level: their groups and grants apply across every developer
                        portal.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="create-tenant-form"
                    className="space-y-4"
                    onSubmit={event => {
                        event.preventDefault();
                        if (!canSubmit) {
                            return;
                        }
                        onSubmit({ name: name.trim(), description: description.trim(), managementMode });
                        onOpenChange(false);
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="tenant-name">Name</FieldLabel>
                        <Input
                            id="tenant-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="e.g. Nordics Partners"
                            required
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="tenant-description">Description</FieldLabel>
                        <Input
                            id="tenant-description"
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                            placeholder="Optional description"
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="tenant-management-mode">Administration</FieldLabel>
                        <Select
                            value={managementMode}
                            onValueChange={value => setManagementMode(value as PortalTenantManagementMode)}
                        >
                            <SelectTrigger id="tenant-management-mode">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {MANAGEMENT_MODES.map(mode => (
                                    <SelectItem key={mode} value={mode}>
                                        {PORTAL_TENANT_MANAGEMENT_MODE_LABELS[mode].long}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </Field>
                </form>

                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="submit" form="create-tenant-form" disabled={!canSubmit}>
                        Create
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
