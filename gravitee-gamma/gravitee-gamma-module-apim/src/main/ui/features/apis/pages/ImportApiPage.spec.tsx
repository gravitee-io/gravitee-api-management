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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('./ImportApiForm', () => ({
    ImportApiForm: ({ format }: { format: string }) => <div>Import form for {format}</div>,
}));

import { ImportApiPage } from './ImportApiPage';

function renderPage(format: string) {
    return render(
        <MemoryRouter initialEntries={[`/apis/new/import/${format}`]}>
            <Routes>
                <Route path="apis">
                    <Route path="new">
                        <Route index element={<div>Picker page</div>} />
                        <Route path="import/:format" element={<ImportApiPage />} />
                    </Route>
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('ImportApiPage', () => {
    it.each(['gravitee', 'openapi', 'wsdl'])('renders the import form for a valid format: %s', format => {
        renderPage(format);
        expect(screen.getByText(`Import form for ${format}`)).toBeInTheDocument();
    });

    it('redirects back to the picker page for an invalid format', () => {
        renderPage('not-a-real-format');
        expect(screen.getByText('Picker page')).toBeInTheDocument();
    });
});
