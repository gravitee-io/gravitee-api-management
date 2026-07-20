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
import type { OpenApiRenderer, PortalDocumentationViewer } from '../../portals/types';
import { DEFAULT_DOCUMENTATION_VIEWER, PORTAL_DOCUMENTATION_VIEWERS } from '../../portals/types';

/** Maps portal documentation viewer setting to the OpenAPI renderer used at runtime. */
export function mapDocumentationViewerToOpenApiRenderer(
    viewer: PortalDocumentationViewer | undefined,
): OpenApiRenderer {
    const normalized =
        viewer && PORTAL_DOCUMENTATION_VIEWERS.includes(viewer) ? viewer : DEFAULT_DOCUMENTATION_VIEWER;

    switch (normalized) {
        case 'redoc':
            return 'redoc';
        case 'in-house':
            return 'gravitee';
        case 'swagger':
        default:
            return 'swagger';
    }
}
