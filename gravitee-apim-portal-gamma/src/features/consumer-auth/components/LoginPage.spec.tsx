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
import { MemoryRouter } from 'react-router-dom';

import type { DeveloperPortal } from '../../portals/types';
import { ConsumerAuthProvider } from '../context/ConsumerAuthProvider';
import { DEMO_CONSUMER_PASSWORD, DEMO_CONSUMER_USERNAME, seedDemoConsumerForPortal } from '../storage/seed-demo-consumer';
import { setupConsumerAuthDatabaseTests, TEST_PORTAL_ID } from '../testing/consumer-auth.test-utils';
import { LoginPage } from './LoginPage';

const portal: DeveloperPortal = {
    id: TEST_PORTAL_ID,
    name: 'Payments Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'header-content-footer',
    showFooter: true,
    pageWidth: 'narrow',
    portalIconUrl: '',
    portalLabel: 'Payments',
    footerLinks: [],
    userMenuItems: [],
};

function renderLoginPage() {
    return renderWithGraphene(
        <MemoryRouter>
            <ConsumerAuthProvider portalId={TEST_PORTAL_ID} consumerAuthGateEnabled={false} previewMode>
                <LoginPage
                    portal={portal}
                    loginPath={`/portals/${TEST_PORTAL_ID}/login`}
                    signupPath={`/portals/${TEST_PORTAL_ID}/signup`}
                    defaultRedirectPath={`/portals/${TEST_PORTAL_ID}/home`}
                />
            </ConsumerAuthProvider>
        </MemoryRouter>,
    );
}

describe('LoginPage', () => {
    setupConsumerAuthDatabaseTests();

    beforeEach(async () => {
        await seedDemoConsumerForPortal(TEST_PORTAL_ID);
    });

    it('should render login form with demo credentials hint', () => {
        renderLoginPage();

        expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
        expect(screen.getByLabelText('Email or username')).toHaveValue(DEMO_CONSUMER_USERNAME);
        expect(screen.getByLabelText('Password')).toHaveValue(DEMO_CONSUMER_PASSWORD);
        expect(screen.getByText(/Demo credentials/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Continue with Google' })).toBeInTheDocument();
    });

    it('should show error for invalid credentials', async () => {
        const user = userEvent.setup();
        renderLoginPage();

        await user.clear(screen.getByLabelText('Email or username'));
        await user.type(screen.getByLabelText('Email or username'), 'wrong');
        await user.clear(screen.getByLabelText('Password'));
        await user.type(screen.getByLabelText('Password'), 'wrong');
        await user.click(screen.getByRole('button', { name: 'Sign in' }));

        expect(await screen.findByRole('alert')).toHaveTextContent(/incorrect/i);
    });
});
