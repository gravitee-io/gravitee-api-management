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
import userEvent from '@testing-library/user-event';

import { ApplicationGroupsField } from './ApplicationGroupsField';
import type { ApplicationGroup } from '../../types/applicationCreate';

const GROUPS: ApplicationGroup[] = [
    { id: 'group-z', name: 'Zeta Team' },
    { id: 'group-a', name: 'Alpha Team' },
    { id: 'group-m', name: 'Middle Team' },
];

function renderField(
    props: Partial<{
        groups: ApplicationGroup[];
        selectedGroupIds: string[];
        onSelectedGroupIdsChange: (groupIds: string[]) => void;
        isLoading: boolean;
        required: boolean;
    }> = {},
) {
    const onSelectedGroupIdsChange = props.onSelectedGroupIdsChange ?? jest.fn();

    render(
        <ApplicationGroupsField
            groups={props.groups ?? GROUPS}
            selectedGroupIds={props.selectedGroupIds ?? []}
            onSelectedGroupIdsChange={onSelectedGroupIdsChange}
            isLoading={props.isLoading}
            required={props.required}
        />,
    );

    return { onSelectedGroupIdsChange };
}

describe('ApplicationGroupsField', () => {
    it('shows the required indicator and placeholder when required', () => {
        renderField({ required: true });

        expect(screen.getByText('*')).not.toBeNull();
        expect(screen.getByPlaceholderText('Select groups (required)')).not.toBeNull();
    });

    it('renders selected groups as removable chips', () => {
        renderField({ selectedGroupIds: ['group-a', 'group-z'] });

        expect(screen.getByText('Alpha Team')).not.toBeNull();
        expect(screen.getByText('Zeta Team')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Remove Alpha Team' })).not.toBeNull();
    });

    it('lists groups in alphabetical order when the combobox is opened', async () => {
        const user = userEvent.setup();
        renderField();

        await user.click(screen.getByLabelText('Search groups'));

        const options = screen.getAllByRole('option');
        expect(options.map(option => option.textContent)).toEqual(['Alpha Team', 'Middle Team', 'Zeta Team']);
        expect(document.querySelector('[data-slot="combobox-empty"]')).toBeNull();
    });

    it('adds a group to the selection', async () => {
        const user = userEvent.setup();
        const { onSelectedGroupIdsChange } = renderField();

        await user.click(screen.getByLabelText('Search groups'));
        await user.click(screen.getByRole('option', { name: 'Middle Team' }));

        expect(onSelectedGroupIdsChange).toHaveBeenCalledWith(['group-m']);
    });

    it('removes a group when its chip is dismissed', async () => {
        const user = userEvent.setup();
        const { onSelectedGroupIdsChange } = renderField({ selectedGroupIds: ['group-a', 'group-m'] });

        await user.click(screen.getByRole('button', { name: 'Remove Alpha Team' }));

        expect(onSelectedGroupIdsChange).toHaveBeenCalledWith(['group-m']);
    });

    it('shows an empty state when no groups are available', async () => {
        const user = userEvent.setup();
        renderField({ groups: [] });

        await user.click(screen.getByLabelText('Search groups'));

        const searchInput = screen.getByLabelText('Search groups');
        expect(searchInput.getAttribute('data-list-empty')).not.toBeNull();
        expect(screen.getByRole('listbox').getAttribute('data-empty')).not.toBeNull();
        expect(screen.queryAllByRole('option')).toHaveLength(0);
        expect(document.querySelector('[data-slot="combobox-empty"]')?.textContent).toContain('No groups available');
    });

    it('disables the combobox while loading', () => {
        renderField({ isLoading: true });

        expect(screen.getByLabelText('Search groups').disabled).toBe(true);
    });

    it('prevents typing in the groups input', async () => {
        const user = userEvent.setup();
        renderField();

        const input = screen.getByLabelText('Search groups');
        await user.click(input);
        await user.type(input, 'alpha');

        expect((input as HTMLInputElement).value).toBe('');
    });

    it('does not change selection while loading', async () => {
        const user = userEvent.setup();
        const { onSelectedGroupIdsChange } = renderField({ isLoading: true });

        await user.click(screen.getByLabelText('Search groups'));

        expect(onSelectedGroupIdsChange).not.toHaveBeenCalled();
    });
});
