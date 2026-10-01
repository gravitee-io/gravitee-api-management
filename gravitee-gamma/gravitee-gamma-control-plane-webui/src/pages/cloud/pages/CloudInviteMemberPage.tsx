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
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    toast,
} from '@gravitee/graphene-core';
import { type FormEvent, useCallback, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { CLOUD_ACCOUNT_MEMBER_ROLES_MOCK } from '../cloud.config';

const EMAIL_MAX_LENGTH = 128;

function isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function CloudInviteMemberPage() {
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState(() => searchParams.get('name') ?? '');
    const [roleId, setRoleId] = useState('');
    const [sending, setSending] = useState(false);

    const canSend = useMemo(() => {
        const trimmedEmail = email.trim();
        return trimmedEmail.length >= 2 && trimmedEmail.length <= EMAIL_MAX_LENGTH && isValidEmail(trimmedEmail) && roleId.length > 0;
    }, [email, roleId]);

    const handleSubmit = useCallback(
        (event: FormEvent) => {
            event.preventDefault();
            if (!canSend || sending) return;

            setSending(true);
            const trimmedEmail = email.trim();
            toast.success(`Invitation have been sent to ${trimmedEmail}`);
            setSending(false);
        },
        [canSend, email, sending],
    );

    return (
        <div className="max-w-3xl space-y-6">
            <h1 className="text-2xl font-bold tracking-tight">Invite member to account</h1>

            <Card>
                <CardContent className="pt-6">
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="member-email">
                                User email<span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="member-email"
                                data-testid="member-email-input"
                                type="email"
                                value={email}
                                onChange={event => setEmail(event.target.value)}
                                placeholder="User email"
                                autoFocus
                                maxLength={EMAIL_MAX_LENGTH}
                                aria-required
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="member-role">
                                Role<span className="text-destructive">*</span>
                            </Label>
                            <Select value={roleId} onValueChange={setRoleId}>
                                <SelectTrigger id="member-role" data-testid="member-role-select" aria-label="Role" className="w-full">
                                    <SelectValue placeholder="Role" />
                                </SelectTrigger>
                                <SelectContent>
                                    {CLOUD_ACCOUNT_MEMBER_ROLES_MOCK.map(role => (
                                        <SelectItem key={role.id} value={role.id}>
                                            {role.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit" data-testid="send-invitation-btn" disabled={!canSend || sending}>
                                Send invitation
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
