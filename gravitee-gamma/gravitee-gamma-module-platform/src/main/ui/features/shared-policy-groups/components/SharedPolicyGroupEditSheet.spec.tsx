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

import { fireEvent, render, screen } from '@testing-library/react';

import { SharedPolicyGroupEditSheet } from './SharedPolicyGroupEditSheet';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth',
    prerequisiteMessage: 'Needs cache',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [],
};

function renderSheet(overrides: Partial<React.ComponentProps<typeof SharedPolicyGroupEditSheet>> = {}) {
    return render(
        <SharedPolicyGroupEditSheet
            open
            sharedPolicyGroup={SPG}
            onClose={jest.fn()}
            onSubmit={jest.fn()}
            isSaving={false}
            {...overrides}
        />,
    );
}

describe('SharedPolicyGroupEditSheet', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the edit title and pre-fills fields from the shared policy group', () => {
        renderSheet();
        expect(screen.queryByRole('heading', { name: 'Edit Policy Group' })).not.toBeNull();
        expect(screen.getByLabelText(/Name/i)).toHaveProperty('value', 'Auth Bundle');
        expect(screen.getByLabelText('Describe the purpose of this policy group')).toHaveProperty('value', 'Reusable auth');
        expect(screen.getByLabelText('Prerequisite message')).toHaveProperty('value', 'Needs cache');
    });

    it('shows API type and phase as read-only — matching classic Console edit dialog', () => {
        renderSheet();
        expect(screen.queryByText('Proxy')).not.toBeNull();
        expect(screen.queryByText('Request')).not.toBeNull();
        expect(screen.queryByRole('radio', { name: 'Proxy' })).toBeNull();
        expect(screen.queryByRole('radio', { name: 'Request' })).toBeNull();
    });

    it('disables Save when the name is cleared', () => {
        renderSheet();
        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '   ' } });
        expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);
    });

    it('submits the trimmed form values', () => {
        const onSubmit = jest.fn();
        renderSheet({ onSubmit });

        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '  Updated Name  ' } });
        fireEvent.change(screen.getByLabelText('Describe the purpose of this policy group'), { target: { value: 'New description' } });
        fireEvent.change(screen.getByLabelText('Prerequisite message'), { target: { value: 'New prerequisite' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        expect(onSubmit).toHaveBeenCalledWith({
            name: 'Updated Name',
            description: 'New description',
            prerequisiteMessage: 'New prerequisite',
        });
    });

    it('calls onClose when Cancel is clicked', () => {
        const onClose = jest.fn();
        renderSheet({ onClose });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });
});
