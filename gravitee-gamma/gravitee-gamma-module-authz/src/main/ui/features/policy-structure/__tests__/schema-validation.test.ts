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
import { describe, expect, it } from 'vitest';
import { schemaDiagnostics } from '../schema-validation';

describe('schemaDiagnostics', () => {
    it('returns no diagnostics for a valid schema', () => {
        expect(schemaDiagnostics('entity User {};')).toEqual([]);
    });
    it('flags an action missing appliesTo', () => {
        expect(schemaDiagnostics('action "x" { principal: [User] };').length).toBeGreaterThan(0);
    });
    it('treats blank as a diagnostic', () => {
        expect(schemaDiagnostics('   ').length).toBeGreaterThan(0);
    });
    it('returns no diagnostics for a valid multi-entity schema', () => {
        const src = `entity Group {
  name: String
};
entity User in [Group] {
  name: String,
  email: String
};`;
        expect(schemaDiagnostics(src)).toEqual([]);
    });
    it('returns no diagnostics for a valid action with appliesTo', () => {
        const src = `entity User {};
entity MCPServer {};
action "can_invoke" appliesTo {
  principal: [User],
  resource: [MCPServer]
};`;
        expect(schemaDiagnostics(src)).toEqual([]);
    });
    it('flags an entity with a syntax error', () => {
        expect(schemaDiagnostics('entity {').length).toBeGreaterThan(0);
    });
});
