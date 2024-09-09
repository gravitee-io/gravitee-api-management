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

import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiScoringComponent } from './api-scoring.component';
import { ApiScoringHarness } from './api-scoring.harness';
import { ApiScoringModule } from './api-scoring.module';
import { fakeApiScoring, fakeApiScoringTriggerResponse } from './api-scoring.fixture';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiFederated } from '../../../entities/management-api-v2';
import { AsyncJob, fakeAsyncJob } from '../../../entities/async-job';
import { fakePaginatedResult } from '../../../entities/paginatedResult';

describe('ApiScoringComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiScoringComponent>;
  let componentHarness: ApiScoringHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiScoringComponent],
      imports: [ApiScoringModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
            },
          },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiScoringComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoringHarness);

    fixture.detectChanges();
  };

  describe('initialize', () => {
    it('should display loading panel while loading', fakeAsync(async () => {
      await init();
      expect(await componentHarness.getLoaderPanel()).not.toBeNull();

      discardPeriodicTasks();
    }));

    it('should disable evaluate button when request is pending', fakeAsync(async () => {
      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, [fakeAsyncJob({ sourceId: API_ID, status: 'PENDING' })]);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID);

      expect(await componentHarness.evaluateButtonDisabled()).toBeTruthy();
      discardPeriodicTasks();
    }));

    it('should show result summary when loaded', fakeAsync(async () => {
      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID);

      expect(await componentHarness.getSummaryText()).toEqual(['All (1)', 'Errors (0)', 'Warnings (1)', 'Infos (0)', 'Hints (0)']);
    }));
  });

  describe('evaluate', () => {
    it('should trigger API scoring on click', fakeAsync(async () => {
      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID);

      await componentHarness.clickEvaluate();
      tick(1);

      expectApiScorePostRequest('api-id');
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID);
    }));
  });

  function expectApiGetRequest(apiId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
        method: 'GET',
      })
      .flush(fakeApiFederated({ id: apiId }));
  }

  function expectApiScoreGetRequest(apiId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/scoring`,
        method: 'GET',
      })
      .flush(fakeApiScoring());
  }

  function expectAsyncJobGetRequest(apiId: string, data: AsyncJob[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/async-jobs?page=1&perPage=10&type=SCORING_REQUEST&status=PENDING&sourceId=${apiId}`,
        method: 'GET',
      })
      .flush(fakePaginatedResult(data, { page: 1, pageCount: 1, pageItemCount: data.length, perPage: 20, totalCount: data.length }));
  }

  function expectApiScorePostRequest(apiId: string) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/scoring/_evaluate`,
      method: 'POST',
    });
    req.flush(fakeApiScoringTriggerResponse());
  }
});
