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

import { SharedPolicyGroupHistoryCompareDialog } from './SharedPolicyGroupHistoryCompareDialog';
import { SharedPolicyGroupHistoryJsonDialog } from './SharedPolicyGroupHistoryJsonDialog';
import { WIDE_DIALOG_STYLE } from '../../../shared/layout/dialogLayout';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

const VERSION_ONE: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Original Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    version: 1,
    steps: [{ policy: 'jwt', enabled: true }],
};

const VERSION_TWO: SharedPolicyGroup = {
    ...VERSION_ONE,
    name: 'Auth Bundle',
    version: 2,
    steps: [{ policy: 'jwt', enabled: false }],
};

describe('Shared Policy Group history dialogs', () => {
    it('renders an accessible read-only JSON source', () => {
        render(<SharedPolicyGroupHistoryJsonDialog sharedPolicyGroup={VERSION_ONE} onOpenChange={jest.fn()} />);

        const dialog = screen.getByRole('dialog', { name: 'Version 1 JSON Source' });
        expect(dialog.style.width).toBe(WIDE_DIALOG_STYLE.width);
        expect(screen.getByLabelText('Shared Policy Group JSON source').textContent).toContain('"name": "Original Auth Bundle"');
    });

    it('labels added and removed diff lines independently of color', () => {
        render(<SharedPolicyGroupHistoryCompareDialog open left={VERSION_ONE} right={VERSION_TWO} onOpenChange={jest.fn()} />);

        const dialog = screen.getByRole('dialog', { name: 'Comparing version 1 with version 2' });
        expect(dialog.style.width).toBe(WIDE_DIALOG_STYLE.width);
        expect(screen.getAllByLabelText('Removed lines').length).toBeGreaterThan(0);
        expect(screen.getAllByLabelText('Added lines').length).toBeGreaterThan(0);
    });

    it('faces each removed line with the added line that replaced it', () => {
        render(<SharedPolicyGroupHistoryCompareDialog open left={VERSION_ONE} right={VERSION_TWO} onOpenChange={jest.fn()} />);

        expect(screen.getByText('Version 1')).not.toBeNull();
        expect(screen.getByText('Version 2')).not.toBeNull();

        const removedName = screen.getByText('"name": "Original Auth Bundle",');
        const addedName = screen.getByText('"name": "Auth Bundle",');
        expect(removedName.parentElement).toBe(addedName.parentElement);
        expect(removedName.getAttribute('aria-label')).toBe('Removed lines');
        expect(addedName.getAttribute('aria-label')).toBe('Added lines');
    });

    it('identifies the current pending configuration in the comparison title', () => {
        render(
            <SharedPolicyGroupHistoryCompareDialog
                open
                left={VERSION_TWO}
                right={{ ...VERSION_TWO, lifecycleState: 'PENDING' }}
                rightIsPending
                onOpenChange={jest.fn()}
            />,
        );

        expect(screen.getByRole('dialog', { name: 'Comparing version 2 with version to be deployed' })).not.toBeNull();
    });
});
