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

import { SharedPolicyGroupCreateSheet } from './SharedPolicyGroupCreateSheet';

function renderSheet(overrides: Partial<React.ComponentProps<typeof SharedPolicyGroupCreateSheet>> = {}) {
    return render(<SharedPolicyGroupCreateSheet open onClose={jest.fn()} onSubmit={jest.fn()} isSaving={false} {...overrides} />);
}

describe('SharedPolicyGroupCreateSheet', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the title and description', () => {
        renderSheet();
        expect(screen.queryByRole('heading', { name: 'Add Policy Group' })).not.toBeNull();
        expect(
            screen.queryByText('Policy groups can be reused across multiple APIs, so you only need to configure their policies once.'),
        ).not.toBeNull();
    });

    it('renders the Basic information and Scope sections', () => {
        renderSheet();
        expect(screen.queryByText('Basic information')).not.toBeNull();
        expect(screen.queryByText('Scope')).not.toBeNull();
    });

    it('shows the field placeholders and character-limit hints', () => {
        renderSheet();
        expect(screen.getByPlaceholderText('e.g. Default authentication')).not.toBeNull();
        expect(screen.getByPlaceholderText('Describe what this policy group is used for')).not.toBeNull();
        expect(screen.getAllByText('300 characters max.')).toHaveLength(2);
    });

    it('defaults to Proxy, offering only the phases valid for a Proxy API', () => {
        renderSheet();
        expect(screen.getByRole('radio', { name: 'Proxy' }).getAttribute('aria-checked')).toBe('true');
        expect(screen.queryByRole('radio', { name: 'Request' })).not.toBeNull();
        expect(screen.queryByRole('radio', { name: 'Response' })).not.toBeNull();
        expect(screen.queryByRole('radio', { name: 'Publish' })).toBeNull();
    });

    it('switches to Message-specific phases when Message is selected', () => {
        renderSheet();
        fireEvent.click(screen.getByRole('radio', { name: 'Message' }));
        expect(screen.queryByRole('radio', { name: 'Publish' })).not.toBeNull();
        expect(screen.queryByRole('radio', { name: 'Subscribe' })).not.toBeNull();
    });

    it('disables Create until a name is entered', () => {
        renderSheet();
        expect(screen.getByRole('button', { name: 'Create' })).toHaveProperty('disabled', true);
        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My SPG' } });
        expect(screen.getByRole('button', { name: 'Create' })).toHaveProperty('disabled', false);
    });

    it('submits the trimmed form values, including api type, description, prerequisite message, and selected phase', () => {
        const onSubmit = jest.fn();
        renderSheet({ onSubmit });

        fireEvent.click(screen.getByRole('radio', { name: 'Message' }));
        fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: '  My SPG  ' } });
        fireEvent.change(screen.getByLabelText('Describe the purpose of this policy group'), { target: { value: 'A bundle' } });
        fireEvent.change(screen.getByLabelText('Prerequisite message'), { target: { value: 'Needs cache' } });
        fireEvent.click(screen.getByRole('radio', { name: 'Publish' }));
        fireEvent.click(screen.getByRole('button', { name: 'Create' }));

        expect(onSubmit).toHaveBeenCalledWith({
            name: 'My SPG',
            description: 'A bundle',
            prerequisiteMessage: 'Needs cache',
            apiType: 'MESSAGE',
            phase: 'PUBLISH',
        });
    });

    it('calls onClose when Cancel is clicked', () => {
        const onClose = jest.fn();
        renderSheet({ onClose });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });
});
