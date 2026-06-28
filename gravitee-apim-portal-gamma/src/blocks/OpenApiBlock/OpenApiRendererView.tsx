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
import type { OpenApiRenderer } from '../../features/portals/types';
import { normalizeOpenApiRenderer } from '../../features/portal-shell/utils/page-type-options';
import { RedocRenderer } from './RedocRenderer';
import { SwaggerRenderer } from './SwaggerRenderer';

interface OpenApiRendererViewProps {
    readonly renderer: OpenApiRenderer;
    readonly specContent: string;
}

export function OpenApiRendererView({ renderer, specContent }: OpenApiRendererViewProps) {
    if (!specContent.trim()) {
        return <p>No OpenAPI spec content to display.</p>;
    }

    switch (normalizeOpenApiRenderer(renderer)) {
        case 'redoc':
            return <RedocRenderer specContent={specContent} />;
        case 'swagger':
        default:
            return <SwaggerRenderer specContent={specContent} />;
    }
}
