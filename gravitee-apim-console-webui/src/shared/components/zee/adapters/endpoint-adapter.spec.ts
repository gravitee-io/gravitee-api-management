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

import { ENDPOINT_ADAPTER } from './endpoint-adapter';

describe('ENDPOINT_ADAPTER', () => {
  it('should have the correct preview label', () => {
    expect(ENDPOINT_ADAPTER.previewLabel).toEqual('Generated Endpoint');
  });

  describe('transform()', () => {
    it('should return defaults for minimal payload', () => {
      const result = ENDPOINT_ADAPTER.transform({});

      expect(result).toEqual({
        name: '',
        type: 'http-proxy',
        weight: 1,
        inheritConfiguration: true,
        secondary: false,
        configuration: {},
        sharedConfigurationOverride: {},
        services: {},
      });
    });

    it('should pass through all properties', () => {
      const generated = {
        name: 'My Backend',
        type: 'grpc',
        weight: 5,
        inheritConfiguration: false,
        secondary: true,
        configuration: { target: 'https://api.example.com' },
        sharedConfigurationOverride: { timeout: 5000 },
        services: { healthcheck: { enabled: true } },
      };

      const result = ENDPOINT_ADAPTER.transform(generated);

      expect(result).toEqual({
        name: 'My Backend',
        type: 'grpc',
        weight: 5,
        inheritConfiguration: false,
        secondary: true,
        configuration: { target: 'https://api.example.com' },
        sharedConfigurationOverride: { timeout: 5000 },
        services: { healthcheck: { enabled: true } },
      });
    });

    it('should parse stringified JSON configuration', () => {
      const generated = {
        name: 'Endpoint',
        configuration: '{"target":"https://api.example.com","retries":3}',
      };

      const result = ENDPOINT_ADAPTER.transform(generated);

      expect(result.configuration).toEqual({
        target: 'https://api.example.com',
        retries: 3,
      });
    });

    it('should fall back to empty object for invalid JSON configuration', () => {
      const generated = {
        name: 'Endpoint',
        configuration: '{broken json}',
      };

      const result = ENDPOINT_ADAPTER.transform(generated);

      expect(result.configuration).toEqual({});
    });

    it('should handle null/undefined configuration gracefully', () => {
      const result = ENDPOINT_ADAPTER.transform({ name: 'Endpoint', configuration: null });
      expect(result.configuration).toEqual({});
    });

    it('should handle nested configuration.target from Replace op', () => {
      const generated = {
        name: 'HTTP Backend',
        type: 'http-proxy',
        configuration: {
          target: 'https://httpbin.org/anything',
        },
      };

      const result = ENDPOINT_ADAPTER.transform(generated);

      expect(result.configuration).toEqual({ target: 'https://httpbin.org/anything' });
    });
  });
});
