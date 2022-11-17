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
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiProxyResponseTemplatesListComponent } from './api-proxy-response-templates-list.component';

import { ApiProxyResponseTemplatesModule } from '../api-proxy-response-templates.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { UIRouterStateParams, CurrentUserService, UIRouterState, AjsRootScope } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { Api } from '../../../../../entities/api';

describe('ApiProxyResponseTemplatesListComponent', () => {
  const API_ID = 'apiId';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiProxyResponseTemplatesListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
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
        { provide: AjsRootScope, useValue: null },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyResponseTemplatesListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

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

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.responsetemplates.new', { apiId: 'apiId' });
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

      const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

      const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Button to edit a Response Template"]' }),
      );
      await vhTableFirstRowHostInput.click();

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.responsetemplates.edit', {
        apiId: 'apiId',
        responseTemplateId: 'DEFAULT-application/json',
      });
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
      ['DEFAULT', 'application/json', '200', ''],
      ['DEFAULT', 'text/xml', '200', ''],
      ['DEFAULT', '*/*', '200', ''],
    ]);
  });

  it('should delete response template', async () => {
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
    const rtTableFirstRow = (await rtTable.getRows())[0];

    const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableFirstRow.getCells();

    const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
      MatButtonHarness.with({ selector: '[aria-label="Button to delete a Response Template"]' }),
    );
    await vhTableFirstRowHostInput.click();

    const confirmDialog = await rootLoader.getHarness(MatDialogHarness);
    await (await confirmDialog.getHarness(MatButtonHarness.with({ text: /^Delete/ }))).click();

    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}` });
    expect(req.request.body.response_templates['DEFAULT']['application/json']).toBeUndefined();
  });

  it('should disable field when origin is kubernetes', async () => {
    const routerSpy = jest.spyOn(fakeUiRouter, 'go');

    const api = fakeApi({
      id: API_ID,
      definition_context: {
        origin: 'kubernetes',
      },
      response_templates: {
        DEFAULT: {
          'application/json': {
            body: 'json',
            status: 200,
          },
        },
      },
    });
    expectApiGetRequest(api);

    const rtTable = await loader.getHarness(MatTableHarness.with({ selector: '#responseTemplateTable' }));
    const rtTableRows = await rtTable.getRows();

    const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

    const allActionsBtn = await rtTableFirstRowActionsCell.getAllHarnesses(MatButtonHarness);
    expect(allActionsBtn.length).toBe(1);

    // expect open detail btn
    const opentDetailBtn = allActionsBtn[0];
    expect(await (await opentDetailBtn.host()).getAttribute('aria-label')).toBe('Button to open Response Template detail');

    await opentDetailBtn.click();

    expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.responsetemplates.edit', {
      apiId: 'apiId',
      responseTemplateId: 'DEFAULT-application/json',
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
