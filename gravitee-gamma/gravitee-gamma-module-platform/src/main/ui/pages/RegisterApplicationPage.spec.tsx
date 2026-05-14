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
import { MemoryRouter } from 'react-router-dom';

import { RegisterApplicationPage } from './RegisterApplicationPage';

jest.mock('../features/applications/components/create', () => ({
    RegisterApplicationForm: () => <div data-testid="register-form" />,
}));

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/applications/new']}>
            <RegisterApplicationPage />
        </MemoryRouter>,
    );
}

describe('RegisterApplicationPage', () => {
    it('renders the page header and registration form', () => {
        renderPage();

        expect(screen.queryByText('Application creation')).not.toBeNull();
        expect(screen.queryByTestId('register-form')).not.toBeNull();
    });
});
