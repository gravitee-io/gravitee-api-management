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

import { ConnectorService } from './connector.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ConnectorService', () => {
  let httpTestingController: HttpTestingController;
  let connectorService: ConnectorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    connectorService = TestBed.inject<ConnectorService>(ConnectorService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    const mockConnectorItem = [
      {
        id: 'connector-http',
        name: 'HTTP Connector',
        description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
        version: '2.0.6',
        schema: '{}',
        supportedTypes: ['http', 'grpc'],
      },
    ];

    it('should get the connector list item without any expand', done => {
      connectorService.list().subscribe(response => {
        expect(response).toMatchObject(mockConnectorItem);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/connectors`,
      });

      req.flush(mockConnectorItem);
    });

    it('should get the connector list item with schema expand', done => {
      connectorService.list(true).subscribe(response => {
        expect(response).toMatchObject(mockConnectorItem);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/connectors?expand=schema`,
      });

      req.flush(mockConnectorItem);
    });

    it('should get the connector list item with icon expand', done => {
      connectorService.list(false, true).subscribe(response => {
        expect(response).toMatchObject(mockConnectorItem);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/connectors?expand=icon`,
      });

      req.flush(mockConnectorItem);
    });

    it('should get the connector list item with icon and schema expands', done => {
      connectorService.list(true, true).subscribe(response => {
        expect(response).toMatchObject(mockConnectorItem);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/connectors?expand=schema&expand=icon`,
      });

      req.flush(mockConnectorItem);
    });
  });
});
