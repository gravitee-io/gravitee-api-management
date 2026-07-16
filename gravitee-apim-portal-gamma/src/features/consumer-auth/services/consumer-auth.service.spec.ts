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
import { getTenantsByPortalId } from '../../tenants/storage/portal-tenants.storage';
import {
    acceptInvitation,
    ConsumerAuthError,
    createTenantInvitation,
    getAuthenticatedConsumer,
    loginConsumer,
    logoutConsumer,
    signupConsumer,
} from './consumer-auth.service';
import { getInvitationByToken } from '../storage/portal-invitations.storage';
import { getConsumerByPortalAndEmail, getConsumersByPortalId } from '../storage/portal-consumers.storage';
import { seedDemoConsumerForPortal } from '../storage/seed-demo-consumer';
import {
    setupConsumerAuthDatabaseTests,
    TEST_PORTAL_ID,
} from '../testing/consumer-auth.test-utils';

describe('consumer-auth.service', () => {
    setupConsumerAuthDatabaseTests();

    it('should log in demo user with username alias', async () => {
        await seedDemoConsumerForPortal(TEST_PORTAL_ID);

        const consumer = await loginConsumer(TEST_PORTAL_ID, 'user', 'user');
        expect(consumer.email).toBe('user@demo.local');

        const sessionConsumer = await getAuthenticatedConsumer(TEST_PORTAL_ID);
        expect(sessionConsumer?.id).toBe(consumer.id);
    });

    it('should sign up a new consumer and persist session', async () => {
        const consumer = await signupConsumer(TEST_PORTAL_ID, {
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            password: 'secret',
            company: 'Analytical Engines',
        });

        expect(consumer.email).toBe('ada@example.com');
        expect(await getAuthenticatedConsumer(TEST_PORTAL_ID)).toMatchObject({ id: consumer.id });
    });

    it('should reject duplicate signup email', async () => {
        await signupConsumer(TEST_PORTAL_ID, {
            firstName: 'Grace',
            lastName: 'Hopper',
            email: 'grace@example.com',
            password: 'secret',
            company: 'US Navy',
        });

        await expect(
            signupConsumer(TEST_PORTAL_ID, {
                firstName: 'Grace',
                lastName: 'Duplicate',
                email: 'grace@example.com',
                password: 'secret',
                company: 'US Navy',
            }),
        ).rejects.toBeInstanceOf(ConsumerAuthError);
    });

    it('should clear session on logout', async () => {
        await seedDemoConsumerForPortal(TEST_PORTAL_ID);
        await loginConsumer(TEST_PORTAL_ID, 'user', 'user');

        logoutConsumer(TEST_PORTAL_ID);

        expect(await getAuthenticatedConsumer(TEST_PORTAL_ID)).toBeNull();
    });

    it('should accept invitation and mark it accepted', async () => {
        const tenants = await getTenantsByPortalId(TEST_PORTAL_ID);
        const invitation = await createTenantInvitation({
            portalId: TEST_PORTAL_ID,
            tenantId: tenants[0]!.id,
            email: 'invitee@example.com',
            role: 'member',
        });

        const consumer = await acceptInvitation(TEST_PORTAL_ID, {
            token: invitation.token,
            firstName: 'Invited',
            lastName: 'User',
            password: 'welcome123',
        });

        expect(consumer.email).toBe('invitee@example.com');

        const storedInvitation = await getInvitationByToken(invitation.token);
        expect(storedInvitation?.acceptedAt).toBeTruthy();
        expect(await getConsumerByPortalAndEmail(TEST_PORTAL_ID, 'invitee@example.com')).toBeTruthy();
    });

    it('should seed demo consumer once when called concurrently', async () => {
        await Promise.all([
            seedDemoConsumerForPortal(TEST_PORTAL_ID),
            seedDemoConsumerForPortal(TEST_PORTAL_ID),
        ]);

        const consumers = await getConsumersByPortalId(TEST_PORTAL_ID);
        expect(consumers).toHaveLength(1);
        expect(consumers[0]?.email).toBe('user@demo.local');
    });
});
