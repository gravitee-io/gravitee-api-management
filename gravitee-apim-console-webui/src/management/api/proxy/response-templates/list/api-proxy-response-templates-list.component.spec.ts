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

import { ApiProxyResponseTemplatesModule } from '../api-proxy-response-templates.module';
import { ApiProxyResponseTemplatesListComponent } from './api-proxy-response-templates-list.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { UIRouterStateParams, CurrentUserService, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { Api } from '../../../../../entities/api';

describe('ApiProxyResponseTemplatesListComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiProxyResponseTemplatesListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-response_templates-c', 'api-response_templates-u', 'api-response_templates-d'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyResponseTemplatesModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyResponseTemplatesListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('onAddResponseTemplateClicked', () => {
    it('should navigate to new Response Template page on click to add button', async () => {
      const api = fakeApi({
        id: API_ID,
      });
      expectApiGetRequest(api);

      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ text: /Add new Response Template/ })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-responsetemplate-edit', { apiId: 'apiId', key: '' });
    });
  });

  describe('onEditResponseTemplateClicked', () => {
    it('should navigate to new apis page on click to add button', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      const api = fakeApi({
        id: API_ID,
        response_templates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              status: 200,
            },
            'text/xml': {
              body: 'xml',
              status: 200,
            },
            '*/*': {
              body: 'default',
              status: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({ selector: '#responseTemplateTable' }));
      const rtTableRows = await rtTable.getRows();

      const [_1, _2, _3, _4, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

      const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Button to edit a Response Template"]' }),
      );
      await vhTableFirstRowHostInput.click();

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-responsetemplate-edit', { apiId: 'apiId', key: 'DEFAULT' });
    });
  });

  it('should display response templates table', async () => {
    const api = fakeApi({
      id: API_ID,
      response_templates: {
        DEFAULT: {
          'application/json': {
            body: 'json',
            status: 200,
          },
          'text/xml': {
            body: 'xml',
            status: 200,
          },
          '*/*': {
            body: 'default',
            status: 200,
          },
        },
      },
    });
    expectApiGetRequest(api);

    const rtTable = await loader.getHarness(MatTableHarness.with({ selector: '#responseTemplateTable' }));
    const rtTableRows = await rtTable.getCellTextByIndex();

    expect(rtTableRows).toEqual([
      ['DEFAULT', 'application/json', '200', 'json', ''],
      ['DEFAULT', 'text/xml', '200', 'xml', ''],
      ['DEFAULT', '*/*', '200', 'default', ''],
    ]);
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
