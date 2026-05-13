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

import { ApiDetailPlaceholderPage } from './ApiDetailPlaceholderPage';

describe('ApiDetailPlaceholderPage', () => {
    it('renders the title prop as the page heading', () => {
        render(<ApiDetailPlaceholderPage title="Entrypoints" />);
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Entrypoints');
    });

    it('renders multi-word titles correctly', () => {
        render(<ApiDetailPlaceholderPage title="Policy Studio" />);
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Policy Studio');
    });

    it('renders exact titles including acronyms and compound phrases', () => {
        render(<ApiDetailPlaceholderPage title="Deployment Configuration" />);
        expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Deployment Configuration');
    });

    it('always shows "Coming soon." subtitle', () => {
        render(<ApiDetailPlaceholderPage title="General" />);
        expect(screen.getByText('Coming soon.')).toBeInTheDocument();
    });
});
