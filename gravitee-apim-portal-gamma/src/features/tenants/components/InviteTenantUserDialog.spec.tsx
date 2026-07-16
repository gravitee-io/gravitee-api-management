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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { createDummyPortalTenants } from '../storage/dummy-portal-tenants';
import { setupConsumerAuthDatabaseTests } from '../../consumer-auth/testing/consumer-auth.test-utils';
import { InviteTenantUserDialog } from './InviteTenantUserDialog';

const tenant = createDummyPortalTenants('portal-payments')[0]!;

describe('InviteTenantUserDialog', () => {
    setupConsumerAuthDatabaseTests();

    it('should show success state with copy invite link action', async () => {
        const user = userEvent.setup();
        const onInvited = jest.fn();

        renderWithGraphene(
            <InviteTenantUserDialog
                tenant={tenant}
                portalName="Payments Portal"
                open
                onOpenChange={jest.fn()}
                onInvited={onInvited}
            />,
        );

        await user.type(screen.getByLabelText('Email address'), 'new.user@example.com');
        await user.click(screen.getByRole('button', { name: 'Send invitation' }));

        expect(await screen.findByText(/Invitation sent to/i)).toBeInTheDocument();
        expect(onInvited).toHaveBeenCalled();
        expect(screen.getByText(/\/invite\//)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Copy invite link' })).toBeInTheDocument();
    });
});
