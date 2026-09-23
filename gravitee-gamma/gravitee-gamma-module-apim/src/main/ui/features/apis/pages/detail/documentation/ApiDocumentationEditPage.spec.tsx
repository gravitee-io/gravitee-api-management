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
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDocumentationEditPage } from './ApiDocumentationEditPage';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import {
    useApiDocumentationPage,
    useApiDocumentationPages,
    useCreateDocumentationPage,
    useDocumentationFetchers,
    useUpdateDocumentationPage,
} from '../../../hooks/useApiDocumentation';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

jest.mock('../../../hooks/useApiDocumentation', () => ({
    useApiDocumentationPage: jest.fn(),
    useApiDocumentationPages: jest.fn(),
    useCreateDocumentationPage: jest.fn(),
    useUpdateDocumentationPage: jest.fn(),
    useDocumentationFetchers: jest.fn(),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseApiDetailContext = useApiDetailContext as jest.Mock;
const mockUsePage = useApiDocumentationPage as jest.Mock;
const mockUsePages = useApiDocumentationPages as jest.Mock;
const mockUseCreate = useCreateDocumentationPage as jest.Mock;
const mockUseUpdate = useUpdateDocumentationPage as jest.Mock;
const mockUseFetchers = useDocumentationFetchers as jest.Mock;

function mutationMock(mutate = jest.fn()) {
    return { mutate, isPending: false };
}

function renderCreate(search = '?pageType=MARKDOWN&parentId=ROOT') {
    return render(
        <MemoryRouter initialEntries={[`/apis/api-1/documentation/new${search}`]}>
            <Routes>
                <Route path="/apis/:apiId/documentation/new" element={<ApiDocumentationEditPage />} />
                <Route path="/apis/:apiId/documentation" element={<div>list</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

function renderEdit() {
    return render(
        <MemoryRouter initialEntries={['/apis/api-1/documentation/page-1']}>
            <Routes>
                <Route path="/apis/:apiId/documentation/:pageId" element={<ApiDocumentationEditPage />} />
                <Route path="/apis/:apiId/documentation" element={<div>list</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiDocumentationEditPage', () => {
    beforeEach(() => {
        mockUseApiDetailContext.mockReturnValue({
            api: { id: 'api-1', name: 'Orders', type: 'PROXY', definitionVersion: 'V4' },
            isLoading: false,
            permissionsReady: true,
        });
        mockUsePage.mockReturnValue({ data: undefined, isLoading: false, isError: false });
        mockUsePages.mockReturnValue({ pages: [], breadcrumbs: [], isLoading: false, isError: false });
        mockUseCreate.mockReturnValue(mutationMock());
        mockUseUpdate.mockReturnValue(mutationMock());
        mockUseFetchers.mockReturnValue({ data: [] });
    });

    it('creates a markdown page through the wizard', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseCreate.mockReturnValue(mutationMock(mutate));
        renderCreate();

        await user.type(screen.getByRole('textbox', { name: /name/i }), 'Getting started');
        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(screen.getByText('Fill in the content myself')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.type(screen.getByLabelText('Page content'), '# Hello');
        await user.click(screen.getByRole('button', { name: /^Save$/ }));

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                type: 'MARKDOWN',
                name: 'Getting started',
                content: '# Hello',
                parentId: 'ROOT',
            }),
            expect.any(Object),
        );
    });

    it('shows OpenAPI as the page type without the classic Try-it panel', () => {
        renderCreate('?pageType=SWAGGER');
        expect(screen.getAllByText('OpenAPI').length).toBeGreaterThan(0);
        expect(screen.queryByText('OpenAPI configuration')).not.toBeInTheDocument();
        expect(screen.queryByText('Try-it')).not.toBeInTheDocument();
    });

    it('loads an existing page and saves content changes', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn();
        mockUseUpdate.mockReturnValue(mutationMock(mutate));
        mockUsePage.mockReturnValue({
            data: {
                id: 'page-1',
                name: 'Overview',
                type: 'MARKDOWN',
                content: 'old',
                visibility: 'PRIVATE',
                parentId: 'ROOT',
                published: true,
            },
            isLoading: false,
            isError: false,
        });
        renderEdit();

        expect(screen.getByRole('heading', { name: 'Overview' })).toBeInTheDocument();
        const editor = screen.getByLabelText('Page content');
        await user.clear(editor);
        await user.type(editor, 'new content');
        await user.click(screen.getByRole('button', { name: /save changes/i }));

        expect(mutate).toHaveBeenCalledWith(
            expect.objectContaining({
                pageId: 'page-1',
                payload: expect.objectContaining({ name: 'Overview', type: 'MARKDOWN', content: 'new content' }),
            }),
            expect.any(Object),
        );
    });

    it('treats an external source page as a read-only preview', () => {
        mockUsePage.mockReturnValue({
            data: {
                id: 'page-1',
                name: 'Spec',
                type: 'SWAGGER',
                content: '{}',
                source: { type: 'http-fetcher', configuration: {} },
            },
            isLoading: false,
            isError: false,
        });
        renderEdit();
        expect(screen.getByText(/cannot be edited because the page is linked to an external source/i)).toBeInTheDocument();
        expect(screen.getByLabelText('Page content')).toBeDisabled();
        expect(screen.queryByRole('button', { name: /refresh content/i })).not.toBeInTheDocument();
    });
});
