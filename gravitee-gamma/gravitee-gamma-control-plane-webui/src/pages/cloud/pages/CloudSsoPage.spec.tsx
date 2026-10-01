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
import { render, screen } from '@testing-library/react';

import { CloudSsoPage } from './CloudSsoPage';

describe('CloudSsoPage', () => {
    it('renders Cockpit-style SSO configuration entry point', () => {
        render(<CloudSsoPage />);

        expect(screen.getByRole('heading', { name: 'Single Sign On' })).toBeTruthy();
        expect(
            screen.getByText(
                'Single Sign On (SSO) is an authentication method that enables users to access multiple applications with one set of credentials.',
            ),
        ).toBeTruthy();
        expect(
            screen.getByText(
                'You can set up your Gravitee Cloud account to trust a third-party Identity Provider (IdP) using Oauth 2.0/ Open ID connect.',
            ),
        ).toBeTruthy();
        expect(screen.getByText('OpenID Connect')).toBeTruthy();
        expect(screen.getByTestId('configure-sso-button')).toBeTruthy();
    });
});
