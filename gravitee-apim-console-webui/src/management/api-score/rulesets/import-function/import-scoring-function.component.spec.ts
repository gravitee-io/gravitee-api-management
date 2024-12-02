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
import { HttpTestingController } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ImportScoringFunctionComponent } from './import-scoring-function.component';
import { ImportScoringFunctionHarness } from './import-scoring-function.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiScoreRulesetsModule } from '../api-score-rulesets.module';
import { CreateFunctionRequestData, ScoringFunction } from '../../../../entities/management-api-v2/api/v4/ruleset';
import { fakeScoringFunction } from '../../../../entities/management-api-v2/api/v4/ruleset.fixture';

describe('ImportScoringFunctionComponent', () => {
  let componentHarness: ImportScoringFunctionHarness;
  let fixture: ComponentFixture<ImportScoringFunctionComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, ApiScoreRulesetsModule, BrowserAnimationsModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ImportScoringFunctionComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ImportScoringFunctionHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should not send empty form', async () => {
    expectGetFunctionsRequest();
    const importButton = await componentHarness.locatorForSubmitImportButton();
    expect(await importButton.isDisabled()).toEqual(true);
  });

  it('should send form', async () => {
    expectGetFunctionsRequest();
    await componentHarness.pickFiles([new File([JSON.stringify('test')], 'testFile.js', { type: 'js' })]);
    const importButton = await componentHarness.locatorForSubmitImportButton();
    expect(await importButton.isDisabled()).toEqual(false);

    await importButton.click();

    const data = {
      name: 'testFile.js',
      payload: '"test"',
    };

    expectCreateFunctionRequest(data);
  });

  function expectCreateFunctionRequest(data: CreateFunctionRequestData) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/functions` });
    expect(req.request.body).toBeTruthy();
    expect(req.request.body).toEqual(data);
    req.flush(null);
    fixture.detectChanges();
  }

  function expectGetFunctionsRequest(res: { data: ScoringFunction[] } = { data: [fakeScoringFunction()] }) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/functions`;
    const req = httpTestingController.expectOne({ method: 'GET', url: url });
    req.flush(res);
  }
});
