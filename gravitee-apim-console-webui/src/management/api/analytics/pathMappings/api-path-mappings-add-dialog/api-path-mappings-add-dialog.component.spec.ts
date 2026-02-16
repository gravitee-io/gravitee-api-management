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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { ApiPathMappingsAddDialogComponent } from './api-path-mappings-add-dialog.component';

import { ApiPathMappingsModule } from '../api-path-mappings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV2, fakeApiV2 } from '../../../../../entities/management-api-v2';
import { mapDefinitionVersionToLabel } from '../../../../../shared/utils/api.util';

describe('ApiPathMappingsEditDialogComponent', () => {
  const API_ID = 'apiId';
  const api = fakeApiV2({ id: API_ID, pathMappings: ['/test', '/test/:id'] });
  const matDialogRefMock = {
    close: jest.fn(),
  };

  let fixture: ComponentFixture<ApiPathMappingsAddDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiPathMappingsModule],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            api,
            swaggerDocs: [
              {
                name: 'Swagger 1',
                id: 'swagger1',
              },
              {
                name: 'Swagger 2',
                id: 'swagger2',
              },
            ],
          },
        },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });

    fixture = TestBed.createComponent(ApiPathMappingsAddDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('form tests', () => {
    it('should save new path mapping', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then(input => input.setValue('/test2'));

      const addBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add path mapping"]' }));
      expect(await addBtn.isDisabled()).toStrictEqual(false);
      await addBtn.click();
      expectApiGetRequest(api);
      expectApiPutRequest(api);
    });

    it('should not be able to save existing path', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then(input => input.setValue('/test/:id'));

      expect(
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add path mapping"]' })).then(btn => btn.isDisabled()),
      ).toStrictEqual(true);
    });
  });

  describe('import swagger tests', () => {
    it('should import swagger', async () => {
      await loader.getHarness(MatTabHarness.with({ label: 'Swagger Document' })).then(tab => tab.select());

      expect(
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add path mapping"]' })).then(btn => btn.isDisabled()),
      ).toStrictEqual(true);

      await loader.getHarness(MatRadioGroupHarness).then(radioGroup => radioGroup.checkRadioButton({ label: /^Swagger 2/ }));
      fixture.detectChanges();

      const addBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add path mapping"]' }));
      expect(await addBtn.isDisabled()).toStrictEqual(false);
      await addBtn.click();
      expectApiGetRequest(api);
      expectPathMappingImportRequest(api, 'swagger2');
    });
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectPathMappingImportRequest(api: ApiV2, pageId: string) {
    const defVersion = mapDefinitionVersionToLabel(api.definitionVersion);
    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/import-path-mappings?page=${pageId}&definitionVersion=${defVersion}`,
      })
      .flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV2) {
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}` });
    expect(req.request.body).toBeTruthy();
    expect(req.request.body.pathMappings).toStrictEqual(api.pathMappings);
    req.flush(api);
    fixture.detectChanges();
  }
});
