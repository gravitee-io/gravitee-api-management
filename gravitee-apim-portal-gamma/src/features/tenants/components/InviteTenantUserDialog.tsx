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
    Input,
    Label,
} from '@gravitee/graphene-core';
import { useState } from 'react';

import constants from '../../../constants.json';
import { notify } from '../../../shared/notify/notify';
import { buildInviteUrl, createTenantInvitation } from '../../consumer-auth/services/consumer-auth.service';
import type { PortalInvitation } from '../../consumer-auth/types/consumer-auth.types';
import type { PortalTenant, PortalTenantMemberRole } from '../types/portal-tenant.types';

interface InviteTenantUserDialogProps {
    readonly tenant: PortalTenant;
    readonly portalName: string;
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onInvited: (invitation: PortalInvitation) => void;
}

export function InviteTenantUserDialog({
    tenant,
    portalName,
    open,
    onOpenChange,
    onInvited,
}: InviteTenantUserDialogProps) {
    const [email, setEmail] = useState('');
    const [role, setRole] = useState<PortalTenantMemberRole>('member');
    const [isPending, setIsPending] = useState(false);
    const [createdInvitation, setCreatedInvitation] = useState<PortalInvitation | null>(null);
    const [error, setError] = useState<string | null>(null);

    const resetForm = () => {
        setEmail('');
        setRole('member');
        setCreatedInvitation(null);
        setError(null);
        setIsPending(false);
    };

    const handleOpenChange = (nextOpen: boolean) => {
        if (!nextOpen) {
            resetForm();
        }
        onOpenChange(nextOpen);
    };

    const handleInvite = async () => {
        setError(null);
        setIsPending(true);

        try {
            const invitation = await createTenantInvitation({
                portalId: tenant.portalId,
                tenantId: tenant.id,
                email,
                role,
            });
            setCreatedInvitation(invitation);
            onInvited(invitation);
        } catch {
            setError('Unable to send invitation. Please try again.');
        } finally {
            setIsPending(false);
        }
    };

    const inviteLink = createdInvitation
        ? buildInviteUrl(tenant.portalId, createdInvitation.token, constants.appBasePath ?? '')
        : '';

    const handleCopyLink = async () => {
        try {
            await navigator.clipboard.writeText(inviteLink);
            notify.success('Invite link copied to clipboard');
        } catch {
            notify.error('Failed to copy invite link');
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 32rem)' }}>
                <DialogHeader>
                    <DialogTitle>Invite user to {tenant.name}</DialogTitle>
                    <DialogDescription>
                        Email delivery is simulated in this POC — copy the invite link to share it.
                    </DialogDescription>
                </DialogHeader>

                {createdInvitation ? (
                    <div className="space-y-4 py-2">
                        <p className="text-sm">
                            Invitation sent to <strong>{createdInvitation.email}</strong>
                        </p>
                        <p className="text-sm text-muted-foreground">
                            You&apos;ve been invited to {tenant.name} on {portalName}. Open the link below to complete
                            onboarding.
                        </p>
                        <div className="rounded-md border bg-muted/30 p-3">
                            <p className="break-all text-xs text-muted-foreground">{inviteLink}</p>
                        </div>
                        <Button variant="outline" onClick={() => void handleCopyLink()}>
                            Copy invite link
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-4 py-2">
                        <div className="space-y-2">
                            <Label htmlFor="invite-email">Email address</Label>
                            <Input
                                id="invite-email"
                                type="email"
                                placeholder="colleague@company.com"
                                value={email}
                                onChange={event => setEmail(event.target.value)}
                                required
                            />
                        </div>

                        <fieldset className="space-y-2">
                            <legend className="text-sm font-medium">Role</legend>
                            <div className="flex gap-4 text-sm">
                                <label className="flex items-center gap-2">
                                    <input
                                        type="radio"
                                        name="invite-role"
                                        checked={role === 'member'}
                                        onChange={() => setRole('member')}
                                    />
                                    Member
                                </label>
                                <label className="flex items-center gap-2">
                                    <input
                                        type="radio"
                                        name="invite-role"
                                        checked={role === 'admin'}
                                        onChange={() => setRole('admin')}
                                    />
                                    Tenant admin
                                </label>
                            </div>
                        </fieldset>

                        {error && (
                            <p className="text-sm text-destructive" role="alert">
                                {error}
                            </p>
                        )}
                    </div>
                )}

                <DialogFooter>
                    {createdInvitation ? (
                        <Button onClick={() => handleOpenChange(false)}>Done</Button>
                    ) : (
                        <>
                            <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={isPending}>
                                Cancel
                            </Button>
                            <Button disabled={!email.trim() || isPending} onClick={() => void handleInvite()}>
                                {isPending ? 'Sending…' : 'Send invitation'}
                            </Button>
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
