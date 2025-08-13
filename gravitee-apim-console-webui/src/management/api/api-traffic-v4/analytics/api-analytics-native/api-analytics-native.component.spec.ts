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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ApiAnalyticsNativeHarness } from './api-analytics-native.component.harness';
import { ApiAnalyticsNativeComponent } from './api-analytics-native.component';

import { GioTestingModule } from '../../../../../shared/testing';

describe('ApiAnalyticsNativeComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiAnalyticsNativeComponent>;
  let componentHarness: ApiAnalyticsNativeHarness;
  let httpTestingController: HttpTestingController;

  const initComponent = async (queryParams = {}) => {
    TestBed.configureTestingModule({
      imports: [ApiAnalyticsNativeComponent, OwlNativeDateTimeModule, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: queryParams,
            },
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiAnalyticsNativeComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiAnalyticsNativeHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display loading', async () => {
    await initComponent();
    expect(await componentHarness.isLoaderDisplayed()).toBeTruthy();
  });
});
