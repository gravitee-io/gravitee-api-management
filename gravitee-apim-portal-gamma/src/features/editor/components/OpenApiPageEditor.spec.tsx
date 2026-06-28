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
jest.mock('../../../blocks/OpenApiBlock/OpenApiRendererView', () => ({
    OpenApiRendererView: () => <div data-testid="openapi-renderer-view" />,
}));

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';

import { PETSTORE_OPENAPI_SPEC } from '../services/openapi.service';
import { OpenApiPageEditor } from './OpenApiPageEditor';

describe('OpenApiPageEditor', () => {
    const page = {
        id: 'page-openapi',
        portalId: 'portal-1',
        title: 'Petstore',
        type: 'PAGE' as const,
        contentType: 'OPENAPI' as const,
        renderer: 'swagger' as const,
        specSource: { type: 'INLINE' as const, content: PETSTORE_OPENAPI_SPEC },
        parentId: null,
        order: 0,
        slug: 'petstore',
    };

    const content = {
        id: 'content-1',
        portalId: 'portal-1',
        navigationItemId: 'page-openapi',
        contentType: 'OPENAPI' as const,
        renderer: 'swagger' as const,
        specContent: PETSTORE_OPENAPI_SPEC,
    };

    it('should render source and renderer controls', () => {
        renderWithGraphene(
            <OpenApiPageEditor page={page} content={content} navItems={[]} onSave={jest.fn().mockResolvedValue(undefined)} />,
        );

        expect(screen.getByLabelText('Source')).toBeInTheDocument();
        expect(screen.getByLabelText('Renderer')).toBeInTheDocument();
        expect(screen.getByText('Valid OpenAPI spec')).toBeInTheDocument();
        expect(screen.getByTestId('openapi-renderer-view')).toBeInTheDocument();
    });
});
