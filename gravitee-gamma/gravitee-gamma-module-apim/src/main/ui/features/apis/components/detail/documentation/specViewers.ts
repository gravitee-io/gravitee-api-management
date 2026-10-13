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
import type { SwaggerUIOptions } from 'swagger-ui';

/** What Swagger UI returns: its whole system, of which `@types/swagger-ui` leaves out the actions. */
export interface SwaggerUiInstance {
    specActions: { updateSpec(spec: string): void };
}

export type SwaggerUiFactory = (options: SwaggerUIOptions) => SwaggerUiInstance;

export interface RedocStandalone {
    init(spec: object, options: Record<string, unknown>, element: HTMLElement, callback?: (error?: unknown) => void): void;
}

// Each viewer weighs over a megabyte: they load, with their styles, only when a page of their kind is
// previewed.

export async function loadSwaggerUi(): Promise<SwaggerUiFactory> {
    return (await import('./specViewerBundles/swaggerUi')).default as unknown as SwaggerUiFactory;
}

export async function loadRedoc(): Promise<RedocStandalone> {
    return (await import('./specViewerBundles/redoc')).default;
}
