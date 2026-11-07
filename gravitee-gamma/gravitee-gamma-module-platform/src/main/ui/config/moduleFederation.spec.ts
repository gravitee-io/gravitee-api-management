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
import config from '../../../../module-federation.config';

function sharing(libraryName: string) {
    const { shared } = config;
    if (typeof shared !== 'function') throw new Error('shared is not a function');
    return shared(libraryName, { singleton: true, requiredVersion: '^1.0.0' });
}

describe('platform module federation', () => {
    it('shares the host singletons', () => {
        for (const library of ['react', 'react-dom', 'react-router-dom', '@gravitee/graphene-core', '@monaco-editor/react']) {
            expect(sharing(library)).toMatchObject({ singleton: true, strictVersion: false });
        }
    });

    it('bundles the observability library and graphene-charts: react-query is shared by no module', () => {
        expect(sharing('@gravitee/gamma-lib-observability')).toBe(false);
        expect(sharing('@gravitee/graphene-charts')).toBe(false);
        expect(sharing('@tanstack/react-query')).toBe(false);
    });
});
