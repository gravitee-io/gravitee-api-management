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
import { SimpleChange } from '@angular/core';

import { ApiGeneralInfoQualityComponent } from './api-general-info-quality.component';

import { ApiGeneralInfoModule } from '../api-general-info.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiQualityMetrics } from '../../../../entities/api';
import { QualityRule } from '../../../../entities/qualityRule';

describe('ApiGeneralInfoQualityComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiGeneralInfoQualityComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiGeneralInfoModule, MatIconTestingModule],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiGeneralInfoQualityComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    fixture.componentInstance.apiId = API_ID;
    fixture.componentInstance.ngOnChanges({ apiId: {} as SimpleChange });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display quality', async () => {
    expectQualityRulesGetRequest([{ id: 'ruleId', name: 'Custom rule', description: 'Custom rule', weight: 42 }]);
    expectQualityMetricsGetRequest(API_ID, { score: 42, metrics_passed: { ruleId: true, 'api.quality.metrics.logo.weight': true } });
    fixture.detectChanges();

    const qualityMetrics: NodeList = fixture.nativeElement.querySelectorAll('.api-quality__content__list__item');

    expect(qualityMetrics).toHaveLength(2);
    expect(Array.from(qualityMetrics).map(item => item.textContent)).toEqual(['Put your own logo', 'Custom rule']);
  });

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
