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
import { fakeRulesetsList } from '../../../entities/management-api-v2/api/v4/ruleset.fixture';

describe('ApiScoreRulesetsComponent', () => {
  let component: ApiScoreRulesetsComponent;
  let fixture: ComponentFixture<ApiScoreRulesetsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiScoreRulesetsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, ApiScoreModule, BrowserAnimationsModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiScoreRulesetsComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreRulesetsHarness);
    fixture.detectChanges();
  });

  it('should display rulesets', async () => {
    expectListRulesets();
    expect(component).toBeTruthy();

    const matExpansionPanel = await componentHarness.getMatExpansionPanelHarness();
    const title = await matExpansionPanel.getTitle();
    const description = await matExpansionPanel.getDescription();

    expect(title).toEqual('Test ruleset name');
    expect(description).toEqual('Test ruleset description');
  });

  it('should display no ruleset info if backend returns empty list of rulesets', async () => {
    const emptyRulesetList = { data: [] };
    expectListRulesets(emptyRulesetList);

    const matCardHarness = await componentHarness.getMatCardHarness();

    const content = await matCardHarness.getText();

    expect(content).toContain('No rulesets');
  });

  function expectListRulesets(res = fakeRulesetsList()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }
});
