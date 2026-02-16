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

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiScoreModule } from '../api-score.module';
import { fakeRulesetsList, fakeScoringFunction } from '../../../entities/management-api-v2/api/v4/ruleset.fixture';
import { ScoringFunction, RulesetFormat, ScoringRulesetsResponse } from '../../../entities/management-api-v2/api/v4/ruleset';

describe('ApiScoreRulesetsComponent', () => {
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, ApiScoreModule, BrowserAnimationsModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiScoreRulesetsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreRulesetsHarness);
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
      expectListRulesets(fakeRulesetsList(rulesetResponse));
      expectListFunctions();

      const matExpansionPanel = await componentHarness.getMatExpansionPanelHarness();
      const title = await matExpansionPanel.getTitle();
      const description = await matExpansionPanel.getDescription();

      expect(title).toEqual(testParams.expected);
      expect(description).toEqual('Test ruleset description');

      const matCardHarness = await componentHarness.getRulesetsEmpty();
      expect(matCardHarness).toBeFalsy();
    });
  });

  it('should display no functions info if backend returns empty list of rulesets', async () => {
    const emptyFunctionsRes = { data: [] };
    expectListRulesets();
    expectListFunctions(emptyFunctionsRes);

    const matCardHarness = await componentHarness.getFunctionsEmpty();
    expect(matCardHarness).toBeTruthy();
  });

  function expectListRulesets(res = fakeRulesetsList()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }

  function expectListFunctions(res: { data: ScoringFunction[] } = { data: [fakeScoringFunction()] }) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/functions`;
    const req = httpTestingController.expectOne({ method: 'GET', url: url });
    req.flush(res);
  }
});
