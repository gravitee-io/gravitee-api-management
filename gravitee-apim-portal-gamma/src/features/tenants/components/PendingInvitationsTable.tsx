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
import { Button } from '@gravitee/graphene-core';
import { useMemo } from 'react';

import constants from '../../../constants.json';
import { notify } from '../../../shared/notify/notify';
import { buildInviteUrl } from '../../consumer-auth/services/consumer-auth.service';
import type { PortalInvitation } from '../../consumer-auth/types/consumer-auth.types';

interface PendingInvitationsTableProps {
    readonly portalId: string;
    readonly invitations: readonly PortalInvitation[];
}

export function PendingInvitationsTable({ portalId, invitations }: PendingInvitationsTableProps) {
    const pendingInvitations = useMemo(
        () => invitations.filter(invitation => !invitation.acceptedAt),
        [invitations],
    );

    if (pendingInvitations.length === 0) {
        return null;
    }

    const handleCopy = async (token: string) => {
        const link = buildInviteUrl(portalId, token, constants.appBasePath ?? '');
        try {
            await navigator.clipboard.writeText(link);
            notify.success('Invite link copied to clipboard');
        } catch {
            notify.error('Failed to copy invite link');
        }
    };

    return (
        <div className="space-y-3">
            <div>
                <h3 className="text-base font-semibold">Pending invitations</h3>
                <p className="text-sm text-muted-foreground">
                    Invitations that have not been accepted yet.
                </p>
            </div>

            <div className="overflow-hidden rounded-lg border">
                <table className="w-full text-sm">
                    <thead className="bg-muted/40 text-left">
                        <tr>
                            <th className="px-4 py-3 font-medium">Email</th>
                            <th className="px-4 py-3 font-medium">Role</th>
                            <th className="px-4 py-3 font-medium">Status</th>
                            <th className="px-4 py-3 font-medium" />
                        </tr>
                    </thead>
                    <tbody>
                        {pendingInvitations.map(invitation => (
                            <tr key={invitation.id} className="border-t">
                                <td className="px-4 py-3">{invitation.email}</td>
                                <td className="px-4 py-3 capitalize">
                                    {invitation.role === 'admin' ? 'Tenant admin' : 'Member'}
                                </td>
                                <td className="px-4 py-3 text-muted-foreground">Pending</td>
                                <td className="px-4 py-3 text-right">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => void handleCopy(invitation.token)}
                                    >
                                        Copy link
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
