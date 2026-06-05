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

import { ApplicationSubscriptionEditRequestSheet } from './ApplicationSubscriptionEditRequestSheet';
import { querySheetHeading } from '../test/sheetSpecHelpers';

function renderSheet(open = true) {
    const onOpenChange = jest.fn();
    const onSave = jest.fn();
    render(
        <ApplicationSubscriptionEditRequestSheet
            open={open}
            initialRequest="Please approve"
            onOpenChange={onOpenChange}
            onSave={onSave}
            isLoading={false}
        />,
    );
    return { onOpenChange, onSave };
}

describe('ApplicationSubscriptionEditRequestSheet', () => {
    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(querySheetHeading('Edit subscription message')).toBeNull();
    });

    it('shows sheet title when open', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Edit subscription message' })).not.toBeNull();
    });

    it('invokes onOpenChange(false) when Cancel is clicked', () => {
        const { onOpenChange } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('invokes onSave with trimmed request when Save changes is clicked', () => {
        const { onSave } = renderSheet(true);
        fireEvent.change(screen.getByLabelText(/Publisher message/i), { target: { value: '  Updated message  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));
        expect(onSave).toHaveBeenCalledWith('Updated message');
    });
});
