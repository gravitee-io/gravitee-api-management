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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiScoreDashboardComponent } from './api-score-dashboard.component';
import { ApiScoreDashboardHarness } from './api-score-dashboard.harness';

import { ApiScoreModule } from '../api-score.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApisScoring } from '../../api/scoring/api-scoring.fixture';
import { ApisScoring, ApisScoringResponse } from '../api-score.model';

describe('ApiScoreDashboardComponent', () => {
  let fixture: ComponentFixture<ApiScoreDashboardComponent>;
  let componentHarness: ApiScoreDashboardHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiScoreDashboardComponent],
      imports: [GioTestingModule, ApiScoreModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiScoreDashboardComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreDashboardHarness);
    fixture.detectChanges();
  };

  describe('table', () => {
    it('should have correct number of rows', async () => {
      await init();
      expectApiScoreGetRequest([fakeApisScoring(), fakeApisScoring(), fakeApisScoring()]);

      fixture.detectChanges();
      expect(await componentHarness.rowsNumber()).toEqual(3);
    });
  });

  function expectApiScoreGetRequest(responseData: ApisScoring[] = [fakeApisScoring()], page = 1, perPage = 10): void {
    const response: ApisScoringResponse = {
      data: responseData,
      pagination: {
        totalCount: responseData.length,
      },
    };

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/apis?page=${page}&perPage=${perPage}`,
        method: 'GET',
      })
      .flush(response);
  }
});
