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
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { tick } from '@angular/core/testing';

import {
  ConnectorPlugin,
  fakeApiV4,
  fakeConnectorPlugin,
  fakePlanV4,
  getEntrypointConnectorSchema,
} from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING } from '../../../shared/testing';
import { License } from '../../../entities/license/License';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { RestrictedDomain } from '../../../entities/restricted-domain/restrictedDomain';

export class ApiCreationV4SpecHttpExpects {
  constructor(private httpTestingController: HttpTestingController) {}

  expectApiGetPortalSettings() {
    const settings: PortalSettings = {
      portal: {
        entrypoint: 'entrypoint',
      },
    };
    this.httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/settings`, method: 'GET' }).flush(settings);
  }

  expectVerifyContextPath(invalidPath = false) {
    tick(250);
    this.httpTestingController
      .match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/paths`, method: 'POST' })
      .filter((request) => !request.cancelled)
      .map((testRequest) =>
        testRequest.flush({
          ok: !invalidPath,
          reason: invalidPath ? `Path [${invalidPath}] already exists` : '',
        }),
      );
    tick(500);
  }

  expectRestrictedDomainsGetRequest(restrictedDomains: RestrictedDomain[]) {
    this.httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/restrictedDomains`, method: 'GET' })
      .flush(restrictedDomains);
  }

  expectSchemaGetRequest(connectors: Partial<ConnectorPlugin>[], connectorType: 'entrypoints' | 'endpoints' = 'entrypoints') {
    connectors.forEach((connector) => {
      this.httpTestingController
        .match({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/${connectorType}/${connector.id}/schema`, method: 'GET' })
        .map((req) => {
          if (!req.cancelled) req.flush(getEntrypointConnectorSchema(connector.id));
        });
    });
  }

  expectEntrypointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    this.httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints` }).flush(fullConnectors);
  }

  expectLicenseGetRequest(license: License) {
    this.httpTestingController.expectOne({ url: LICENSE_CONFIGURATION_TESTING.resourceURL, method: 'GET' }).flush(license);
  }

  expectEndpointGetRequest(connector: Partial<ConnectorPlugin>) {
    const fullConnector = fakeConnectorPlugin(connector);

    this.httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${fullConnector.id}`, method: 'GET' })
      .flush(fullConnector);
  }

  expectEndpointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    this.httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`, method: 'GET' })
      .flush(fullConnectors);
  }

  expectEndpointsSharedConfigurationSchemaGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    connectors.forEach((connector) => {
      this.httpTestingController
        .match({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${connector.id}/shared-configuration-schema`, method: 'GET' })
        .map((req) => {
          if (!req.cancelled) req.flush(getEntrypointConnectorSchema(connector.id));
        });
    });
  }

  expectCallsForApiAndPlanCreation(apiId: string, planId: string) {
    this.expectCallsForApiCreation(apiId);

    const createPlansRequest = this.httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans`,
      method: 'POST',
    });
    expect(createPlansRequest.request.body).toEqual(
      expect.objectContaining({
        definitionVersion: 'V4',
        name: 'Update name',
      }),
    );
    createPlansRequest.flush(fakePlanV4({ apiId: apiId, id: planId }));
  }

  expectCallsForApiCreation(apiId: string) {
    const createApiRequest = this.httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis`, method: 'POST' });

    expect(createApiRequest.request.body).toEqual(
      expect.objectContaining({
        definitionVersion: 'V4',
        name: 'API name',
      }),
    );
    createApiRequest.flush(fakeApiV4({ id: apiId }));
  }

  expectCallsForApiDeployment(apiId: string, planId: string) {
    this.expectCallsForApiAndPlanCreation(apiId, planId);

    const publishPlansRequest = this.httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_publish`,
      method: 'POST',
    });
    publishPlansRequest.flush({});

    const startApiRequest = this.httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_start`,
      method: 'POST',
    });
    startApiRequest.flush(fakeApiV4({ id: apiId }));
  }

  expectVerifyHosts(hosts: string[], times = 1) {
    tick(250);
    const requests = this.httpTestingController.match({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_verify/hosts`, method: 'POST' });
    expect(requests.length).toStrictEqual(times);
    hosts.forEach((host, index) => {
      const request = requests[index];
      expect(request.request.body).toEqual(
        expect.objectContaining({
          hosts: [host],
        }),
      );
      request.flush({ ok: true });
      tick(500);
    });
  }
}
