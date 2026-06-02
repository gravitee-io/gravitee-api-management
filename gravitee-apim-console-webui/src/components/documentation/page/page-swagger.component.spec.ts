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

// swagger-ui is globally mocked as jest.fn() in setup-jest.ts
import SwaggerUI from 'swagger-ui';

import { PageSwaggerComponent } from './page-swagger.component';

const SwaggerUIMock = SwaggerUI as jest.Mock;

describe('PageSwaggerComponent', () => {
  // Direct controller instantiation — this is an AngularJS hybrid component;
  // TestBed is not needed to exercise the controller logic.
  const ControllerClass = PageSwaggerComponent.controller as new (...args: unknown[]) => any;

  let controller: any;
  let mockUserService: any;
  let mockWindow: any;

  beforeEach(() => {
    mockUserService = {
      isAuthenticated: jest.fn().mockReturnValue(false),
    };
    mockWindow = {
      location: { origin: 'http://localhost:4100', pathname: '/console/' },
    };

    controller = new ControllerClass(mockUserService, mockWindow);
    controller.pageConfiguration = {};
    controller.pageContent = '{"openapi":"3.0.0","info":{"title":"Test","version":"1.0"}}';
    controller.cfg = {};

    SwaggerUIMock.mockClear();
  });

  describe('$onChanges()', () => {
    it('should always call SwaggerUI with inline spec regardless of showURL=false', () => {
      controller.pageConfiguration = { showURL: 'false' };

      controller.$onChanges();

      expect(SwaggerUIMock).toHaveBeenCalledTimes(1);
      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.spec).toBeDefined();
      expect(config.url).toBeUndefined();
    });

    it('should always call SwaggerUI with inline spec even when showURL=true', () => {
      // This is the regression test for APIM-14243:
      // Previously, showURL=true switched to url mode which (a) crashed because
      // activatedRoute was undefined and (b) got a 401 from the management API.
      controller.pageConfiguration = { showURL: 'true' };

      controller.$onChanges();

      expect(SwaggerUIMock).toHaveBeenCalledTimes(1);
      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.spec).toBeDefined();
      expect(config.url).toBeUndefined();
    });

    it('should parse YAML pageContent and pass it as spec', () => {
      controller.pageContent = 'openapi: "3.0.0"\ninfo:\n  title: Test\n  version: "1.0"';

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.spec).toEqual({ openapi: '3.0.0', info: { title: 'Test', version: '1.0' } });
      expect(config.url).toBeUndefined();
    });

    it('should forward docExpansion configuration to SwaggerUI', () => {
      controller.pageConfiguration = { docExpansion: 'full' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.docExpansion).toBe('full');
    });

    it('should default docExpansion to "none" when not configured', () => {
      controller.pageConfiguration = {};

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.docExpansion).toBe('none');
    });

    it('should set displayOperationId from configuration', () => {
      controller.pageConfiguration = { displayOperationId: 'true' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.displayOperationId).toBe(true);
    });

    it('should set maxDisplayedTags as a number when valid', () => {
      controller.pageConfiguration = { maxDisplayedTags: '5' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.maxDisplayedTags).toBe(5);
    });

    it('should set maxDisplayedTags to undefined when value is -1', () => {
      controller.pageConfiguration = { maxDisplayedTags: '-1' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.maxDisplayedTags).toBeUndefined();
    });

    it('should disable TryItOut plugin when tryIt is not enabled', () => {
      controller.pageConfiguration = { tryIt: 'false' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.plugins).toHaveLength(1);
    });

    it('should not add DisableTryItOut plugin when tryIt is enabled and user is authenticated', () => {
      mockUserService.isAuthenticated.mockReturnValue(true);
      controller.pageConfiguration = { tryIt: 'true' };

      controller.$onChanges();

      const config = SwaggerUIMock.mock.calls[0][0];
      expect(config.plugins).toHaveLength(0);
    });

    it('should mount to dom_id #swagger-container', () => {
      controller.$onChanges();

      expect(SwaggerUIMock).toHaveBeenCalledWith(expect.objectContaining({ dom_id: '#swagger-container' }));
    });
  });

  describe('tryItEnabled()', () => {
    it('should return false when tryIt is not set', () => {
      controller.pageConfiguration = {};
      expect(controller.tryItEnabled()).toBe(false);
    });

    it('should return false when tryIt is true but user is not authenticated and anonymous is disabled', () => {
      mockUserService.isAuthenticated.mockReturnValue(false);
      controller.pageConfiguration = { tryIt: 'true', tryItAnonymous: 'false' };
      expect(controller.tryItEnabled()).toBe(false);
    });

    it('should return true when tryIt is true and user is authenticated', () => {
      mockUserService.isAuthenticated.mockReturnValue(true);
      controller.pageConfiguration = { tryIt: 'true' };
      expect(controller.tryItEnabled()).toBe(true);
    });

    it('should return true when tryIt is true and anonymous access is enabled', () => {
      mockUserService.isAuthenticated.mockReturnValue(false);
      controller.pageConfiguration = { tryIt: 'true', tryItAnonymous: 'true' };
      expect(controller.tryItEnabled()).toBe(true);
    });
  });

  describe('loadContent()', () => {
    it('should parse valid JSON content', () => {
      controller.pageContent = '{"swagger":"2.0","info":{"title":"My API","version":"1.0"}}';
      const result = controller.loadContent();
      expect(result).toEqual({ swagger: '2.0', info: { title: 'My API', version: '1.0' } });
    });

    it('should fall back to YAML parsing when content is not valid JSON', () => {
      controller.pageContent = 'swagger: "2.0"\ninfo:\n  title: My API\n  version: "1.0"';
      const result = controller.loadContent();
      expect(result).toEqual({ swagger: '2.0', info: { title: 'My API', version: '1.0' } });
    });

    it('should return undefined when content is empty', () => {
      // yaml.load('') returns undefined — pre-existing behavior
      controller.pageContent = '';
      const result = controller.loadContent();
      expect(result).toBeUndefined();
    });
  });
});
