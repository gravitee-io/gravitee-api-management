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
import { firstHttpListener, hasPathMappings, storedPathMappings } from './apiPathMappings';
import { fakeHttpListener, fakeKafkaListener, fakeSubscriptionListener } from './listener.fixture';

import { fakeProxyApiV4 } from '../api.fixture';

describe('apiPathMappings', () => {
  describe('storedPathMappings', () => {
    it('should read the path mappings of the first HTTP listener only', () => {
      const api = fakeProxyApiV4({
        listeners: [
          fakeSubscriptionListener(),
          fakeHttpListener({ pathMappings: ['/charges', '/charges/:chargeId'] }),
          fakeHttpListener({ pathMappings: ['/ignored'] }),
        ],
      });

      expect(storedPathMappings(api)).toEqual(['/charges', '/charges/:chargeId']);
      expect(firstHttpListener(api)?.pathMappings).toEqual(['/charges', '/charges/:chargeId']);
    });

    it('should return a copy the caller can change without touching the API', () => {
      const listener = fakeHttpListener({ pathMappings: ['/charges'] });
      const api = fakeProxyApiV4({ listeners: [listener] });

      storedPathMappings(api).push('/added');

      expect(listener.pathMappings).toEqual(['/charges']);
    });

    it.each([
      ['undefined path mappings', [fakeHttpListener({ pathMappings: undefined })]],
      ['an empty path mapping list', [fakeHttpListener({ pathMappings: [] })]],
      ['no HTTP listener', [fakeSubscriptionListener(), fakeKafkaListener()]],
      ['no listener at all', undefined],
    ])('should return no path mapping for %s', (_, listeners) => {
      const api = fakeProxyApiV4({ listeners });

      expect(storedPathMappings(api)).toEqual([]);
    });

    it('should return no path mapping without an API', () => {
      expect(storedPathMappings(undefined)).toEqual([]);
      expect(firstHttpListener(undefined)).toBeUndefined();
    });
  });

  describe('hasPathMappings', () => {
    it('should be true when the first HTTP listener has at least one path mapping', () => {
      expect(hasPathMappings(fakeProxyApiV4({ listeners: [fakeHttpListener({ pathMappings: ['/charges/:chargeId'] })] }))).toBe(true);
    });

    it('should be false when only a later HTTP listener has path mappings', () => {
      const api = fakeProxyApiV4({
        listeners: [fakeHttpListener({ pathMappings: [] }), fakeHttpListener({ pathMappings: ['/charges/:chargeId'] })],
      });

      expect(hasPathMappings(api)).toBe(false);
    });

    it.each([
      ['undefined path mappings', [fakeHttpListener({ pathMappings: undefined })]],
      ['an empty path mapping list', [fakeHttpListener({ pathMappings: [] })]],
      ['no HTTP listener', [fakeSubscriptionListener(), fakeKafkaListener()]],
      ['no listener at all', undefined],
    ])('should be false for %s', (_, listeners) => {
      expect(hasPathMappings(fakeProxyApiV4({ listeners }))).toBe(false);
    });
  });
});
