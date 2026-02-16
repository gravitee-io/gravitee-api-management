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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';

import { ApiProxyEndpointListComponent } from './api-proxy-endpoint-list.component';
import { ApiProxyEndpointListHarness } from './api-proxy-endpoint-list.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiEndpointsModule } from '../api-endpoints.module';
import { ApiV2, fakeApiV2 } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyEndpointListComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiProxyEndpointListComponent>;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let endpointsGroupHarness: ApiProxyEndpointListHarness;

  const initComponent = async (api: ApiV2) => {
    TestBed.configureTestingModule({
      declarations: [ApiProxyEndpointListComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiEndpointsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u', 'api-definition-r'] },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });

    fixture = TestBed.createComponent(ApiProxyEndpointListComponent);

    endpointsGroupHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProxyEndpointListHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    expectApiGetRequest(api);
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('mat table tests', () => {
    it('should display the endpoint groups tables', async () => {
      await initComponent(
        fakeApiV2({
          id: API_ID,
          proxy: {
            groups: [
              {
                name: 'default-group',
                endpoints: [
                  {
                    name: 'default',
                    target: 'https://api.le-systeme-solaire.net/rest/',
                    weight: 1,
                    backup: false,
                    type: 'HTTP',
                    inherit: false,
                  },
                  {
                    name: 'secondary endpoint',
                    target: 'https://api.gravitee.io/echo',
                    weight: 1,
                    backup: false,
                    type: 'HTTP',
                    inherit: false,
                  },
                ],
              },
              {
                name: 'second group',
                endpoints: [
                  {
                    name: 'default',
                    target: 'https://api.gravitee.io/echo',
                    weight: 1,
                    backup: false,
                    type: 'HTTP',
                    inherit: false,
                  },
                ],
              },
            ],
          },
        }),
      );

      expect(await endpointsGroupHarness.getTableRows(0)).toEqual([
        ['default', 'heart', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
        ['secondary endpoint', 'heart', 'https://api.gravitee.io/echo', 'HTTP', '1', ''],
      ]);

      expect(await endpointsGroupHarness.getTableRows(1)).toEqual([['default', 'heart', 'https://api.gravitee.io/echo', 'HTTP', '1', '']]);
    });

    it("should display health check icon when it's configured at endpoint level", async () => {
      await initComponent(
        fakeApiV2({
          id: API_ID,
          services: {
            healthCheck: {
              enabled: false,
            },
          },
          proxy: {
            groups: [
              {
                name: 'default-group',
                endpoints: [
                  {
                    name: 'default',
                    target: 'https://api.le-systeme-solaire.net/rest/',
                    weight: 1,
                    backup: false,
                    type: 'HTTP',
                    inherit: true,
                    healthCheck: {
                      enabled: true,
                      inherit: false,
                      schedule: '0 0/1 * 1/1 * ? *',
                      steps: [],
                    },
                  },
                ],
              },
            ],
          },
        }),
      );

      expect(await rootLoader.getChildLoader('[mattooltip="Health-check is enabled locally"]')).toBeTruthy();
    });

    it("should display health check icon when it's configured at API level", async () => {
      await initComponent(
        fakeApiV2({
          id: API_ID,
          services: {
            healthCheck: {
              enabled: true,
            },
          },
        }),
      );

      expect(await rootLoader.getChildLoader('[mattooltip="Health-check is enabled (inherit)"]')).toBeTruthy();
    });

    it('should not display health check icon', async () => {
      expect.assertions(1);
      await initComponent(
        fakeApiV2({
          id: API_ID,
          services: {
            healthCheck: {
              enabled: false,
            },
          },
        }),
      );

      await rootLoader
        .getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))
        .catch(error =>
          expect(error.message).toEqual(
            'Failed to find element matching one of the following queries:\n' +
              '(_MatIconHarness with host element matching selector: ".mat-icon" satisfying the constraints: host matches selector "[mattooltip="Health check is enabled"]")',
          ),
        );
    });
  });

  describe('deleteGroup', () => {
    it('should delete the endpoint group', async () => {
      const api = fakeApiV2({
        id: API_ID,
      });
      await initComponent(api);

      expect(await endpointsGroupHarness.getTableRows(0)).toEqual([
        ['default', 'heart network-alt', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
      ]);

      await endpointsGroupHarness.deleteEndpointGroup(rootLoader);

      expectApiGetRequest(api);
      expectApiPutRequest({ ...api, proxy: { groups: [] } });
    });
  });

  describe('deleteEndpoint', () => {
    it('should delete the endpoint', async () => {
      const api = fakeApiV2({
        id: API_ID,
        proxy: {
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'default',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                },
                {
                  name: 'secondary endpoint',
                  target: 'https://api.gravitee.io/echo',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                },
              ],
            },
          ],
        },
      });
      await initComponent(api);

      expect(await endpointsGroupHarness.getTableRows(0)).toEqual([
        ['default', 'heart', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
        ['secondary endpoint', 'heart', 'https://api.gravitee.io/echo', 'HTTP', '1', ''],
      ]);

      await endpointsGroupHarness.deleteEndpoint(1, rootLoader);

      expectApiGetRequest(api);
      expectApiPutRequest({
        ...api,
        proxy: {
          groups: [
            {
              name: 'default-group',
              endpoints: [
                {
                  name: 'default',
                  target: 'https://api.le-systeme-solaire.net/rest/',
                  weight: 1,
                  backup: false,
                  type: 'HTTP',
                  inherit: false,
                },
              ],
            },
          ],
        },
      });

      expect(await endpointsGroupHarness.getTableRows(0)).toEqual([
        ['default', 'heart', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
      ]);
    });
  });

  describe('HTTP configuration', () => {
    it('should display inherit HTTP configuration icon', async () => {
      await initComponent(
        fakeApiV2({
          id: API_ID,
          proxy: {
            groups: [
              {
                name: 'default-group',
                endpoints: [
                  {
                    name: 'default',
                    target: 'https://api.le-systeme-solaire.net/rest/',
                    weight: 1,
                    backup: true,
                    type: 'HTTP',
                    inherit: true,
                  },
                ],
              },
            ],
          },
        }),
      );

      expect(await rootLoader.getChildLoader('[mattooltip="Configuration inherited"]')).toBeTruthy();
    });
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectApiPutRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }
});
