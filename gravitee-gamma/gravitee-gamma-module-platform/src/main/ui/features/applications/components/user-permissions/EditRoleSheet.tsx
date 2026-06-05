/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Button,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { formatRoleLabel, getApplicationRole } from './memberHelpers';
import type { ApplicationUiMember } from '../../types/applicationMembers.types';

export function EditRoleSheet({
    member,
    roles,
    onClose,
    onSave,
    isSaving,
}: Readonly<{
    member: ApplicationUiMember | null;
    roles: string[];
    onClose: () => void;
    onSave: (roleName: string) => void;
    isSaving: boolean;
}>) {
    const [role, setRole] = useState('');

    useEffect(() => {
        setRole(member ? getApplicationRole(member) : '');
    }, [member]);

    const currentRole = member ? getApplicationRole(member) : '';
    const hasChange = Boolean(member) && role !== currentRole;

    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={member !== null} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Edit Role</SheetTitle>
                    <SheetDescription>Change the role for {member?.displayName}.</SheetDescription>
                </SheetHeader>

                {member ? (
                    <div className="min-h-0 flex-1 space-y-4 px-4">
                        <div className="flex items-center gap-3">
                            <MemberAvatar name={member.displayName} />
                            <div className="min-w-0">
                                <p className="text-sm font-medium">{member.displayName}</p>
                                {member.email ? <p className="text-xs text-muted-foreground truncate">{member.email}</p> : null}
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Role</Label>
                            <Select value={role} onValueChange={setRole}>
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select a role…" />
                                </SelectTrigger>
                                <SelectContent>
                                    {roles.map(roleName => (
                                        <SelectItem key={roleName} value={roleName}>
                                            {formatRoleLabel(roleName)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                ) : null}

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onSave(role)} disabled={isSaving || !role || !member || !hasChange}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
