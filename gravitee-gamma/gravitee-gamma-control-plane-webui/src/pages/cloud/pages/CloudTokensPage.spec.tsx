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

import { CloudTokensPage } from './CloudTokensPage';

describe('CloudTokensPage', () => {
    it('renders Cockpit-style cloud tokens empty state', () => {
        render(<CloudTokensPage />);

        expect(screen.getByRole('heading', { name: 'Cloud Tokens' })).toBeTruthy();
        expect(screen.getByText('Your Cloud Tokens')).toBeTruthy();
        expect(screen.getByTestId('generate-cloud-token-button')).toBeTruthy();
        expect(
            screen.getByText(
                'Cloud tokens are secure, signed Json Web Tokens (JWT) that enable connection between your self-hosted services and the Gravitee Cloud API Management Control Plane. They are used for:',
            ),
        ).toBeTruthy();
        expect(
            screen.getByText('Importing APIs from other Gateways via Gravitee\'s Federated API Management capability.'),
        ).toBeTruthy();
        expect(
            screen.getByText('Managing APIs across your API Management Environments in Gravitee Cloud using automation tools.'),
        ).toBeTruthy();
        expect(screen.getByTestId('cloud-tokens-empty')).toBeTruthy();
        expect(screen.getByText('No tokens... yet')).toBeTruthy();
    });
});
