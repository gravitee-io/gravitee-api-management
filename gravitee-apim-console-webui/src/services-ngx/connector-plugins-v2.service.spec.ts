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

import { ConnectorPluginsV2Service } from './connector-plugins-v2.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeConnectorPlugin } from '../entities/management-api-v2/connector/connectorPlugin.fixture';

describe('Installation Plugins Service', () => {
  let httpTestingController: HttpTestingController;
  let installationPluginsService: ConnectorPluginsV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    installationPluginsService = TestBed.inject<ConnectorPluginsV2Service>(ConnectorPluginsV2Service);
  });

  describe('Endpoints', () => {
    describe('listEndpointPlugins', () => {
      it('should call the API', (done) => {
        const fakeConnectors = [fakeConnectorPlugin()];

        installationPluginsService.listEndpointPlugins().subscribe((connectors) => {
          expect(connectors).toMatchObject(fakeConnectors);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });

    describe('v4Get', () => {
      it('should call the API', (done) => {
        const fakeConnectors = fakeConnectorPlugin();

        installationPluginsService.getEndpointPlugin('endpointId').subscribe((connectors) => {
          expect(connectors).toEqual(fakeConnectors);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/endpointId`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });
  });

  describe('Entrypoints', () => {
    describe('listSyncEntrypointPlugins', () => {
      it('should call the API', (done) => {
        const fakeConnectors = [fakeConnectorPlugin({ supportedApiType: 'PROXY' }), fakeConnectorPlugin({ supportedApiType: 'MESSAGE' })];

        installationPluginsService.listSyncEntrypointPlugins().subscribe((connectors) => {
          expect(connectors).toMatchObject([fakeConnectors[0]]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });

    describe('listAsyncEntrypointPlugins', () => {
      it('should call the API', (done) => {
        const fakeConnectors = [fakeConnectorPlugin({ supportedApiType: 'PROXY' }), fakeConnectorPlugin({ supportedApiType: 'MESSAGE' })];

        installationPluginsService.listAsyncEntrypointPlugins().subscribe((connectors) => {
          expect(connectors).toMatchObject([fakeConnectors[1]]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints`,
            method: 'GET',
          })
          .flush(fakeConnectors);
      });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
