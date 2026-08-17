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
import { buildPathnameAfterEnvironmentChange, hostNavPath, pathSegmentsAfterEnvironment, resolveHostRoute } from './routes';

describe('hostNavPath', () => {
    it('should build home path under environment', () => {
        expect(hostNavPath('home', 'default')).toBe('/environments/default/home');
    });
});

describe('resolveHostRoute', () => {
    it('should resolve home for /environments/:envHrid/home', () => {
        const { activeNavKey, breadcrumbSegments } = resolveHostRoute('/environments/my-env/home', 'my-env');
        expect(activeNavKey).toBe('home');
        expect(breadcrumbSegments[0]?.label).toBe('Home');
    });

    it('should use default when pathname env does not match param', () => {
        const { activeNavKey } = resolveHostRoute('/environments/other/home', 'my-env');
        expect(activeNavKey).toBe('home');
    });
});

describe('pathSegmentsAfterEnvironment', () => {
    it('should return segments under the given environment', () => {
        expect(pathSegmentsAfterEnvironment('/environments/a/apim/nested', 'a')).toEqual(['apim', 'nested']);
        expect(pathSegmentsAfterEnvironment('/environments/a/home', 'a')).toEqual(['home']);
    });

    it('should return empty when pathname does not start with the environment prefix', () => {
        expect(pathSegmentsAfterEnvironment('/environments/b/apim', 'a')).toEqual([]);
    });
});

describe('buildPathnameAfterEnvironmentChange', () => {
    it('should drop the module sub-path and land on the module root', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/env-1/some-module/apis/list', 'env-1', 'env-2')).toBe(
            '/environments/env-2/some-module',
        );
    });

    it('should keep the module root for any module id, including unknown ones', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/env-1/aim/catalog/models/m-1', 'env-1', 'env-2')).toBe(
            '/environments/env-2/aim',
        );
    });

    it('should drop the sub-path of host areas too', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/a/tasks/task-1', 'a', 'b')).toBe('/environments/b/tasks');
    });

    it('should use home when the URL is only the environment base', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/env-1', 'env-1', 'env-2')).toBe('/environments/env-2/home');
    });

    it('should use home when the pathname is not under the current environment', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/other/apim/apis', 'env-1', 'env-2')).toBe('/environments/env-2/home');
    });

    it('should keep host home when switching', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/a/home', 'a', 'b')).toBe('/environments/b/home');
    });

    it('should keep the module root when already on it', () => {
        expect(buildPathnameAfterEnvironmentChange('/environments/a/apim', 'a', 'b')).toBe('/environments/b/apim');
    });
});
