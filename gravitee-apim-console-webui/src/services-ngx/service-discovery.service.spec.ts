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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ServiceDiscoveryService } from './service-discovery.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ServiceDiscoveryService', () => {
  let httpTestingController: HttpTestingController;
  let serviceDiscoveryService: ServiceDiscoveryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    serviceDiscoveryService = TestBed.inject<ServiceDiscoveryService>(ServiceDiscoveryService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should get the service discovery list', done => {
      const serviceDiscoveryList = [
        {
          id: 'consul-service-discovery',
          name: 'Consul.io Service Discovery',
          description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
          version: '1.3.0',
        },
      ];

      serviceDiscoveryService.list().subscribe(response => {
        expect(response).toMatchObject(serviceDiscoveryList);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/services-discovery`,
      });

      req.flush(serviceDiscoveryList);
    });
  });

  describe('getSchema', () => {
    it('should get the service discovery schema', done => {
      const schema = {
        type: 'object',
        id: 'urn:jsonschema:io:gravitee:discovery:consul:configuration:ConsulServiceDiscoveryConfiguration',
        properties: {},
        required: ['url', 'service', 'trustStoreType', 'keyStoreType'],
      };

      serviceDiscoveryService.getSchema('consul-service-discovery').subscribe(response => {
        expect(response).toMatchObject(schema);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/services-discovery/consul-service-discovery/schema`,
      });

      req.flush(schema);
    });
  });
});
