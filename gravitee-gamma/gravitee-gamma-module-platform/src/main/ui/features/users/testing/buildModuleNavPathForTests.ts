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

/** Mirrors {@link buildModuleNavPath} from `@gravitee/gamma-modules-sdk/routing` for Jest specs. */
export function buildModuleNavPathForTests(moduleId: string, routePath: string, currentPathname: string): string {
    if (moduleId) {
        const environmentRootEnd = currentPathname.indexOf('/', 14);
        return currentPathname.startsWith('/environments/') && environmentRootEnd > 0
            ? `${currentPathname.slice(0, environmentRootEnd)}/${moduleId}/${routePath}`
            : `/${moduleId}/${routePath}`;
    }
    return `/${routePath}`;
}

export { buildModuleNavPathForTests as buildModuleNavPath };
