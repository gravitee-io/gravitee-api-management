/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { IntegrationDiscoverButton } from './IntegrationDiscoverButton';
import type { IntegrationAgentStatus } from '../types/integration';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useHasPermission: jest.fn() }));

const mockUseHasPermission = jest.mocked(useHasPermission);

function grantEnvironmentPermissions(permissions: string[]) {
    mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.some(permission => permissions.includes(permission)) ?? false);
}

function renderDiscoverButton(agentStatus: IntegrationAgentStatus | undefined, isIngesting: boolean) {
    render(
        <MemoryRouter initialEntries={['/integrations/int-1']}>
            <IntegrationDiscoverButton integrationId="int-1" agentStatus={agentStatus} isIngesting={isIngesting} />
        </MemoryRouter>,
    );
}

describe('IntegrationDiscoverButton', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it.each([
        { name: 'renders no Discover APIs element without the environment integration create permission', permissions: [], shown: false },
        {
            name: 'renders the Discover APIs action with the environment integration create permission',
            permissions: ['environment-integration-c'],
            shown: true,
        },
    ])('$name', ({ permissions, shown }) => {
        grantEnvironmentPermissions(permissions);

        renderDiscoverButton('CONNECTED', false);

        expect(screen.queryByRole('button', { name: 'Discover APIs' }) !== null).toBe(shown);
    });

    it.each([
        {
            name: 'enables Discover APIs for a connected agent with no running ingestion',
            agentStatus: 'CONNECTED' as const,
            isIngesting: false,
            enabled: true,
        },
        {
            name: 'disables Discover APIs for a disconnected agent',
            agentStatus: 'DISCONNECTED' as const,
            isIngesting: false,
            enabled: false,
        },
        {
            name: 'disables Discover APIs when the agent status is unknown',
            agentStatus: undefined,
            isIngesting: false,
            enabled: false,
        },
        {
            name: 'disables Discover APIs while an ingestion is running for a connected agent',
            agentStatus: 'CONNECTED' as const,
            isIngesting: true,
            enabled: false,
        },
    ])('$name', ({ agentStatus, isIngesting, enabled }) => {
        grantEnvironmentPermissions(['environment-integration-c']);

        renderDiscoverButton(agentStatus, isIngesting);

        expect(screen.getByRole('button', { name: 'Discover APIs' })).toHaveProperty('disabled', !enabled);
    });
});
