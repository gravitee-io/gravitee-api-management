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
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiAnalyticsComponent } from './api-analytics.component';
import { ApiAnalyticsProxyHarness } from './api-analytics-proxy/api-analytics-proxy.component.harness';

import { CONSTANTS_TESTING, GioTestingModule, provideHighchartsTesting } from '../../../../shared/testing';
import { ApiV4, fakeProxyApiV4 } from '../../../../entities/management-api-v2';

describe('ApiAnalyticsComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<ApiAnalyticsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAnalyticsComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        provideHighchartsTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiId: API_ID }, queryParams: {} },
            queryParams: of({}),
            params: of({ apiId: API_ID }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAnalyticsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should display the proxy analytics for an AUTHZ API', async () => {
    expectApiGetRequest(fakeProxyApiV4({ id: API_ID, type: 'AUTHZ' }));

    expect(await loader.getHarnessOrNull(ApiAnalyticsProxyHarness)).not.toBeNull();
  });

  it('should display an empty state for an API type without analytics', async () => {
    expectApiGetRequest(fakeProxyApiV4({ id: API_ID, type: 'A2A_PROXY' }));

    expect(fixture.nativeElement.querySelector('gio-card-empty-state')?.textContent).toContain('API Traffic not available');
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
