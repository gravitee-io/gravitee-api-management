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
import { ApiScoring, ScoringAssetType, ScoringSeverity } from './api-scoring.model';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiFederated } from '../../../entities/management-api-v2';
import { AsyncJob, fakeAsyncJob } from '../../../entities/async-job';
import { fakePaginatedResult } from '../../../entities/paginatedResult';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiScoringComponent', () => {
  const fakeSnackBarService = {
    error: jest.fn(),
  };

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
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
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

  afterEach(() => {
    httpTestingController.verify();
  });

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

    it('should show snackbar with evaluation errors if any exist', fakeAsync(async () => {
      const apiScoringWithEvaluationErrors = fakeApiScoring({
        assets: [
          {
            name: 'Asset name',
            diagnostics: [],
            type: ScoringAssetType.GRAVITEE_DEFINITION,
            errors: [
              {
                code: 'undefined-function',
                path: ['rules', 'api-key-security-scheme', 'then', 'function'],
              },
            ],
          },
        ],
      });
      const expectedErrorMessage = `Errors occurred while scoring this API:

Asset: Asset name
Code: undefined-function
Path: rules,api-key-security-scheme,then,function`;

      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID, apiScoringWithEvaluationErrors);
      expect(fakeSnackBarService.error).toHaveBeenCalledWith(expectedErrorMessage);
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

  describe('filters', () => {
    it('should show filter options', fakeAsync(async () => {
      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(API_ID);

      const filterButtons = await componentHarness.filterButtons();
      const toggles = await filterButtons.getToggles();

      expect(toggles.length).toBe(5);
      expect(await toggles[0].isDisabled()).toBe(false);
      expect(await toggles[0].getText()).toBe('All (1)');
      expect(await toggles[1].isDisabled()).toBe(true);
      expect(await toggles[2].isDisabled()).toBe(false);
      expect(await toggles[2].getText()).toBe('Warnings (1)');
      expect(await toggles[3].isDisabled()).toBe(true);
      expect(await toggles[4].isDisabled()).toBe(true);
    }));

    it('should filter apis', fakeAsync(async () => {
      await init();
      tick(1);
      expectAsyncJobGetRequest(API_ID, []);
      expectApiGetRequest(API_ID);
      expectApiScoreGetRequest(
        API_ID,
        fakeApiScoring({
          summary: {
            all: 3,
            errors: 2,
            warnings: 1,
            infos: 0,
            hints: 0,
            score: 0.9,
          },
          assets: [
            {
              name: 'echo-oas.json',
              type: ScoringAssetType.SWAGGER,
              diagnostics: [
                {
                  range: {
                    start: { line: 17, character: 12 },
                    end: { line: 38, character: 25 },
                  },
                  severity: ScoringSeverity.ERROR,
                  message: 'Some Error 1',
                  path: 'paths./echo.get',
                },
                {
                  range: {
                    start: { line: 17, character: 12 },
                    end: { line: 38, character: 25 },
                  },
                  severity: ScoringSeverity.ERROR,
                  message: 'Some Error 2',
                  path: 'paths./echo.get',
                },
                {
                  range: {
                    start: { line: 17, character: 12 },
                    end: { line: 38, character: 25 },
                  },
                  severity: ScoringSeverity.WARN,
                  message: 'Some Warning 1',
                  path: 'paths./echo.get',
                },
              ],
            },
          ],
        }),
      );

      const filterButtons = await componentHarness.filterButtons();
      const toggles = await filterButtons.getToggles();

      // check initial length of table
      const table = await componentHarness.getTables();
      expect(await table.getRows()).toHaveLength(3);

      // click on filters and check the table length
      expect(await toggles[1].getText()).toBe('Errors (2)');
      await toggles[1].toggle();
      expect(await table.getRows()).toHaveLength(2);

      expect(await toggles[2].getText()).toBe('Warnings (1)');
      await toggles[2].toggle();
      expect(await table.getRows()).toHaveLength(1);

      expect(await toggles[0].getText()).toBe('All (3)');
      await toggles[0].toggle();
      expect(await table.getRows()).toHaveLength(3);
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

  function expectApiScoreGetRequest(apiId: string, payload: ApiScoring = fakeApiScoring()) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/scoring`,
        method: 'GET',
      })
      .flush(payload);
  }

  function expectAsyncJobGetRequest(apiId: string, data: AsyncJob[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/async-jobs?page=1&perPage=10&type=SCORING_REQUEST&sourceId=${apiId}`,
        method: 'GET',
      })
      .flush(
        fakePaginatedResult(data, {
          page: 1,
          pageCount: 1,
          pageItemCount: data.length,
          perPage: 20,
          totalCount: data.length,
        }),
      );
  }

  function expectApiScorePostRequest(apiId: string) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/scoring/_evaluate`,
      method: 'POST',
    });
    req.flush(fakeApiScoringTriggerResponse());
  }
});
