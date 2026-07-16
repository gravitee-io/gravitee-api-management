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
import { createTenantMemberId } from '../../tenants/utils/tenant-hrid';
import { getMemberByUserId, savePortalTenantMember } from '../../tenants/storage/portal-tenant-members.storage';
import { getTenantsByPortalId } from '../../tenants/storage/portal-tenants.storage';
import type { PortalTenantMemberRole } from '../../tenants/types/portal-tenant.types';
import {
    findConsumerByCredential,
    getConsumerById,
    getConsumerByPortalAndEmail,
    savePortalConsumer,
} from '../storage/portal-consumers.storage';
import {
    getInvitationByToken,
    savePortalInvitation,
} from '../storage/portal-invitations.storage';
import {
    clearConsumerSession,
    readConsumerSession,
    writeConsumerSession,
} from '../storage/consumer-session.storage';
import {
    createConsumerId,
    createInvitationId,
    createInvitationToken,
} from '../utils/consumer-auth-ids';
import type {
    AcceptInviteInput,
    ConsumerAuthProvider,
    PortalConsumer,
    PortalConsumerSession,
    PortalInvitation,
    SignupInput,
} from '../types/consumer-auth.types';

export class ConsumerAuthError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'ConsumerAuthError';
    }
}

async function ensureTenantMember(
    consumer: PortalConsumer,
    role: PortalTenantMemberRole = 'member',
): Promise<void> {
    const existing = await getMemberByUserId(consumer.id);
    if (existing) {
        return;
    }

    await savePortalTenantMember({
        id: createTenantMemberId(),
        tenantId: consumer.tenantId,
        userId: consumer.id,
        displayName: `${consumer.firstName} ${consumer.lastName}`.trim(),
        email: consumer.email,
        role,
    });
}

export async function loginConsumer(
    portalId: string,
    identifier: string,
    password: string,
): Promise<PortalConsumer> {
    const consumer = await findConsumerByCredential(portalId, identifier, password);
    if (!consumer) {
        throw new ConsumerAuthError('The username or password you entered is incorrect.');
    }

    writeConsumerSession({
        consumerId: consumer.id,
        portalId: consumer.portalId,
        tenantId: consumer.tenantId,
    });

    return consumer;
}

export function logoutConsumer(portalId: string): void {
    clearConsumerSession(portalId);
}

export async function getAuthenticatedConsumer(portalId: string): Promise<PortalConsumer | null> {
    const session = readConsumerSession(portalId);
    if (!session) {
        return null;
    }

    const consumer = await getConsumerById(session.consumerId);
    if (!consumer || consumer.portalId !== portalId) {
        clearConsumerSession(portalId);
        return null;
    }

    return consumer;
}

export async function signupConsumer(portalId: string, input: SignupInput): Promise<PortalConsumer> {
    const email = input.email.trim().toLowerCase();
    const existing = await getConsumerByPortalAndEmail(portalId, email);
    if (existing) {
        throw new ConsumerAuthError('An account with this email already exists.');
    }

    const tenants = await getTenantsByPortalId(portalId);
    const tenantId = input.tenantId ?? tenants[0]?.id;
    if (!tenantId) {
        throw new ConsumerAuthError('No tenant is available for self-service signup.');
    }

    const consumer: PortalConsumer = {
        id: createConsumerId(),
        portalId,
        tenantId,
        email,
        firstName: input.firstName.trim(),
        lastName: input.lastName.trim(),
        company: input.company.trim(),
        jobTitle: input.jobTitle?.trim() || undefined,
        useCase: input.useCase,
        password: input.password,
        authProvider: 'local',
        createdAt: new Date().toISOString(),
    };

    await savePortalConsumer(consumer);
    await ensureTenantMember(consumer, 'member');

    writeConsumerSession({
        consumerId: consumer.id,
        portalId,
        tenantId,
    });

    return consumer;
}

export async function loginOrCreateSsoConsumer(
    portalId: string,
    provider: ConsumerAuthProvider,
): Promise<PortalConsumer> {
    if (provider === 'local') {
        throw new ConsumerAuthError('Invalid SSO provider.');
    }

    const tenants = await getTenantsByPortalId(portalId);
    const tenantId = tenants[0]?.id;
    if (!tenantId) {
        throw new ConsumerAuthError('No tenant is available for SSO signup.');
    }

    const suffix = Math.random().toString(36).slice(2, 8);
    const email = `${provider}-${suffix}@sso.demo`;

    let consumer = await getConsumerByPortalAndEmail(portalId, email);
    if (!consumer) {
        consumer = {
            id: createConsumerId(),
            portalId,
            tenantId,
            email,
            firstName: provider.charAt(0).toUpperCase() + provider.slice(1),
            lastName: 'User',
            password: '',
            authProvider: provider,
            createdAt: new Date().toISOString(),
        };
        await savePortalConsumer(consumer);
        await ensureTenantMember(consumer, 'member');
    }

    writeConsumerSession({
        consumerId: consumer.id,
        portalId,
        tenantId: consumer.tenantId,
    });

    return consumer;
}

export async function createTenantInvitation(input: {
    portalId: string;
    tenantId: string;
    email: string;
    role: PortalTenantMemberRole;
}): Promise<PortalInvitation> {
    const invitation: PortalInvitation = {
        id: createInvitationId(),
        portalId: input.portalId,
        tenantId: input.tenantId,
        email: input.email.trim().toLowerCase(),
        token: createInvitationToken(),
        role: input.role,
        createdAt: new Date().toISOString(),
    };

    await savePortalInvitation(invitation);
    return invitation;
}

export async function acceptInvitation(
    portalId: string,
    input: AcceptInviteInput,
): Promise<PortalConsumer> {
    const invitation = await getInvitationByToken(input.token);
    if (!invitation || invitation.portalId !== portalId) {
        throw new ConsumerAuthError('This invitation link is invalid or has expired.');
    }

    if (invitation.acceptedAt) {
        throw new ConsumerAuthError('This invitation has already been accepted.');
    }

    const existing = await getConsumerByPortalAndEmail(portalId, invitation.email);
    if (existing) {
        throw new ConsumerAuthError('An account with this email already exists.');
    }

    const consumer: PortalConsumer = {
        id: createConsumerId(),
        portalId,
        tenantId: invitation.tenantId,
        email: invitation.email,
        firstName: input.firstName.trim(),
        lastName: input.lastName.trim(),
        company: input.company?.trim() || undefined,
        password: input.password,
        authProvider: 'local',
        createdAt: new Date().toISOString(),
    };

    await savePortalConsumer(consumer);
    await ensureTenantMember(consumer, invitation.role);

    await savePortalInvitation({
        ...invitation,
        acceptedAt: new Date().toISOString(),
    });

    writeConsumerSession({
        consumerId: consumer.id,
        portalId,
        tenantId: consumer.tenantId,
    });

    return consumer;
}

export function buildInviteUrl(portalId: string, token: string, basePath = ''): string {
    const normalizedBase = basePath.replace(/\/$/, '');
    const path = `${normalizedBase}/portals/${portalId}/invite/${token}`;
    return `${window.location.origin}${path}`;
}

export function getSessionForPortal(portalId: string): PortalConsumerSession | null {
    return readConsumerSession(portalId);
}
