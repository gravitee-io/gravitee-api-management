/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { GvPageSwaggerUIComponent } from './gv-page-swaggerui.component';

describe('GvPageSwaggerUIComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageSwaggerUIComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule],
  });

  let spectator: Spectator<GvPageSwaggerUIComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('normalizeTypeArrays', () => {
    it('should flatten a single-element type array to that type', () => {
      expect(component['normalizeTypeArrays']({ type: ['integer'] })).toEqual({ type: 'integer' });
    });

    it('should pick the most permissive OAS type when multiple non-null types are present', () => {
      expect(component['normalizeTypeArrays']({ type: ['integer', 'string'] })).toEqual({ type: 'string' });
    });

    it('should pick "number" over "integer" when both are present', () => {
      expect(component['normalizeTypeArrays']({ type: ['integer', 'number'] })).toEqual({ type: 'number' });
    });

    it('should leave a non-OAS type array (e.g. extension values) untouched', () => {
      expect(component['normalizeTypeArrays']({ type: ['compact', 'expanded'] })).toEqual({ type: ['compact', 'expanded'] });
    });

    it('should strip null and add nullable:true for a nullable type', () => {
      expect(component['normalizeTypeArrays']({ type: ['string', 'null'] })).toEqual({ type: 'string', nullable: true });
    });

    it('should fall back to "string" and add nullable:true when the type array contains only "null"', () => {
      expect(component['normalizeTypeArrays']({ type: ['null'] })).toEqual({ type: 'string', nullable: true });
    });

    it('should leave a plain string type untouched', () => {
      expect(component['normalizeTypeArrays']({ type: 'number' })).toEqual({ type: 'number' });
    });

    it('should recurse into nested schema objects', () => {
      const input = { properties: { id: { type: ['integer', 'string'] } } };
      expect(component['normalizeTypeArrays'](input)).toEqual({ properties: { id: { type: 'string' } } });
    });

    it('should recurse into arrays', () => {
      expect(component['normalizeTypeArrays']([{ type: ['integer'] }])).toEqual([{ type: 'integer' }]);
    });

    it('should return primitives and null unchanged', () => {
      expect(component['normalizeTypeArrays'](null)).toBeNull();
      expect(component['normalizeTypeArrays']('string')).toBe('string');
      expect(component['normalizeTypeArrays'](42)).toBe(42);
    });
  });
});
