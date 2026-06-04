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
import { classifyEntity, isResourceCategory } from '../entity-types';

const NONE: ReadonlySet<string> = new Set();

describe('classifyEntity', () => {
    it('classifies a type used as a principal in an action as principal', () => {
        expect(classifyEntity('Subject', new Set(['Subject']), NONE)).toBe('principal');
    });

    it('classifies an unknown type used as a resource as the generic resource category', () => {
        expect(classifyEntity('Report', NONE, new Set(['Report']))).toBe('resource');
    });

    it('keeps the specific service category for a known resource type used as a resource', () => {
        expect(classifyEntity('MCPServer', NONE, new Set(['MCPServer']))).toBe('mcp');
    });

    it('falls back to the built-in name map when the type is not referenced by any action', () => {
        expect(classifyEntity('Group', NONE, NONE)).toBe('principal');
        expect(classifyEntity('API', NONE, NONE)).toBe('api');
    });

    it('falls back to custom for an unreferenced, unknown type', () => {
        expect(classifyEntity('Widget', NONE, NONE)).toBe('custom');
    });

    it('prefers principal when a type is used on both sides', () => {
        expect(classifyEntity('Agent', new Set(['Agent']), new Set(['Agent']))).toBe('principal');
    });
});

describe('isResourceCategory', () => {
    it('treats service and generic resource categories as resource kinds', () => {
        expect(['mcp', 'api', 'agent', 'model', 'event', 'resource'].every(isResourceCategory)).toBe(true);
    });

    it('does not treat principal or custom as resource kinds', () => {
        expect(isResourceCategory('principal')).toBe(false);
        expect(isResourceCategory('custom')).toBe(false);
    });
});
