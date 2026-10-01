/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { NewEnvironmentDialog } from './NewEnvironmentDialog';

describe('NewEnvironmentDialog', () => {
    it('renders Cockpit-style create environment form', () => {
        render(<NewEnvironmentDialog open onOpenChange={() => undefined} onCreate={() => undefined} />);

        expect(screen.getByRole('heading', { name: 'Create New Environment' })).toBeTruthy();
        expect(screen.getByTestId('new-environment-name')).toBeTruthy();
        expect(screen.getByTestId('new-environment-product')).toBeTruthy();
        expect(screen.getByTestId('new-environment-hrid')).toBeTruthy();
        expect((screen.getByTestId('create-new-environment-button') as HTMLButtonElement).disabled).toBe(true);
    });

    it('auto-fills hrid from name and calls onCreate when valid', async () => {
        const user = userEvent.setup();
        const onCreate = jest.fn();

        render(<NewEnvironmentDialog open onOpenChange={() => undefined} onCreate={onCreate} />);

        await user.type(screen.getByTestId('new-environment-name'), 'Staging');
        expect((screen.getByTestId('new-environment-hrid') as HTMLInputElement).value).toBe('staging');

        await user.click(screen.getByTestId('create-new-environment-button'));

        expect(onCreate).toHaveBeenCalledWith({ name: 'Staging', hrid: 'staging', product: 'APIM' });
    });
});
