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

import { ApiPathMappingsEditDialogComponent } from './api-path-mappings-edit-dialog.component';

import { ApiPathMappingsModule } from '../api-path-mappings.module';
import { GioTestingModule } from '../../../../../shared/testing';
import { fakeApiV2 } from '../../../../../entities/management-api-v2';

describe('ApiPathMappingsEditDialogComponent', () => {
  const API_ID = 'apiId';
  const api = fakeApiV2({ id: API_ID, pathMappings: ['/test', '/test/:id'] });

  let fixture: ComponentFixture<ApiPathMappingsEditDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiPathMappingsModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { api, path: '/test' } },
        { provide: MatDialogRef, useValue: {} },
      ],
    });

    fixture = TestBed.createComponent(ApiPathMappingsEditDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('form tests', () => {
    it('should init the form with the path value', async () => {
      const input = await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }));
      expect(await input.getValue()).toStrictEqual('/test');
    });

    it('should save updated path', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then(input => input.setValue('/test2'));
      expect(
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Save path mapping"]' })).then(btn => btn.isDisabled()),
      ).toStrictEqual(false);
    });

    it('should not be able to save existing path', async () => {
      await loader
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then(input => input.setValue('/test/:id'));

      expect(
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Save path mapping"]' })).then(btn => btn.isDisabled()),
      ).toStrictEqual(true);
    });
  });
});
