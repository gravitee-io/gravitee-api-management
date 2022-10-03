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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';

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
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyEndpointListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

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

      const [_1, _2, _3, _4, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

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
                inherit: true,
              },
              {
                name: 'secondary endpoint',
                target: 'https://api.gravitee.io/echo',
                weight: 1,
                backup: false,
                type: 'HTTP',
                inherit: true,
              },
            ],
            load_balancing: {
              type: 'ROUND_ROBIN',
            },
            http: {
              connectTimeout: 5000,
              idleTimeout: 60000,
              keepAlive: true,
              readTimeout: 10000,
              pipelining: false,
              maxConcurrentConnections: 100,
              useCompression: true,
              followRedirects: false,
            },
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
                inherit: true,
              },
            ],
            load_balancing: {
              type: 'ROUND_ROBIN',
            },
            http: {
              connectTimeout: 5000,
              idleTimeout: 60000,
              keepAlive: true,
              readTimeout: 10000,
              pipelining: false,
              maxConcurrentConnections: 100,
              useCompression: true,
              followRedirects: false,
            },
          },
        ],
      },
    });
    expectApiGetRequest(api);

    const rtTable0 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-0' }));
    const rtTableRows0 = await rtTable0.getCellTextByIndex();

    expect(rtTableRows0).toEqual([
      ['default', 'https://api.le-systeme-solaire.net/rest/', 'HTTP', '1', ''],
      ['secondary endpoint', 'https://api.gravitee.io/echo', 'HTTP', '1', ''],
    ]);

    const rtTable1 = await loader.getHarness(MatTableHarness.with({ selector: '#endpointGroupsTable-1' }));
    const rtTableRows1 = await rtTable1.getCellTextByIndex();

    expect(rtTableRows1).toEqual([['default', 'https://api.gravitee.io/echo', 'HTTP', '1', '']]);
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
