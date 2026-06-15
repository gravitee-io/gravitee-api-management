/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import SwaggerUI from 'swagger-ui';

import { PageSwaggerComponent } from './page-swagger.component';
import { fakePage } from '../../../entities/page/page.fixtures';
import { AppTestingModule } from '../../../testing/app-testing.module';

<<<<<<< HEAD
=======
jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: jest.fn() })));
const SwaggerUIMock = jest.mocked(SwaggerUI);

type WithNormalizeTypeArrays = { normalizeTypeArrays: (obj: unknown) => unknown };

>>>>>>> dfdd2b3875 (fix(portal): normalize OAS 3.1 type arrays when show_url is enabled)
describe('PageSwaggerComponent', () => {
  let component: PageSwaggerComponent;
  let fixture: ComponentFixture<PageSwaggerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageSwaggerComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PageSwaggerComponent);
    component = fixture.componentInstance;
    component.page = fakePage({ type: 'SWAGGER' });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
<<<<<<< HEAD
=======

  it('should normalize OAS 3.1 type arrays in the spec before passing it to Swagger UI (JSON)', () => {
    const spec = {
      openapi: '3.1.0',
      paths: {
        '/test': {
          get: {
            parameters: [{ name: 'id', in: 'query', schema: { type: ['integer', 'string'] } }],
          },
        },
      },
    };
    component.page = fakePage({ type: 'SWAGGER', content: JSON.stringify(spec) });
    component.ngOnChanges();
    expect(component).toBeTruthy();
  });

  it('should normalize OAS 3.1 type arrays in the spec before passing it to Swagger UI (YAML)', () => {
    const yaml = `openapi: "3.1.0"\npaths:\n  /test:\n    get:\n      parameters:\n        - name: id\n          in: query\n          schema:\n            type:\n              - integer\n              - string\n`;
    component.page = fakePage({ type: 'SWAGGER', content: yaml });
    component.ngOnChanges();
    expect(component).toBeTruthy();
  });

  it('should handle missing page content gracefully', () => {
    component.page = fakePage({ type: 'SWAGGER', content: undefined });
    component.ngOnChanges();
    expect(component).toBeTruthy();
  });

  describe('show_url normalization', () => {
    it('should register normalizeSpecPlugin when show_url is enabled', () => {
      SwaggerUIMock.mockClear();

      component.page = fakePage({
        type: 'SWAGGER',
        content: '{}',
        configuration: { show_url: true },
        _links: { content: 'http://example.com/spec.json' },
      });
      component.ngOnChanges();

      const options = SwaggerUIMock.mock.calls[0][0];
      expect(options.url).toBe('http://example.com/spec.json');
      expect(options.spec).toBeUndefined();
      expect(options.plugins!.length).toBeGreaterThan(0);
    });

    it('should not register normalizeSpecPlugin when show_url is disabled', () => {
      SwaggerUIMock.mockClear();

      component.page = fakePage({
        type: 'SWAGGER',
        content: '{}',
        configuration: { show_url: false },
      });
      component.ngOnChanges();

      const options = SwaggerUIMock.mock.calls[0][0];
      expect(options.url).toBeUndefined();
      // Only disabledTryItOutPlugin and disabledAuthorizationPlugin should be present, not normalizeSpecPlugin
      expect(options.plugins).toHaveLength(2);
    });
  });

  describe('normalizeTypeArrays', () => {
    it('should flatten a single-element type array to that type', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['integer'] })).toEqual({ type: 'integer' });
    });

    it('should pick the most permissive OAS type when multiple non-null types are present', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['integer', 'string'] })).toEqual({
        type: 'string',
      });
    });

    it('should pick "number" over "integer" when both are present', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['integer', 'number'] })).toEqual({
        type: 'number',
      });
    });

    it('should leave a non-OAS type array (e.g. extension values) untouched', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['compact', 'expanded'] })).toEqual({
        type: ['compact', 'expanded'],
      });
    });

    it('should strip null and add nullable:true for a nullable type', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['string', 'null'] })).toEqual({
        type: 'string',
        nullable: true,
      });
    });

    it('should fall back to "string" and add nullable:true when the type array contains only "null"', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: ['null'] })).toEqual({
        type: 'string',
        nullable: true,
      });
    });

    it('should leave a plain string type untouched', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays({ type: 'number' })).toEqual({ type: 'number' });
    });

    it('should recurse into nested schema objects', () => {
      const input = { properties: { id: { type: ['integer', 'string'] } } };
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays(input)).toEqual({
        properties: { id: { type: 'string' } },
      });
    });

    it('should recurse into arrays', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays([{ type: ['integer'] }])).toEqual([{ type: 'integer' }]);
    });

    it('should return primitives and null unchanged', () => {
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays(null)).toBeNull();
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays('string')).toBe('string');
      expect((component as unknown as WithNormalizeTypeArrays).normalizeTypeArrays(42)).toBe(42);
    });
  });
>>>>>>> dfdd2b3875 (fix(portal): normalize OAS 3.1 type arrays when show_url is enabled)
});
