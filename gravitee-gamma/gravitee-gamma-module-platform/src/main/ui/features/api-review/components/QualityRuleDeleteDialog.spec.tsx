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

import { QualityRuleDeleteDialog } from './QualityRuleDeleteDialog';
import type { QualityRule } from '../types/qualityRule';

const RULE: QualityRule = { id: 'rule-1', name: 'Primary owner is a group', description: 'A team owns the API.', weight: 0 };

function renderDialog({ open = true, rule = RULE, isDeleting = false }: { open?: boolean; rule?: QualityRule; isDeleting?: boolean } = {}) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(<QualityRuleDeleteDialog open={open} rule={rule} onClose={onClose} onConfirm={onConfirm} isDeleting={isDeleting} />);
    return { onClose, onConfirm };
}

describe('QualityRuleDeleteDialog', () => {
    it('names the rule about to be deleted', () => {
        renderDialog();
        expect(screen.getByRole('heading', { name: 'Delete manual rule' })).toBeInTheDocument();
        expect(screen.getByText('Primary owner is a group')).toBeInTheDocument();
    });

    it('confirms and cancels', () => {
        const { onClose, onConfirm } = renderDialog();
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });

    it('blocks both buttons while deleting', () => {
        renderDialog({ isDeleting: true });
        expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });

    it('renders nothing while closed', () => {
        renderDialog({ open: false });
        expect(screen.queryByRole('heading', { name: 'Delete manual rule' })).not.toBeInTheDocument();
    });
});
