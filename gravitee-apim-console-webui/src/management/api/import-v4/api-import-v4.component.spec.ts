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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiImportV4Component } from './api-import-v4.component';
import { ApiImportV4Harness } from './api-import-v4.harness';

import { GioTestingModule } from '../../../shared/testing';

describe('ImportV4Component', () => {
  let fixture: ComponentFixture<ApiImportV4Component>;
  let componentHarness: ApiImportV4Harness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiImportV4Component, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportV4Component);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiImportV4Harness);
    fixture.detectChanges();
  });

  it('should not be able to save when form is invalid', async () => {
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
  });
});
