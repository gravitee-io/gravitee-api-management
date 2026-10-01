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

import { CloudCustomReportersPage } from './CloudCustomReportersPage';

describe('CloudCustomReportersPage', () => {
    it('renders Cockpit-style custom reporters table', () => {
        render(<CloudCustomReportersPage />);

        expect(screen.getByRole('heading', { name: 'Custom Reporters' })).toBeTruthy();
        expect(screen.getByText('Manage and configure your custom API gateway reporters.')).toBeTruthy();
        expect(screen.getByText('Your Custom Reporters')).toBeTruthy();
        expect(screen.getByRole('link', { name: 'View documentation' })).toBeTruthy();
        expect(screen.getByTestId('create-custom-reporter')).toBeTruthy();
        expect(screen.getByText('Config 1')).toBeTruthy();
        expect(screen.getByText('Config 2')).toBeTruthy();
        expect(screen.getAllByText('TCP Reporter').length).toBe(2);
        expect(screen.getAllByText('JSON').length).toBe(2);
        expect(screen.getByText('Active')).toBeTruthy();
        expect(screen.getByText('Not Linked')).toBeTruthy();
    });
});
