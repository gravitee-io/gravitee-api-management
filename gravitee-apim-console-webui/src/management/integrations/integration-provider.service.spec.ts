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
import { IntegrationProviderService } from './integration-provider.service';

describe('IntegrationProviderService', () => {
  let service: IntegrationProviderService;

  beforeEach(() => {
    service = new IntegrationProviderService();
  });

  describe('getApimDocsNameByValue', () => {
    it('should return the correct apimDocsName for a valid value', () => {
      const result = service.getApimDocsNameByValue('aws-api-gateway');
      expect(result).toBe('aws-api-gateway');
    });

    it('should return undefined for an unknown value', () => {
      const result = service.getApimDocsNameByValue('nonexistent');
      expect(result).toBeUndefined();
    });
  });

  describe('getActiveProviders', () => {
    it('should return the list of active providers', () => {
      const providers = service.getActiveProviders();
      expect(providers.length).toBeGreaterThan(0);
      expect(providers).toEqual(expect.arrayContaining([expect.objectContaining({ value: 'aws-api-gateway' })]));
    });
  });

  describe('getComingSoonProviders', () => {
    it('should return an empty array by default', () => {
      const providers = service.getComingSoonProviders();
      expect(providers).toEqual([]);
    });
  });
});
