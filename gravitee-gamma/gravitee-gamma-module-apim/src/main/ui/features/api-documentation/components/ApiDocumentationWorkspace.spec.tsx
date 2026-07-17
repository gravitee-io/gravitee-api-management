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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { installFakeIndexedDB, resetFakeIndexedDB } from '@apim/portal-editor/testing/fake-indexeddb';
import { ApiDocumentationWorkspace } from './ApiDocumentationWorkspace';

jest.mock('@apim/portal-editor/blocks/ApiSpecBlock/api-ref-page-generator', () => ({
    buildTagPageDefinitions: jest.fn(async () => ({
        overviewDocument: [{ id: 'block-1', type: 'paragraph', content: [], children: [] }],
        tagPages: [],
    })),
}));

jest.mock('@apim/portal-editor/editor/gmd/gmd-content', () => ({
    serializeDocumentToGmd: jest.fn(() => ''),
}));

jest.mock('@apim/portal-editor/portal-shell/components/ContentArea', () => ({
    ContentArea: () => <div data-testid="content-area">Content area</div>,
}));

jest.mock('@apim/portal-editor/portals/storage/portals.storage', () => ({
    seedPortalsIfEmpty: jest.fn(async () => [{ id: 'portal-1', name: 'Payments Portal' }]),
}));

installFakeIndexedDB();

describe('ApiDocumentationWorkspace', () => {
    beforeEach(() => {
        resetFakeIndexedDB();
        localStorage.clear();
    });

    it('should render the documentation shell with navigation and content', async () => {
        renderWithGraphene(<ApiDocumentationWorkspace apiId="api-1" apiName="Payments API" />);

        await waitFor(() => {
            expect(screen.getByRole('heading', { name: /^documentation$/i })).toBeInTheDocument();
        });

        expect(screen.getByText(/build and publish documentation for payments api/i)).toBeInTheDocument();
        expect(screen.getByRole('navigation', { name: /portal navigation/i })).toBeInTheDocument();
        expect(screen.getByTestId('content-area')).toBeInTheDocument();
        expect(screen.getByRole('group', { name: /editor mode/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /open designer/i })).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/target portal/i)).not.toBeInTheDocument();
    });

    it('should open the publish dialog when Publish is clicked', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<ApiDocumentationWorkspace apiId="api-1" apiName="Payments API" />);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /^publish$/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole('button', { name: /^publish$/i }));

        expect(screen.getByRole('dialog', { name: /publish to portal/i })).toBeInTheDocument();
    });
});
