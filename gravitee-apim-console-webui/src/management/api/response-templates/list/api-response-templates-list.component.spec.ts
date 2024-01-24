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
import { ActivatedRoute } from '@angular/router';

import { ApiResponseTemplatesListComponent } from './api-response-templates-list.component';

import { ApiResponseTemplatesModule } from '../api-response-templates.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV2, ApiV4, fakeApiV2, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyResponseTemplatesListComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiResponseTemplatesListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiResponseTemplatesModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-response_templates-c', 'api-response_templates-u', 'api-response_templates-d'],
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiResponseTemplatesListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('API V2', () => {
    it('should display response templates table', async () => {
      const api = fakeApiV2({
        id: API_ID,
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
            'text/xml': {
              body: 'xml',
              statusCode: 200,
            },
            '*/*': {
              body: 'default',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableRows = await rtTable.getCellTextByIndex();

      expect(rtTableRows).toEqual([
        ['DEFAULT', 'application/json', '200', ''],
        ['DEFAULT', 'text/xml', '200', ''],
        ['DEFAULT', '*/*', '200', ''],
      ]);
    });

    it('should delete response template', async () => {
      const api = fakeApiV2({
        id: API_ID,
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
            'text/xml': {
              body: 'xml',
              statusCode: 200,
            },
            '*/*': {
              body: 'default',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableFirstRow = (await rtTable.getRows())[0];

      const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableFirstRow.getCells();

      const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
        MatButtonHarness.with({selector: '[aria-label="Button to delete a Response Template"]'}),
      );
      await vhTableFirstRowHostInput.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness);
      await (await confirmDialog.getHarness(MatButtonHarness.with({text: /^Delete/}))).click();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`
      });
      expect(req.request.body.responseTemplates['DEFAULT']['application/json']).toBeUndefined();
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApiV2({
        id: API_ID,
        definitionContext: {
          origin: 'KUBERNETES',
        },
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableRows = await rtTable.getRows();

      const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

      const allActionsBtn = await rtTableFirstRowActionsCell.getAllHarnesses(MatButtonHarness);
      expect(allActionsBtn.length).toBe(1);

      // expect open detail btn
      const opentDetailBtn = allActionsBtn[0];
      expect(await (await opentDetailBtn.host()).getAttribute('aria-label')).toBe('Button to open Response Template detail');
    });

    function expectApiGetRequest(api: ApiV2) {
      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET'
      }).flush(api);
      fixture.detectChanges();
    }
  })

  describe('API V4', () => {
    it('should display response templates table', async () => {
      const api = fakeApiV4({
        id: API_ID,
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
            'text/xml': {
              body: 'xml',
              statusCode: 200,
            },
            '*/*': {
              body: 'default',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableRows = await rtTable.getCellTextByIndex();

      expect(rtTableRows).toEqual([
        ['DEFAULT', 'application/json', '200', ''],
        ['DEFAULT', 'text/xml', '200', ''],
        ['DEFAULT', '*/*', '200', ''],
      ]);
    });

    it('should delete response template', async () => {
      const api = fakeApiV4({
        id: API_ID,
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
            'text/xml': {
              body: 'xml',
              statusCode: 200,
            },
            '*/*': {
              body: 'default',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableFirstRow = (await rtTable.getRows())[0];

      const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableFirstRow.getCells();

      const vhTableFirstRowHostInput = await rtTableFirstRowActionsCell.getHarness(
        MatButtonHarness.with({selector: '[aria-label="Button to delete a Response Template"]'}),
      );
      await vhTableFirstRowHostInput.click();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness);
      await (await confirmDialog.getHarness(MatButtonHarness.with({text: /^Delete/}))).click();

      expectApiGetRequest(api);
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`
      });
      expect(req.request.body.responseTemplates['DEFAULT']['application/json']).toBeUndefined();
    });

    it('should disable field when origin is kubernetes', async () => {
      const api = fakeApiV4({
        id: API_ID,
        definitionContext: {
          origin: 'KUBERNETES',
        },
        responseTemplates: {
          DEFAULT: {
            'application/json': {
              body: 'json',
              statusCode: 200,
            },
          },
        },
      });
      expectApiGetRequest(api);

      const rtTable = await loader.getHarness(MatTableHarness.with({selector: '#responseTemplateTable'}));
      const rtTableRows = await rtTable.getRows();

      const [_1, _2, _3, rtTableFirstRowActionsCell] = await rtTableRows[0].getCells();

      const allActionsBtn = await rtTableFirstRowActionsCell.getAllHarnesses(MatButtonHarness);
      expect(allActionsBtn.length).toBe(1);

      // expect open detail btn
      const opentDetailBtn = allActionsBtn[0];
      expect(await (await opentDetailBtn.host()).getAttribute('aria-label')).toBe('Button to open Response Template detail');
    });


    function expectApiGetRequest(api: ApiV4) {
      httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET'
      }).flush(api);
      fixture.detectChanges();
    }

  })
});
