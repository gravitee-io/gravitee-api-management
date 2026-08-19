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
import { formatCurlCommandLine } from './api-access.utils';

describe('formatCurlCommandLine', () => {
  it('should return an empty command when no entrypoint is available', () => {
    expect(formatCurlCommandLine('', 'API_KEY', 'X-Gravitee-Api-Key', 'api-key')).toBe('');
  });

  it('should format an API key command with the configured header and key', () => {
    expect(formatCurlCommandLine('https://api.example.com', 'API_KEY', 'X-Gravitee-Api-Key', 'api-key')).toBe(
      'curl --header "X-Gravitee-Api-Key: api-key" https://api.example.com',
    );
  });

  it('should use an API key placeholder when no key is available', () => {
    expect(formatCurlCommandLine('https://api.example.com', 'API_KEY', 'X-Gravitee-Api-Key')).toBe(
      'curl --header "X-Gravitee-Api-Key: {{ API_KEY }}" https://api.example.com',
    );
  });

  it.each(['JWT', 'OAUTH2'] as const)('should format a bearer token command for %s', planSecurity => {
    expect(formatCurlCommandLine('https://api.example.com', planSecurity)).toBe(
      'curl --header "Authorization: Bearer {{ ACCESS_TOKEN }}" https://api.example.com',
    );
  });

  it('should format a headerless command for KEY_LESS', () => {
    expect(formatCurlCommandLine('https://api.example.com', 'KEY_LESS')).toBe('curl https://api.example.com');
  });
});
