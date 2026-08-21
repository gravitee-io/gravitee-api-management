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

import { IdentityProviderMappingMultiSelect } from './IdentityProviderMappingMultiSelect';

function renderSelect(
    values: string[] = ['group-a', 'deleted-group'],
    onChange: (values: string[]) => void = jest.fn(),
    hideLabel = false,
) {
    return renderWithGraphene(
        <IdentityProviderMappingMultiSelect
            id="idp-groups"
            label="Group"
            values={values}
            options={[{ id: 'group-a', name: 'Group A' }]}
            placeholder="Select groups"
            emptyMessage="No groups available"
            hideLabel={hideLabel}
            onChange={onChange}
        />,
    );
}

describe('IdentityProviderMappingMultiSelect', () => {
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

    it('includes selected ids that are missing from the catalog in the summary', () => {
        renderSelect();
        expect(screen.getByText('Group A, deleted-group')).not.toBeNull();
    });

    it('shows three selected names then an ellipsis, with the full list on hover', async () => {
        const user = userEvent.setup();
        renderWithGraphene(
            <IdentityProviderMappingMultiSelect
                id="idp-roles"
                label="Organization roles"
                values={['org-8', 'user', 'org', 'admin', 'org-2']}
                options={[
                    { id: 'org-8', name: 'ORGANIZATION_8' },
                    { id: 'user', name: 'USER' },
                    { id: 'org', name: 'ORG' },
                    { id: 'admin', name: 'ADMIN' },
                    { id: 'org-2', name: 'ORGANIZATION_2' },
                ]}
                placeholder="Select organization roles"
                emptyMessage="No organization roles available"
                onChange={jest.fn()}
            />,
        );

        expect(screen.getByText('ORGANIZATION_8, USER, ORG...')).not.toBeNull();
        expect(screen.queryByText('ORGANIZATION_8, USER, ORG, ADMIN, ORGANIZATION_2')).toBeNull();

        await user.hover(screen.getByText('ORGANIZATION_8, USER, ORG...'));
        expect((await screen.findByRole('tooltip')).textContent).toBe('ORGANIZATION_8, USER, ORG, ADMIN, ORGANIZATION_2');
        expect(screen.getByRole('button', { name: 'Organization roles: ORGANIZATION_8, USER, ORG, ADMIN, ORGANIZATION_2' })).not.toBeNull();
    });

    it('shows every selected name when there are three or fewer', () => {
        renderSelect();
        expect(screen.getByText('Group A, deleted-group')).not.toBeNull();
        expect(screen.queryByText('Group A, deleted-group...')).toBeNull();
    });

    it('lets the user remove a value that is past the truncated summary', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();
        renderWithGraphene(
            <IdentityProviderMappingMultiSelect
                id="idp-roles"
                label="Organization roles"
                values={['org-8', 'user', 'org', 'admin', 'org-2']}
                options={[
                    { id: 'org-8', name: 'ORGANIZATION_8' },
                    { id: 'user', name: 'USER' },
                    { id: 'org', name: 'ORG' },
                    { id: 'admin', name: 'ADMIN' },
                    { id: 'org-2', name: 'ORGANIZATION_2' },
                ]}
                placeholder="Select organization roles"
                emptyMessage="No organization roles available"
                onChange={onChange}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Organization roles: ORGANIZATION_8, USER, ORG, ADMIN, ORGANIZATION_2' }));
        await user.click(screen.getByRole('button', { name: 'ADMIN' }));
        expect(onChange).toHaveBeenCalledWith(['org-8', 'user', 'org', 'org-2']);
    });

    it('keeps a visually hidden label for table cells', () => {
        renderSelect(['group-a'], jest.fn(), true);
        expect(screen.getByText('Group')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Group: Group A' })).not.toBeNull();
    });
});
