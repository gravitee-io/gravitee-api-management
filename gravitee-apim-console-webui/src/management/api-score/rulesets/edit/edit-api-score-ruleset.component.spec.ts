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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { EditApiScoreRulesetComponent } from './edit-api-score-ruleset.component';
import { EditApiScoreRulesetHarness } from './edit-api-score-ruleset.harness';

import { ApiScoreRulesetsModule } from '../api-score-rulesets.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeRuleset } from '../../../../entities/management-api-v2/api/v4/ruleset.fixture';
import { ScoringRuleset } from '../../../../entities/management-api-v2/api/v4/ruleset';

describe('EditApiScoreRulesetComponent', () => {
  let fixture: ComponentFixture<EditApiScoreRulesetComponent>;
  const id = 'testID';

  let componentHarness: EditApiScoreRulesetHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, ApiScoreRulesetsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { id } } } }],
    }).compileComponents();

    fixture = TestBed.createComponent(EditApiScoreRulesetComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EditApiScoreRulesetHarness);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should not send empty form', async () => {
    expectGetRulesetRequest();
    const saveBar = await componentHarness.saveBarLocator();
    expect(saveBar).toBeFalsy();
  });

  it('should send request with data form', async () => {
    expectGetRulesetRequest();

    await componentHarness.setName('Test ruleset name');
    await componentHarness.setDescription('Test description');

    fixture.detectChanges();

    expect(fixture.componentInstance.form.value.name).toEqual('Test ruleset name');
    expect(fixture.componentInstance.form.value.description).toEqual('Test description');

    const saveBar = await componentHarness.saveBarLocator();
    expect(saveBar).toBeTruthy();

    await saveBar.clickSubmit();

    expectPutRulesetRequest();
    expectGetRulesetRequest();
  });

  it('should delete ruleset', async () => {
    expectGetRulesetRequest();

    const deleteButton = await componentHarness.deleteRulesetButton();
    await deleteButton.click();

    fixture.detectChanges();

    const dialogHarness: GioConfirmDialogHarness =
      await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    await dialogHarness.confirm();

    expectDeleteRulesetRequest();
  });

  const expectGetRulesetRequest = (res: ScoringRuleset = fakeRuleset()) => {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets/${id}`;
    const req = httpTestingController.expectOne({ method: 'GET', url: url });
    req.flush(res);
  };

  const expectPutRulesetRequest = () => {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets/${id}`;
    const req = httpTestingController.expectOne({ method: 'PUT', url: url });

    expect(req.request.body).toBeTruthy();
    expect(req.request.body.description).toEqual('Test description');
    expect(req.request.body.name).toEqual('Test ruleset name');

    req.flush(fakeRuleset());
  };

  const expectDeleteRulesetRequest = (res: ScoringRuleset = fakeRuleset()) => {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/rulesets/${id}`;
    const req = httpTestingController.expectOne({ method: 'DELETE', url: url });
    req.flush(res);
  };
});
