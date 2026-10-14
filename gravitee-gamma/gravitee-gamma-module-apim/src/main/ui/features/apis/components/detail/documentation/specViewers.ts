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
import type { AsyncApiProps } from '@asyncapi/react-component';
import type { ComponentType } from 'react';
import type { Redoc } from 'redoc';
import type { SwaggerUIOptions } from 'swagger-ui';

import type { createRedocStore } from './specViewerBundles/redoc';

/** What Swagger UI returns: its whole system, of which `@types/swagger-ui` leaves out the actions. */
export interface SwaggerUiInstance {
    specActions: { updateSpec(spec: string): void };
}

export type SwaggerUiFactory = (options: SwaggerUIOptions) => SwaggerUiInstance;

export interface RedocLibrary {
    createRedocStore: typeof createRedocStore;
    Redoc: typeof Redoc;
}

export type AsyncApiViewer = ComponentType<AsyncApiProps>;

// Each viewer weighs one to three megabytes: they load, with their styles, only when a page of their kind is
// previewed.

export async function loadSwaggerUi(): Promise<SwaggerUiFactory> {
    return (await import('./specViewerBundles/swaggerUi')).default as unknown as SwaggerUiFactory;
}

export async function loadRedoc(): Promise<RedocLibrary> {
    return import('./specViewerBundles/redoc');
}

export async function loadAsyncApiViewer(): Promise<AsyncApiViewer> {
    return (await import('./specViewerBundles/asyncApi')).default;
}
