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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import type { DeveloperPortal } from '../../portals/types';
import { ConsumerAuthProvider } from '../context/ConsumerAuthProvider';
import { ConsumerAuthGate } from './ConsumerAuthGate';

const portal: DeveloperPortal = {
    id: 'portal-payments',
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

function renderGate({
    consumerAuthGateEnabled = true,
    initialPath = '/portals/portal-payments/home',
    inlineAuth = false,
}: {
    consumerAuthGateEnabled?: boolean;
    initialPath?: string;
    inlineAuth?: boolean;
} = {}) {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[initialPath]}>
            <ConsumerAuthProvider
                portalId="portal-payments"
                consumerAuthGateEnabled={consumerAuthGateEnabled}
                previewMode
            >
                <Routes>
                    <Route
                        path="/portals/:id/:slug"
                        element={
                            <ConsumerAuthGate
                                loginPath="/portals/portal-payments/login"
                                inlineAuth={inlineAuth}
                                portal={inlineAuth ? portal : undefined}
                                signupPath={inlineAuth ? '/portals/portal-payments/signup' : undefined}
                                defaultRedirectPath={
                                    inlineAuth ? '/portals/portal-payments/home' : undefined
                                }
                            >
                                <div>Protected portal content</div>
                            </ConsumerAuthGate>
                        }
                    />
                    <Route path="/portals/:id/login" element={<div>Login page</div>} />
                </Routes>
            </ConsumerAuthProvider>
        </MemoryRouter>,
    );
}

describe('ConsumerAuthGate', () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it('should redirect unauthenticated users to login when gate is enabled', async () => {
        renderGate();

        expect(await screen.findByText('Login page')).toBeInTheDocument();
    });

    it('should render protected content when gate is disabled', () => {
        renderGate({ consumerAuthGateEnabled: false });

        expect(screen.getByText('Protected portal content')).toBeInTheDocument();
    });

    it('should render inline login in editor preview without navigating away', async () => {
        renderGate({ inlineAuth: true });

        expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
        expect(screen.queryByText('Login page')).not.toBeInTheDocument();
        expect(screen.queryByText('Protected portal content')).not.toBeInTheDocument();
    });
});
