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

import { AccessManagementPage } from './AccessManagementPage';

const mockNotifySuccess = jest.fn();
jest.mock('../shared/notify', () => ({
    notify: {
        success: (message: string) => mockNotifySuccess(message),
    },
}));

// Stub the heavy connection panel; expose its onSaved so we can assert the page wires a toast.
jest.mock('../features/access-management/components/AmConfigPanel', () => ({
    AmConfigPanel: ({ onSaved }: { onSaved: (cfg: unknown) => void }) => (
        <button type="button" data-testid="save" onClick={() => onSaved({})}>
            save
        </button>
    ),
}));

describe('AccessManagementPage', () => {
    beforeEach(() => mockNotifySuccess.mockClear());

    it('renders the page header and the connection panel', () => {
        render(<AccessManagementPage />);

        expect(screen.queryByText('Access Management')).not.toBeNull();
        expect(screen.queryByTestId('save')).not.toBeNull();
    });

    it('shows a success toast when the panel reports a save', () => {
        render(<AccessManagementPage />);

        fireEvent.click(screen.getByTestId('save'));

        expect(mockNotifySuccess).toHaveBeenCalledWith('Gravitee Access Management settings saved');
    });
});
