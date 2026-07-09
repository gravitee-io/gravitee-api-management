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
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';

jest.mock('@gravitee/graphene-core', () => ({
    Alert: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    AlertDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    AlertTitle: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick }: { children?: ReactNode; onClick?: () => void }) => (
        <button type="button" onClick={onClick}>
            {children}
        </button>
    ),
    Collapsible: ({ children, open }: { children?: ReactNode; open?: boolean }) => (open ? <>{children}</> : null),
    CollapsibleContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Separator: () => <hr />,
    cn: (...args: (string | boolean | undefined)[]) => args.filter(Boolean).join(' '),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { CreateApiProxyPage } from './CreateApiProxyPage';

function ImportFormStub() {
    const { format } = useParams<{ format: string }>();
    return <div>Import form page for {format}</div>;
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/apis/new']}>
            <Routes>
                <Route path="apis">
                    <Route index element={<div>APIs list page</div>} />
                    <Route path="new">
                        <Route index element={<CreateApiProxyPage />} />
                        <Route path="scratch" element={<div>Scratch wizard page</div>} />
                        <Route path="template/:id" element={<div>Template wizard page</div>} />
                        <Route path="import/:format" element={<ImportFormStub />} />
                    </Route>
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('CreateApiProxyPage', () => {
    it('renders the three top-level picker cards', () => {
        renderPage();

        expect(screen.getByRole('button', { name: /start from scratch/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /quick-start templates/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^import api/i })).toBeInTheDocument();
    });

    it('hides the import format sub-cards until Import API is expanded', () => {
        renderPage();

        expect(screen.queryByRole('button', { name: 'Gravitee definition' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'OpenAPI specification' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'WSDL' })).toBeNull();

        fireEvent.click(screen.getByRole('button', { name: /^import api/i }));

        expect(screen.getByRole('button', { name: 'Gravitee definition' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'OpenAPI specification' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'WSDL' })).toBeInTheDocument();
    });

    it.each([
        ['Gravitee definition', 'gravitee'],
        ['OpenAPI specification', 'openapi'],
        ['WSDL', 'wsdl'],
    ])('navigates to the import page with the right format for %s', (name, format) => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^import api/i }));

        fireEvent.click(screen.getByRole('button', { name }));

        expect(screen.getByText(`Import form page for ${format}`)).toBeInTheDocument();
    });

    it('navigates to scratch and template routes unaffected by the new card', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /start from scratch/i }));
        expect(screen.getByText('Scratch wizard page')).toBeInTheDocument();
    });

    it('navigates back to the APIs list when Cancel is clicked', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
        expect(screen.getByText('APIs list page')).toBeInTheDocument();
    });
});
