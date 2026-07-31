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

/**
 * React Router basename derived from the document base href.
 *
 * `<base href>` is set at runtime by nginx from `GAMMA_CONSOLE_BASE_HREF` so the console can be served
 * under a sub-path (e.g. `/gravitee-gamma/`). React Router does not read `<base href>` itself, so we
 * derive the basename to keep client-side routes under the same path as the assets. It expects a
 * leading slash and no trailing one, so `/gravitee-gamma/` becomes `/gravitee-gamma` and root stays `/`.
 */
export function resolveRouterBasename(baseURI: string = document.baseURI): string {
    return new URL(baseURI).pathname.replace(/\/$/, '') || '/';
}
