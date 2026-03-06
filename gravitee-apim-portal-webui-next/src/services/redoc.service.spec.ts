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
import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { REDOC_PRIMARY_COLOR_FALLBACK } from './redoc-defaults';
import { RedocService } from './redoc.service';

describe('RedocService', () => {
  let service: RedocService;
  let redocInitSpy: ReturnType<typeof jest.fn>;
  const mockElement = document.createElement('div');

  const setupTestBed = (primaryColor?: string) => {
    const computedStyle = {
      getPropertyValue: (prop: string) => {
        if (prop === '--gio-app-primary-main-color') return primaryColor ?? '';
        if (prop === '--gio-app-primary-main-color-fallback') return REDOC_PRIMARY_COLOR_FALLBACK;
        return '';
      },
    };
    const mockDocument = {
      documentElement: document.documentElement,
      defaultView: {
        getComputedStyle: () => computedStyle,
      },
    };

    const initSpy = jest.fn();
    (globalThis as unknown as { Redoc: { init: ReturnType<typeof jest.fn> } }).Redoc = { init: initSpy };
    redocInitSpy = initSpy;

    TestBed.configureTestingModule({
      providers: [{ provide: DOCUMENT, useValue: mockDocument }],
    });
    service = TestBed.inject(RedocService);
  };

  afterEach(() => {
    redocInitSpy?.mockClear?.();
  });

  describe('init', () => {
    it('should not call Redoc.init when content is undefined', () => {
      setupTestBed();
      service.init(undefined, {}, mockElement);
      expect(redocInitSpy).not.toHaveBeenCalled();
    });

    it('should not call Redoc.init when content is empty string', () => {
      setupTestBed();
      service.init('', {}, mockElement);
      expect(redocInitSpy).not.toHaveBeenCalled();
    });

    it('should call Redoc.init with parsed JSON spec and merged options when content is JSON', () => {
      setupTestBed();
      const content = '{"openapi":"3.0.0","info":{"title":"Test","version":"1.0.0"}}';
      service.init(content, { hideDownloadButton: true }, mockElement);
      expect(redocInitSpy).toHaveBeenCalledTimes(1);
      const [spec, options, element] = redocInitSpy.mock.calls[redocInitSpy.mock.calls.length - 1];
      expect(spec).toEqual({ openapi: '3.0.0', info: { title: 'Test', version: '1.0.0' } });
      expect(element).toBe(mockElement);
      expect(options).toEqual(
        expect.objectContaining({
          hideDownloadButton: true,
          theme: expect.objectContaining({
            colors: expect.objectContaining({
              primary: { main: REDOC_PRIMARY_COLOR_FALLBACK },
              http: expect.objectContaining({
                get: REDOC_PRIMARY_COLOR_FALLBACK,
                post: REDOC_PRIMARY_COLOR_FALLBACK,
                delete: REDOC_PRIMARY_COLOR_FALLBACK,
              }),
            }),
          }),
        }),
      );
    });

    it('should use primary color from CSS variable when --gio-app-primary-main-color is set', () => {
      setupTestBed(' #ff6600 ');
      const content = '{"openapi":"3.0.0"}';
      service.init(content, {}, mockElement);
      const [, options] = redocInitSpy.mock.calls[redocInitSpy.mock.calls.length - 1];
      expect(options).toEqual(
        expect.objectContaining({
          theme: expect.objectContaining({
            colors: expect.objectContaining({
              primary: { main: '#ff6600' },
              http: expect.objectContaining({ get: '#ff6600' }),
            }),
          }),
        }),
      );
    });

    it('should use default primary color when CSS variable is not set', () => {
      setupTestBed();
      const content = '{"openapi":"3.0.0"}';
      service.init(content, {}, mockElement);
      const [, options] = redocInitSpy.mock.calls[redocInitSpy.mock.calls.length - 1];
      expect(options).toEqual(
        expect.objectContaining({
          theme: expect.objectContaining({
            colors: expect.objectContaining({
              primary: { main: REDOC_PRIMARY_COLOR_FALLBACK },
            }),
          }),
        }),
      );
    });

    it('should preserve existing theme options when merging', () => {
      setupTestBed();
      const content = '{"openapi":"3.0.0"}';
      const baseOptions = {
        theme: {
          breakpoints: { medium: '60rem', large: '80rem' },
          typography: { fontSize: '14px' },
        },
      };
      service.init(content, baseOptions, mockElement);
      const [, options] = redocInitSpy.mock.calls[redocInitSpy.mock.calls.length - 1];
      expect(options).toEqual(
        expect.objectContaining({
          theme: expect.objectContaining({
            breakpoints: { medium: '60rem', large: '80rem' },
            typography: { fontSize: '14px' },
            colors: expect.objectContaining({
              primary: { main: REDOC_PRIMARY_COLOR_FALLBACK },
            }),
          }),
        }),
      );
    });
  });
});
