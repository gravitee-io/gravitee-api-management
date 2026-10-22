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
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiScoreRulesetsComponent } from './api-score-rulesets.component';
import { ApiScoreRulesetsHarness } from './api-score-rulesets.harness';

import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiScoreModule } from '../api-score.module';
import { fakeRulesetsList, fakeScoringFunction } from '../../../entities/management-api-v2/api/v4/ruleset.fixture';
import { ScoringFunction, RulesetFormat, ScoringRulesetsResponse } from '../../../entities/management-api-v2/api/v4/ruleset';

function expectListRulesets(httpTestingController: HttpTestingController, res = fakeRulesetsList()) {
  const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets`;
  const req = httpTestingController.expectOne(req => {
    return req.method === 'GET' && req.url.startsWith(url);
  });
  req.flush(res);
}

function expectListFunctions(
  httpTestingController: HttpTestingController,
  res: { data: ScoringFunction[] } = { data: [fakeScoringFunction()] },
) {
  const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/functions`;
  const req = httpTestingController.expectOne({ method: 'GET', url: url });
  req.flush(res);
}

async function initTestBed(permissions: string[]): Promise<{
  fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  httpTestingController: HttpTestingController;
  componentHarness: ApiScoreRulesetsHarness;
}> {
  await TestBed.configureTestingModule({
    imports: [GioTestingModule, ApiScoreModule, BrowserAnimationsModule, NoopAnimationsModule],
    providers: [{ provide: GioTestingPermissionProvider, useValue: permissions }],
  }).compileComponents();

  const fixture = TestBed.createComponent(ApiScoreRulesetsComponent);
  const httpTestingController = TestBed.inject(HttpTestingController);
  const componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreRulesetsHarness);

  return { fixture, httpTestingController, componentHarness };
}

describe('ApiScoreRulesetsComponent', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  const allPermissions = ['environment-api-c', 'environment-api-r', 'environment-api-u', 'environment-api-d'];

  beforeEach(async () => {
    ({ fixture, httpTestingController, componentHarness } = await initTestBed(allPermissions));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  [
    { input: {}, expected: 'Test ruleset name' },
    { input: { format: RulesetFormat.GRAVITEE_MESSAGE }, expected: 'Test ruleset name Gravitee Message API' },
    { input: { format: RulesetFormat.GRAVITEE_PROXY }, expected: 'Test ruleset name Gravitee Proxy API' },
  ].forEach(testParams => {
    it('should display rulesets for format: ' + testParams.input.format, async () => {
      const rulesetResponse: ScoringRulesetsResponse = {
        data: [
          {
            name: 'Test ruleset name',
            description: 'Test ruleset description',
            payload: 'Ruleset payload',
            id: 'ruleset-id',
            format: testParams.input.format,
            createdAt: '2024-11-19T12:41:18.85Z',
            referenceId: 'DEFAULT',
            referenceType: 'ENVIRONMENT',
          },
        ],
      };
      expectListRulesets(httpTestingController, fakeRulesetsList(rulesetResponse));
      expectListFunctions(httpTestingController);

      const matExpansionPanel = await componentHarness.getMatExpansionPanelHarness();
      const title = await matExpansionPanel.getTitle();
      const description = await matExpansionPanel.getDescription();

      expect(title).toEqual(testParams.expected);
      expect(description).toEqual('Test ruleset description');

      const matCardHarness = await componentHarness.getRulesetsEmpty();
      expect(matCardHarness).toBeFalsy();
    });
  });

  it('shows import, edit, and delete when the user can change scoring configuration', async () => {
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController);
    fixture.detectChanges();
    await componentHarness.expandAllPanels();

    expect(await componentHarness.getImportRulesetButton()).toBeTruthy();
    expect(await componentHarness.getImportFunctionButton()).toBeTruthy();
    expect(await componentHarness.getEditRulesetButton()).toBeTruthy();
    expect(await componentHarness.getDeleteRulesetButton()).toBeTruthy();
    expect(await componentHarness.getDeleteFunctionButton()).toBeTruthy();
  });

  it('should display no functions info if backend returns empty list of rulesets', async () => {
    const emptyFunctionsRes = { data: [] };
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController, emptyFunctionsRes);

    const matCardHarness = await componentHarness.getFunctionsEmpty();
    expect(matCardHarness).toBeTruthy();
  });
});

describe('ApiScoreRulesetsComponent without write permission', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    ({ fixture, httpTestingController, componentHarness } = await initTestBed(['environment-api-r']));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('hides import, edit, and delete', async () => {
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController);
    fixture.detectChanges();
    await componentHarness.expandAllPanels();

    expect(await componentHarness.getImportRulesetButton()).toBeNull();
    expect(await componentHarness.getImportFunctionButton()).toBeNull();
    expect(await componentHarness.getEditRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteFunctionButton()).toBeNull();
  });
});

describe('ApiScoreRulesetsComponent with update permission only', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    ({ fixture, httpTestingController, componentHarness } = await initTestBed(['environment-api-r', 'environment-api-u']));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('shows function import and ruleset edit, hides create and delete', async () => {
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController);
    fixture.detectChanges();
    await componentHarness.expandAllPanels();

    expect(await componentHarness.getImportRulesetButton()).toBeNull();
    expect(await componentHarness.getImportFunctionButton()).toBeTruthy();
    expect(await componentHarness.getEditRulesetButton()).toBeTruthy();
    expect(await componentHarness.getDeleteRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteFunctionButton()).toBeNull();
  });
});

describe('ApiScoreRulesetsComponent with create permission only', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    ({ fixture, httpTestingController, componentHarness } = await initTestBed(['environment-api-r', 'environment-api-c']));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('shows import ruleset and import function, hides edit and delete', async () => {
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController);
    fixture.detectChanges();
    await componentHarness.expandAllPanels();

    expect(await componentHarness.getImportRulesetButton()).toBeTruthy();
    expect(await componentHarness.getImportFunctionButton()).toBeTruthy();
    expect(await componentHarness.getEditRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteFunctionButton()).toBeNull();
  });
});

describe('ApiScoreRulesetsComponent with delete permission only', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    ({ fixture, httpTestingController, componentHarness } = await initTestBed(['environment-api-r', 'environment-api-d']));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('shows delete buttons, hides import and edit', async () => {
    expectListRulesets(httpTestingController);
    expectListFunctions(httpTestingController);
    fixture.detectChanges();
    await componentHarness.expandAllPanels();

    expect(await componentHarness.getImportRulesetButton()).toBeNull();
    expect(await componentHarness.getImportFunctionButton()).toBeNull();
    expect(await componentHarness.getEditRulesetButton()).toBeNull();
    expect(await componentHarness.getDeleteRulesetButton()).toBeTruthy();
    expect(await componentHarness.getDeleteFunctionButton()).toBeTruthy();
  });
});
