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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { RoleListTooltipContent } from './RoleListTooltip';
import { UserRoleMultiSelect } from './UserRoleMultiSelect';

const OPTIONS = [
    { value: 'admin', label: 'ADMIN' },
    { value: 'publisher', label: 'API_PUBLISHER' },
];

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

describe('UserRoleMultiSelect', () => {
    it('opens a checkbox list and commits selected roles when the popover closes', async () => {
        const user = userEvent.setup();
        const onSelectedValuesChange = jest.fn();

        renderWithGraphene(
            <UserRoleMultiSelect
                options={OPTIONS}
                selectedValues={['admin']}
                onSelectedValuesChange={onSelectedValuesChange}
                ariaLabel="Organization roles"
            />,
        );

        expect(screen.getByText('ADMIN')).toBeTruthy();

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));
        expect(screen.getByRole('checkbox', { name: 'API_PUBLISHER' })).toBeTruthy();

        await user.click(screen.getByRole('checkbox', { name: 'API_PUBLISHER' }));
        expect(onSelectedValuesChange).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));
        expect(onSelectedValuesChange).toHaveBeenCalledWith(['admin', 'publisher']);
    });

    it('does not commit when the popover closes without changes', async () => {
        const user = userEvent.setup();
        const onSelectedValuesChange = jest.fn();

        renderWithGraphene(
            <UserRoleMultiSelect
                options={OPTIONS}
                selectedValues={['admin']}
                onSelectedValuesChange={onSelectedValuesChange}
                ariaLabel="Organization roles"
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));
        await user.click(screen.getByRole('button', { name: 'Organization roles' }));

        expect(onSelectedValuesChange).not.toHaveBeenCalled();
    });

    it('does not open or commit when disabled', async () => {
        const user = userEvent.setup();
        const onSelectedValuesChange = jest.fn();

        renderWithGraphene(
            <UserRoleMultiSelect
                options={OPTIONS}
                selectedValues={[]}
                onSelectedValuesChange={onSelectedValuesChange}
                ariaLabel="Organization roles"
                disabled
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));

        expect(screen.queryByRole('checkbox', { name: 'ADMIN' })).toBeNull();
        expect(onSelectedValuesChange).not.toHaveBeenCalled();
    });

    it('shows an empty message when no roles are available', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <UserRoleMultiSelect
                options={[]}
                selectedValues={[]}
                onSelectedValuesChange={jest.fn()}
                ariaLabel="Organization roles"
                emptyMessage="No organization roles"
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));

        expect(screen.getByText('No organization roles')).toBeTruthy();
    });

    it('renders a comma-separated tooltip with every role name intact', () => {
        const labels = ['Admin', 'ORG_TEST1', 'ORG_TEST10', 'AAASKLNG_LSDJGH_OSRGUH_OURGHS_OUGOSUHG_SOUHFOSDUHAOUGI', 'User'];

        renderWithGraphene(<RoleListTooltipContent labels={labels} />);

        expect(screen.getByText(labels.join(', '))).toBeTruthy();
    });

    it('lists every role option when many roles are available', async () => {
        const user = userEvent.setup();
        const manyOptions = Array.from({ length: 20 }, (_, index) => ({
            value: `role-${index}`,
            label: `ORG_TEST${index}`,
        }));

        renderWithGraphene(
            <UserRoleMultiSelect
                options={manyOptions}
                selectedValues={[]}
                onSelectedValuesChange={jest.fn()}
                ariaLabel="Organization roles"
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Organization roles' }));

        expect(screen.getByRole('checkbox', { name: 'ORG_TEST0' })).toBeTruthy();
        expect(screen.getByRole('checkbox', { name: 'ORG_TEST19' })).toBeTruthy();
    });
});
