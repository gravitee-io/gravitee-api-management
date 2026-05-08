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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import SwaggerUI from 'swagger-ui';

import { PageSwaggerComponent } from './page-swagger.component';
import { DocExpansionEnum } from '../../../entities/page/page-configuration';
import { fakePage } from '../../../entities/page/page.fixtures';
import { AppTestingModule } from '../../../testing/app-testing.module';

jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: jest.fn() })));
const SwaggerUIMock = jest.mocked(SwaggerUI);

type WithNormalizeTypeArrays = { normalizeTypeArrays: (obj: unknown) => unknown };

@Component({
  standalone: true,
  imports: [PageSwaggerComponent],
  template: `
    <app-page-swagger [page]="firstPage" />
    <app-page-swagger [page]="secondPage" />
  `,
})
class PageSwaggerHostComponent {
  firstPage = fakePage({ id: 'first-page', type: 'SWAGGER', content: '{"openapi":"3.0.0"}' });
  secondPage = fakePage({ id: 'second-page', type: 'SWAGGER', content: '{"openapi":"3.0.0"}' });
}

describe('PageSwaggerComponent', () => {
  let component: PageSwaggerComponent;
  let fixture: ComponentFixture<PageSwaggerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageSwaggerComponent, PageSwaggerHostComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PageSwaggerComponent);
    component = fixture.componentInstance;
    component.page = fakePage({ type: 'SWAGGER' });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

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

  describe('resolveRelativeServerUrls', () => {
    const callResolve = (spec: Record<string, unknown>, origin: string) =>
      (component as unknown as { resolveRelativeServerUrls: (s: Record<string, unknown>, o: string) => void }).resolveRelativeServerUrls(
        spec,
        origin,
      );

    it('should resolve relative server URLs against the portal origin', () => {
      const spec: Record<string, unknown> = {
        servers: [{ url: '/' }, { url: '/api/v1' }, { url: 'https://gateway.example.com' }],
      };
      callResolve(spec, 'http://localhost:4100');
      expect((spec['servers'] as { url: string }[])[0].url).toBe('http://localhost:4100/');
      expect((spec['servers'] as { url: string }[])[1].url).toBe('http://localhost:4100/api/v1');
      expect((spec['servers'] as { url: string }[])[2].url).toBe('https://gateway.example.com');
    });

    it('should default missing url to "/" per OpenAPI spec', () => {
      const spec: Record<string, unknown> = {
        servers: [{ description: 'no url' }],
      };
      callResolve(spec, 'http://localhost:4100');
      expect((spec['servers'] as { url: string }[])[0].url).toBe('http://localhost:4100/');
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

  it('should map snake_case page configuration to Swagger UI options', () => {
    const initOAuth = jest.fn();
    jest.mocked(SwaggerUI).mockClear();
    jest.mocked(SwaggerUI).mockReturnValue({ initOAuth } as never);
    component.page = fakePage({
      type: 'SWAGGER',
      content: '{"openapi":"3.0.0","info":{"title":"Test","version":"1.0.0"}}',
      configuration: {
        try_it_url: 'https://try-it.example.com',
        disable_syntax_highlight: true,
        doc_expansion: DocExpansionEnum.Full,
        display_operation_id: true,
        enable_filtering: true,
        show_extensions: true,
        show_common_extensions: true,
        max_displayed_tags: 7,
        use_pkce: true,
      },
    });

    component.ngOnChanges();

    expect(SwaggerUI).toHaveBeenCalledWith(
      expect.objectContaining({
        spec: expect.objectContaining({ servers: [{ url: 'https://try-it.example.com' }] }),
        syntaxHighlight: false,
        docExpansion: DocExpansionEnum.Full,
        displayOperationId: true,
        filter: true,
        showExtensions: true,
        showCommonExtensions: true,
        maxDisplayedTags: 7,
      }),
    );
    expect(initOAuth).toHaveBeenCalledWith({
      usePkceWithAuthorizationCodeGrant: true,
    });
  });

  it('should mount Swagger UI on its local element without using a global id lookup', () => {
    const getElementByIdSpy = jest.spyOn(document, 'getElementById');
    jest.mocked(SwaggerUI).mockClear();

    component.ngOnChanges();

    expect(getElementByIdSpy).not.toHaveBeenCalled();
    expect(SwaggerUI).toHaveBeenCalledWith(
      expect.objectContaining({
        domNode: expect.any(HTMLDivElement),
      }),
    );

    getElementByIdSpy.mockRestore();
  });

  it('should mount multiple instances on separate local elements', () => {
    jest.mocked(SwaggerUI).mockClear();

    const hostFixture = TestBed.createComponent(PageSwaggerHostComponent);
    hostFixture.detectChanges();

    const [firstOptions, secondOptions] = jest.mocked(SwaggerUI).mock.calls.map(([options]) => options);

    expect(firstOptions.domNode).toEqual(expect.any(HTMLDivElement));
    expect(secondOptions.domNode).toEqual(expect.any(HTMLDivElement));
    expect(firstOptions.domNode).not.toBe(secondOptions.domNode);
  });
});
