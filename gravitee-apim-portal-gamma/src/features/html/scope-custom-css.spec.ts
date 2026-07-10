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
import { scopeCustomCss } from './scope-custom-css';

describe('scopeCustomCss', () => {
    it('should prefix selectors with scope', () => {
        const css = '.my-card { background: red; }\nh1 { color: blue; }';
        const scoped = scopeCustomCss(css, '[data-block-scope="test"]');

        expect(scoped).toContain('[data-block-scope="test"] .my-card { background: red; }');
        expect(scoped).toContain('[data-block-scope="test"] h1 { color: blue; }');
    });

    it('should skip :root selectors', () => {
        const css = ':root { --x: 1; }\n.foo { color: red; }';
        const scoped = scopeCustomCss(css, '[data-block-scope="test"]');

        expect(scoped).not.toContain(':root');
        expect(scoped).toContain('[data-block-scope="test"] .foo { color: red; }');
    });
});
