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

import type { CreateGroupValues } from '../hooks/usePermissionsDirectory';
import {
    PORTAL_GROUP_MANAGEMENT_MODE_LABELS,
    type PortalGroupManagementMode,
} from '../types/permissions.types';

const MANAGEMENT_MODES: readonly PortalGroupManagementMode[] = ['SELF_MANAGED', 'PLATFORM_MANAGED'];

interface CreateGroupDialogProps {
    readonly open: boolean;
    readonly tenantName: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (values: CreateGroupValues) => void;
}

export function CreateGroupDialog({
    open,
    tenantName,
    onOpenChange,
    onSubmit,
}: CreateGroupDialogProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [managementMode, setManagementMode] = useState<PortalGroupManagementMode>('SELF_MANAGED');

    useEffect(() => {
        if (open) {
            setName('');
            setDescription('');
            setManagementMode('SELF_MANAGED');
        }
    }, [open]);

    const canSubmit = name.trim().length >= 2;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create group in {tenantName}</DialogTitle>
                    <DialogDescription>
                        Groups bundle tenant members so access to assets and portal content can be granted once.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="create-group-form"
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
                        <FieldLabel htmlFor="group-name">Name</FieldLabel>
                        <Input
                            id="group-name"
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="e.g. mobile-devs"
                            required
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="group-description">Description</FieldLabel>
                        <Input
                            id="group-description"
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                            placeholder="Optional description"
                        />
                    </Field>

                    <Field>
                        <FieldLabel htmlFor="group-management-mode">Administration</FieldLabel>
                        <Select
                            value={managementMode}
                            onValueChange={value => setManagementMode(value as PortalGroupManagementMode)}
                        >
                            <SelectTrigger id="group-management-mode">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {MANAGEMENT_MODES.map(mode => (
                                    <SelectItem key={mode} value={mode}>
                                        {PORTAL_GROUP_MANAGEMENT_MODE_LABELS[mode].long}
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
                    <Button type="submit" form="create-group-form" disabled={!canSubmit}>
                        Create
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
