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

import { ImportApiScoreRulesetComponent } from './import-api-score-ruleset.component';
import { ImportApiScoreRulesetHarness } from './import-api-score-ruleset.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiScoreRulesetsModule } from '../api-score-rulesets.module';
import { CreateRulesetRequestData } from '../../../../entities/management-api-v2/api/v4/ruleset';

describe('NewRulesetComponent', () => {
  let fixture: ComponentFixture<ImportApiScoreRulesetComponent>;
  let componentHarness: ImportApiScoreRulesetHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, ApiScoreRulesetsModule, BrowserAnimationsModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ImportApiScoreRulesetComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ImportApiScoreRulesetHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should not submit empty form', async () => {
    const importButton = await componentHarness.locatorForSubmitImportButton();
    expect(await importButton.getText()).toEqual('Import');
    expect(await importButton.isDisabled()).toEqual(true);
  });

  it('should send request with values selected in form', async () => {
    const definitionFormat = await componentHarness.locatorForDefinitionFormatRadioGroup();
    await definitionFormat.select('OpenAPI, Async API');
    expect(await definitionFormat.getSelectedValue()).toEqual('OpenAPI, Async API');

    await componentHarness.setName('Test ruleset name');
    await componentHarness.setDescription('Test description');

    fixture.detectChanges();

    expect(fixture.componentInstance.form.value.name).toEqual('Test ruleset name');
    expect(fixture.componentInstance.form.value.description).toEqual('Test description');

    await componentHarness.pickFiles([new File([JSON.stringify('test')], 'gravitee-api-definition.json', { type: 'application/json' })]);
    const importButton = await componentHarness.locatorForSubmitImportButton();
    expect(await importButton.isDisabled()).toEqual(false);

    await importButton.click();

    const data = {
      description: 'Test description',
      name: 'Test ruleset name',
      payload: '"test"',
    };

    expectCreateRulesetRequest(data);
  });

  function expectCreateRulesetRequest(data: CreateRulesetRequestData) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets` });
    expect(req.request.body).toBeTruthy();
    expect(req.request.body).toEqual(data);
    req.flush(null);
    fixture.detectChanges();
  }
});
