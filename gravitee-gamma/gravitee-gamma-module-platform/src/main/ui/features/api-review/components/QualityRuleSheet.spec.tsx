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

import { QualityRuleSheet } from './QualityRuleSheet';
import type { QualityRule } from '../types/qualityRule';

const EXISTING_RULE: QualityRule = { id: 'rule-1', name: 'Primary owner is a group', description: 'A team owns the API.', weight: 0 };

function renderSheet({ open = true, rule, isSaving = false }: { open?: boolean; rule?: QualityRule; isSaving?: boolean } = {}) {
    const onClose = jest.fn();
    const onSubmit = jest.fn();
    render(<QualityRuleSheet open={open} rule={rule} onClose={onClose} onSubmit={onSubmit} isSaving={isSaving} />);
    return { onClose, onSubmit };
}

describe('QualityRuleSheet', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('renders nothing while closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByRole('heading', { name: 'New manual rule' })).not.toBeInTheDocument();
    });

    it('starts empty in create mode and keeps Create disabled until both fields are filled', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'New manual rule' })).toBeInTheDocument();
        const create = screen.getByRole('button', { name: 'Create rule' });
        expect(create).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/rule name/i), { target: { value: 'Breaking changes are documented' } });
        expect(create).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'Any incompatible change is called out.' } });
        expect(create).toBeEnabled();
    });

    it('submits trimmed values', () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/rule name/i), { target: { value: '  Breaking changes  ' } });
        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: '  Called out.  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create rule' }));
        expect(onSubmit).toHaveBeenCalledWith({ name: 'Breaking changes', description: 'Called out.' });
    });

    it('caps the fields at the backend limits and shows the counters', () => {
        renderSheet();
        expect(screen.getByLabelText(/rule name/i)).toHaveAttribute('maxlength', '64');
        expect(screen.getByLabelText(/description/i)).toHaveAttribute('maxlength', '256');
        fireEvent.change(screen.getByLabelText(/rule name/i), { target: { value: 'abc' } });
        expect(screen.getByText('3/64')).toBeInTheDocument();
        expect(screen.getByText('0/256')).toBeInTheDocument();
    });

    it('pre-fills the rule in edit mode and only enables Save once something changed', () => {
        const { onSubmit } = renderSheet({ rule: EXISTING_RULE });
        expect(screen.getByRole('heading', { name: 'Edit manual rule' })).toBeInTheDocument();
        expect(screen.getByLabelText(/rule name/i)).toHaveValue('Primary owner is a group');
        const save = screen.getByRole('button', { name: 'Save changes' });
        expect(save).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'A team owns the API and its on-call.' } });
        expect(save).toBeEnabled();
        fireEvent.click(save);
        expect(onSubmit).toHaveBeenCalledWith({ name: 'Primary owner is a group', description: 'A team owns the API and its on-call.' });
    });

    it('shows the saving label and blocks the buttons while saving', () => {
        const { onClose } = renderSheet({ isSaving: true });
        expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
        expect(onClose).not.toHaveBeenCalled();
    });

    it('calls onClose from Cancel', () => {
        const { onClose } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });
});
