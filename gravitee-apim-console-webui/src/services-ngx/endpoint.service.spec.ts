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

import { EndpointService } from './endpoint.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeConnectorListItem } from '../entities/connector/connector-list-item.fixture';

describe('EndpointService', () => {
  let httpTestingController: HttpTestingController;
  let endpointService: EndpointService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    endpointService = TestBed.inject<EndpointService>(EndpointService);
  });

  describe('v4ListEndpointPlugins', () => {
    it('should call the API', (done) => {
      const fakeConnectors = [fakeConnectorListItem()];

      endpointService.v4ListEndpointPlugins().subscribe((connectors) => {
        expect(connectors).toMatchObject(fakeConnectors);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/v4/endpoints`,
          method: 'GET',
        })
        .flush(fakeConnectors);
    });
  });

  describe('v4Get', () => {
    it('should call the API', (done) => {
      const fakeConnectors = fakeConnectorListItem();

      endpointService.v4Get('endpointId').subscribe((connectors) => {
        expect(connectors).toEqual(fakeConnectors);
        done();
      });

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.baseURL}/v4/endpoints/endpointId`,
          method: 'GET',
        })
        .flush(fakeConnectors);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
