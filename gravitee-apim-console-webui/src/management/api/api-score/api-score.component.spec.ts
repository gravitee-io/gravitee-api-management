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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiScoreComponent } from './api-score.component';
import { ApiScoreHarness } from './api-score.harness';
import { ApiScoreModule } from './api-score.module';
import { fakeApiScoringTriggerResponse } from './api-score.fixture';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiFederated } from '../../../entities/management-api-v2';

describe('ApiScoreComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiScoreComponent>;
  let componentHarness: ApiScoreHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiScoreComponent],
      imports: [ApiScoreModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
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

    fixture = TestBed.createComponent(ApiScoreComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreHarness);

    expectApiGetRequest(API_ID);
    fixture.detectChanges();
  };

  describe('ApiScoreComponent', () => {
    describe('evaluate', () => {
      beforeEach(() => init());

      it('should trigger API scoring on click', async () => {
        await componentHarness.clickEvaluate();

        expectApiScorePostRequest('api-id');
      });
    });
  });

  function expectApiGetRequest(apiId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}`,
        method: 'GET',
      })
      .flush(fakeApiFederated({ id: apiId }));
  }

  function expectApiScorePostRequest(apiId: string) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_score`,
      method: 'POST',
    });
    req.flush(fakeApiScoringTriggerResponse());
  }
});
