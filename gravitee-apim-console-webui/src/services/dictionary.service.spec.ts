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
import DictionaryService from './dictionary.service';

describe('DictionaryService', () => {
  let $http: any;
  let service: DictionaryService;

  const Constants = { env: { baseURL: 'http://host/management/environments/DEFAULT' } };

  beforeEach(() => {
    $http = { put: jest.fn() };
    service = new DictionaryService($http, Constants);
  });

  describe('update', () => {
    it('should send propertyOptions alongside properties', () => {
      service.update({
        id: 'dic-1',
        name: 'My dictionary',
        type: 'MANUAL',
        properties: { url: 'https://backend', apiKey: 's3cr3t' },
        propertyOptions: { apiKey: { encryptable: true } },
      });

      expect($http.put).toHaveBeenCalledWith(
        'http://host/management/environments/DEFAULT/configuration/dictionaries/dic-1',
        expect.objectContaining({
          properties: { url: 'https://backend', apiKey: 's3cr3t' },
          propertyOptions: { apiKey: { encryptable: true } },
        }),
      );
    });

    it('should omit propertyOptions when the dictionary carries none', () => {
      service.update({
        id: 'dic-1',
        name: 'My dictionary',
        type: 'MANUAL',
        properties: { url: 'https://backend' },
      });

      expect($http.put.mock.calls[0][1].propertyOptions).toBeUndefined();
    });

    it('should send only encryptable for a renewed property, since the new value is plaintext', () => {
      service.update({
        id: 'dic-1',
        name: 'My dictionary',
        type: 'MANUAL',
        properties: { apiKey: 'newS3cr3t' },
        propertyOptions: { apiKey: { encrypted: true, encryptable: true } },
      });

      expect($http.put.mock.calls[0][1].propertyOptions).toEqual({ apiKey: { encryptable: true } });
    });
  });
});
