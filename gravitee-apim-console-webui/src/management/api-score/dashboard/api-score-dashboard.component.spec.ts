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
import { ApisScoring, ApisScoringOverview, ApisScoringResponse } from '../api-score.model';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiScoreDashboardComponent', () => {
  let fixture: ComponentFixture<ApiScoreDashboardComponent>;
  let componentHarness: ApiScoreDashboardHarness;
  let httpTestingController: HttpTestingController;
  const fakeSnackBarService = {
    success: jest.fn(),
    error: jest.fn(),
  };

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiScoreDashboardComponent],
      imports: [GioTestingModule, ApiScoreModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [{ provide: SnackBarService, useValue: fakeSnackBarService }],
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
    afterEach(() => {
      httpTestingController.verify();
    });

    it('should have correct number of rows', async () => {
      await init();
      expectApiScoreGetRequest([fakeApisScoring(), fakeApisScoring(), fakeApisScoring()]);
      expectApisOverviewGetRequest();

      fixture.detectChanges();
      expect(await componentHarness.rowsNumber()).toEqual(3);
    });
  });

  describe('errors', () => {
    afterEach(() => {
      httpTestingController.verify();
    });

    it('in table should be handled with messages', async () => {
      await init();
      expectApiScoreGetRequest();
      expectApisOverviewError();

      fixture.detectChanges();

      expect(fakeSnackBarService.error).toHaveBeenCalledWith('Error found');
    });

    it('in overview should be handled with messages', async () => {
      await init();

      expectApisOverviewGetRequest();
      expectApiScoreError();

      fixture.detectChanges();

      expect(fakeSnackBarService.error).toHaveBeenCalledWith('Overview error');
    });
  });

  function expectApisOverviewError() {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/overview`,
      method: 'GET',
    });

    req.flush({ message: 'Error found' }, { status: 404, statusText: 'Error found' });

    fixture.detectChanges();
  }

  function expectApisOverviewGetRequest(): void {
    const response: ApisScoringOverview = {
      id: 'DEFAULT',
      errors: 0,
      warnings: 15,
      infos: 0,
      hints: 0,
      score: 0.9,
    };

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/overview`,
        method: 'GET',
      })
      .flush(response);
  }

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

  function expectApiScoreError(): void {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/scoring/apis?page=1&perPage=10`,
        method: 'GET',
      })
      .flush({ message: 'Overview error' }, { status: 404, statusText: 'Overview error' });
  }
});
