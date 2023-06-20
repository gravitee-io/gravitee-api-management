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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { UIRouterGlobals } from '@uirouter/core';

import { ApiProxyEndpointListComponent } from './api-proxy-endpoint-list.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { ApiProxyEndpointModule } from '../api-proxy-endpoints.module';
import { ApiV2, fakeApiV2 } from '../../../../../entities/management-api-v2';

describe('ApiProxyEndpointListComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiProxyEndpointListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-r'];

  const initComponent = (api: ApiV2): void => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyEndpointModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterGlobals, useValue: { current: { data: { baseRouteState: null } } } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });

    fixture = TestBed.createComponent(ApiProxyEndpointListComponent);
    fixture.componentInstance.api = api;

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('navigateToGroup', () => {
    let routerSpy: jest.SpyInstance;
    beforeEach(() => {
      initComponent(
        fakeApiV2({
          id: API_ID,
        }),
      );
      routerSpy = jest.spyOn(fakeUiRouter, 'go');
    });

    it('should navigate to new Proxy Endpoint Group page on click to add button', async () => {
      await loader.getHarness(MatButtonHarness.with({ text: /Add new endpoint group/ })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.group', { groupName: '' });
    });

    it('should navigate to existing group', async () => {
      await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Edit group"]' })).then((btn) => btn.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.group', { groupName: 'default-group' });
    });
  });

  describe('navigateToEndpoint', () => {
    it.each`
      definitionContext | buttonAreaLabel
      ${'MANAGEMENT'}   | ${'Button to edit an endpoint'}
      ${'KUBERNETES'}   | ${'Button to open endpoint detail'}
    `('should be able to open an endpoint for API with origin $definitionContext', async ({ definitionContext, buttonAreaLabel }) => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');
      initComponent(
        fakeApiV2({
          id: API_ID,
          definitionContext: { origin: definitionContext },
        }),
      );

      const rtTable = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
      const rtTableRows = await rtTable.getRows();

      const [_1, _2, _3, _4, _5, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

      const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
        MatButtonHarness.with({ selector: `[aria-label="${buttonAreaLabel}"]` }),
      );
      await vhTableFirstRowHostInput.click();

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.endpoint', {
        groupName: 'default-group',
        endpointName: 'default',
      });
    });
  });

  describe('mat table tests', () => {
    it('should display the endpoint groups tables', async () => {
      initComponent(
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

      const rtTable0 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
      const rtTableRows0 = await rtTable0.getCellTextByIndex();

      expect(rtTableRows0).toEqual([
        ['default', 'favorite', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
        ['secondary endpoint', 'favorite', 'https://api.gravitee.io/echo', 'HTTP', '1', ''],
      ]);

      const rtTable1 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-1' }));
      const rtTableRows1 = await rtTable1.getCellTextByIndex();

      expect(rtTableRows1).toEqual([['default', 'favorite', 'https://api.gravitee.io/echo', 'HTTP', '1', '']]);
    });

    it("should display health check icon when it's configured at endpoint level", async () => {
      initComponent(
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

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))).toBeTruthy();
    });

    it("should display health check icon when it's configured at API level", async () => {
      initComponent(
        fakeApiV2({
          id: API_ID,
          services: {
            healthCheck: {
              enabled: true,
            },
          },
        }),
      );

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))).toBeTruthy();
    });

    it('should not display health check icon', async () => {
      expect.assertions(1);
      initComponent(
        fakeApiV2({
          id: API_ID,
          services: {
            healthCheck: {
              enabled: false,
            },
          },
        }),
      );

      await loader
        .getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))
        .catch((error) =>
          expect(error.message).toEqual(
            'Failed to find element matching one of the following queries:\n' +
              '(MatIconHarness with host element matching selector: ".mat-icon" satisfying the constraints: host matches selector "[mattooltip="Health check is enabled"]")',
          ),
        );
    });
  });

  describe('deleteGroup', () => {
    it('should delete the endpoint group', async () => {
      const api = fakeApiV2({
        id: API_ID,
      });
      initComponent(api);

      const rtTable0 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
      let rtTableRows0 = await rtTable0.getCellTextByIndex();
      expect(rtTableRows0).toEqual([
        ['default', 'favoritesubdirectory_arrow_right', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
      ]);

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Delete group"]' })).then((element) => element.click());
      await rootLoader
        .getHarness(MatDialogHarness)
        .then((dialog) => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
        .then((element) => element.click());

      expectApiGetRequest(api);
      expectApiPutRequest({ ...api, proxy: { groups: [] } });
      rtTableRows0 = await rtTable0.getCellTextByIndex();
      expect(rtTableRows0).toEqual([]);
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

      let rtTable0 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
      let rtTableRows0 = await rtTable0.getCellTextByIndex();
      expect(rtTableRows0).toEqual([
        ['default', 'favorite', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
        ['secondary endpoint', 'favorite', 'https://api.gravitee.io/echo', 'HTTP', '1', ''],
      ]);

      await loader
        .getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Delete endpoint"]' }))
        .then((elements) => elements[1].click());
      await rootLoader
        .getHarness(MatDialogHarness)
        .then((dialog) => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
        .then((element) => element.click());

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

      rtTable0 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
      rtTableRows0 = await rtTable0.getCellTextByIndex();
      expect(rtTableRows0).toEqual([['default', 'favorite', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', '']]);
    });
  });

  describe('HTTP configuration', () => {
    it('should display inherit HTTP configuration icon', async () => {
      initComponent(
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

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="HTTP configuration inherited"]' }))).toBeTruthy();
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
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }
});
