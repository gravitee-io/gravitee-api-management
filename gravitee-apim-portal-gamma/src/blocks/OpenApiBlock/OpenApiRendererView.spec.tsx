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

import { PETSTORE_OPENAPI_SPEC } from '../../features/editor/services/openapi.service';

import { OpenApiRendererView } from './OpenApiRendererView';

jest.mock('./SwaggerRenderer', () => ({
    SwaggerRenderer: () => <div data-testid="swagger-renderer" />,
}));

jest.mock('./RedocRenderer', () => ({
    RedocRenderer: () => <div data-testid="redoc-renderer" />,
}));

jest.mock('./GraviteeDocsRenderer', () => ({
    GraviteeDocsRenderer: () => <div data-testid="gravitee-docs-renderer" />,
}));

describe('OpenApiRendererView', () => {
    it('should render gravitee docs renderer when renderer is gravitee', () => {
        render(<OpenApiRendererView renderer="gravitee" specContent={PETSTORE_OPENAPI_SPEC} />);

        expect(screen.getByTestId('gravitee-docs-renderer')).toBeInTheDocument();
    });

    it('should render swagger renderer by default', () => {
        render(<OpenApiRendererView renderer="swagger" specContent={PETSTORE_OPENAPI_SPEC} />);

        expect(screen.getByTestId('swagger-renderer')).toBeInTheDocument();
    });
});
