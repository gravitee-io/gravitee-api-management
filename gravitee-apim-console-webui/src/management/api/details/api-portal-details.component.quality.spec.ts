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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiPortalDetailsModule } from './api-portal-details.module';
import { ApiPortalDetailsComponent } from './api-portal-details.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { Api, ApiQualityMetrics } from '../../../entities/api';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { UIRouterStateParams, CurrentUserService } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';
import { Category } from '../../../entities/category/Category';
import { QualityRule } from '../../../entities/qualityRule';

describe('ApiPortalDetailsComponent - quality', () => {
  const API_ID = 'apiId';
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  let fixture: ComponentFixture<ApiPortalDetailsComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalDetailsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: 'Constants',
          useValue: {
            ...CONSTANTS_TESTING,
            env: {
              ...CONSTANTS_TESTING.env,
              settings: {
                ...CONSTANTS_TESTING.env.settings,
                apiReview: {
                  enabled: true,
                },
                apiQualityMetrics: {
                  ...CONSTANTS_TESTING.env.settings.apiQualityMetrics,
                  enabled: true,
                },
              },
            },
            org: {
              ...CONSTANTS_TESTING.org,
              settings: {
                ...CONSTANTS_TESTING.org.settings,
                jupiterMode: {
                  enabled: true,
                },
              },
            },
          },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiPortalDetailsComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    trackImageOnload();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display quality', async () => {
    const api = fakeApi({
      id: API_ID,
      visibility: 'PRIVATE',
    });

    expectApiGetRequest(api);
    expectCategoriesGetRequest();
    expectQualityRulesGetRequest([{ id: 'ruleId', name: 'Custom rule', description: 'Custom rule', weight: 42 }]);
    expectQualityMetricsGetRequest(API_ID, { score: 42, metrics_passed: { ruleId: true, 'api.quality.metrics.logo.weight': true } });
    await waitImageCheck();
    fixture.detectChanges();

    const qualityMetrics: NodeList = fixture.nativeElement.querySelectorAll('.api-quality__content__list__item');

    expect(qualityMetrics).toHaveLength(2);
    expect(Array.from(qualityMetrics).map((item) => item.textContent)).toEqual(['Put your own logo', 'Custom rule']);
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectCategoriesGetRequest(categories: Category[] = []) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/configuration/categories`, method: 'GET' }).flush(categories);
    fixture.detectChanges();
  }

  function expectQualityRulesGetRequest(qualityRules: QualityRule[] = []) {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules` })
      .flush(qualityRules);
    fixture.detectChanges();
  }

  function expectQualityMetricsGetRequest(apiId: string, qualityMetrics: ApiQualityMetrics) {
    const req = httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality` });

    req.flush(qualityMetrics);
    fixture.detectChanges();
  }
});

/** Override Image global to force onload call */
function trackImageOnload() {
  Object.defineProperty(Image.prototype, 'onload', {
    get: function () {
      return this._onload;
    },
    set: function (fn) {
      this._onload = fn;
      this._onload();
    },
  });
}

export function newImageFile(fileName: string, type: string): File {
  return new File([''], fileName, { type });
}

const waitImageCheck = () => new Promise((resolve) => setTimeout(resolve, 1));
