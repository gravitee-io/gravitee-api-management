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

import { ApiProxyEndpointListComponent } from './api-proxy-endpoint-list.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { Api } from '../../../../../entities/api';
import { ApiProxyEndpointModule } from '../api-proxy-endpoints.module';

describe('ApiProxyEndpointListComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiProxyEndpointListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-r'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyEndpointModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });

    fixture = TestBed.createComponent(ApiProxyEndpointListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('navigateToGroup', () => {
    it('should navigate to new Proxy Endpoint Group page on click to add button', async () => {
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ text: /Add new endpoint group/ })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.group', { groupName: '' });
    });

    it('should navigate to existing group', async () => {
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Edit group"]' })).then((btn) => btn.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.group', { groupName: 'default-group' });
    });
  });

  describe('navigateToEndpoint', () => {
    it.each`
      definitionContext | buttonAreaLabel
      ${'management'}   | ${'Button to edit an endpoint'}
      ${'kubernetes'}   | ${'Button to open endpoint detail'}
    `('should be able to open an endpoint for API with origin $definitionContext', async ({ definitionContext, buttonAreaLabel }) => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      const api = fakeApi({
        id: API_ID,
        definition_context: { origin: definitionContext },
      });
      expectApiGetRequest(api);

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
    it('should display the first endpoint group table', async () => {
      const api = fakeApi({
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
      });
      expectApiGetRequest(api);

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
      const api = fakeApi({
        id: API_ID,
        services: {
          'health-check': {
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
                  healthcheck: {
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
      });
      expectApiGetRequest(api);

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))).toBeTruthy();
    });

    it("should display health check icon when it's configured at API level", async () => {
      const api = fakeApi({
        id: API_ID,
        services: {
          'health-check': {
            enabled: true,
          },
        },
      });
      expectApiGetRequest(api);

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="Health check is enabled"]' }))).toBeTruthy();
    });

    it('should not display health check icon', async () => {
      expect.assertions(1);
      const api = fakeApi({
        id: API_ID,
        services: {
          'health-check': {
            enabled: false,
          },
        },
      });
      expectApiGetRequest(api);

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
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);

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
      const api = fakeApi({
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
      expectApiGetRequest(api);

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
      const api = fakeApi({
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
      });
      expectApiGetRequest(api);

      expect(await loader.getHarness(MatIconHarness.with({ selector: '[mattooltip="HTTP configuration inherited"]' }))).toBeTruthy();
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' }).flush(api);
    fixture.detectChanges();
  }
});
