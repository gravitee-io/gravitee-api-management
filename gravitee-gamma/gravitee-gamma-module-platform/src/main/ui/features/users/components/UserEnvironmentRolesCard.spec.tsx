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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { UserEnvironmentRolesCard } from './UserEnvironmentRolesCard';
import type { OrganizationEnvironment, OrganizationRole, OrganizationUser } from '../types/user';

jest.mock('./UserRoleMultiSelect', () => ({
    UserRoleMultiSelect: ({ ariaLabel }: { ariaLabel: string }) => <div data-testid="role-select">{ariaLabel}</div>,
}));

const ROLES: OrganizationRole[] = [
    { id: 'env-api-user', name: 'API_USER', scope: 'ENVIRONMENT' },
    { id: 'env-api-publisher', name: 'API_PUBLISHER', scope: 'ENVIRONMENT' },
];

const USER: OrganizationUser = {
    id: 'user-1',
    envRoles: {
        env1: [{ id: 'env-api-user', name: 'API_USER' }],
    },
};

function buildEnvironments(count: number): OrganizationEnvironment[] {
    return Array.from({ length: count }, (_, index) => ({
        id: `env${index + 1}`,
        name: `Environment ${index + 1}`,
        description: `Description ${index + 1}`,
    }));
}

function renderCard(environments: OrganizationEnvironment[], overrides: Partial<Parameters<typeof UserEnvironmentRolesCard>[0]> = {}) {
    return renderWithGraphene(
        <UserEnvironmentRolesCard
            user={USER}
            environments={environments}
            roles={ROLES}
            loading={false}
            disabled={false}
            onEnvironmentRolesChange={jest.fn()}
            {...overrides}
        />,
    );
}

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

describe('UserEnvironmentRolesCard', () => {
    it('renders search and pagination controls for environment roles', () => {
        renderCard(buildEnvironments(2));

        const section = screen.getByRole('region', { name: 'Environment roles table' });
        expect(within(section).getByPlaceholderText('Search')).toBeTruthy();
        expect(within(section).getByText('1-2 of 2')).toBeTruthy();
        expect(within(section).getByText('Environment 1')).toBeTruthy();
        expect(within(section).getByText('Environment 2')).toBeTruthy();
    });

    it('paginates environments when more rows exist than the default page size', async () => {
        const user = userEvent.setup();
        renderCard(buildEnvironments(12));

        const section = screen.getByRole('region', { name: 'Environment roles table' });
        expect(within(section).getByText('1-10 of 12')).toBeTruthy();
        expect(within(section).queryByText('Environment 11')).toBeNull();

        await user.click(within(section).getByRole('button', { name: 'Next page' }));

        expect(within(section).getByText('11-12 of 12')).toBeTruthy();
        expect(within(section).getByText('Environment 11')).toBeTruthy();
        expect(within(section).queryByText('Environment 1')).toBeNull();
    });

    it('filters environments client-side and resets pagination when the search changes', async () => {
        const user = userEvent.setup();
        renderCard(buildEnvironments(12));

        const section = screen.getByRole('region', { name: 'Environment roles table' });
        await user.click(within(section).getByRole('button', { name: 'Next page' }));
        expect(within(section).getByText('11-12 of 12')).toBeTruthy();

        await user.type(within(section).getByPlaceholderText('Search'), 'Environment 11');

        expect(within(section).getByText('1-1 of 1')).toBeTruthy();
        expect(within(section).getByText('Environment 11')).toBeTruthy();
        expect(within(section).queryByText('Environment 1')).toBeNull();
    });

    it('does not match environment hrids because Classic only searches name and description', async () => {
        const user = userEvent.setup();
        renderCard([
            {
                id: 'env1',
                name: 'Production',
                description: 'Primary environment',
                hrids: ['prod-env'],
            },
        ]);

        const section = screen.getByRole('region', { name: 'Environment roles table' });
        await user.type(within(section).getByPlaceholderText('Search'), 'prod-env');

        expect(within(section).getByText('No environments match your search.')).toBeTruthy();
    });

    it('resets environment role search and pagination when the user id changes', async () => {
        const user = userEvent.setup();
        const view = renderCard(buildEnvironments(12));

        const section = screen.getByRole('region', { name: 'Environment roles table' });
        await user.click(within(section).getByRole('button', { name: 'Next page' }));
        await user.type(within(section).getByPlaceholderText('Search'), 'Environment 11');
        expect(within(section).getByText('1-1 of 1')).toBeTruthy();

        view.rerender(
            <UserEnvironmentRolesCard
                user={{ ...USER, id: 'user-2' }}
                environments={buildEnvironments(12)}
                roles={ROLES}
                loading={false}
                disabled={false}
                onEnvironmentRolesChange={jest.fn()}
            />,
        );

        expect(within(section).getByText('1-10 of 12')).toBeTruthy();
        expect((within(section).getByPlaceholderText('Search') as HTMLInputElement).value).toBe('');
    });
});
