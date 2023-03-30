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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { HomeOverviewComponent } from './home-overview.component';

import { CurrentUserService } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { HomeModule } from '../home.module';
import { GioQuickTimeRangeHarness } from '../widgets/gio-quick-time-range/gio-quick-time-range.harness';
import { GioRequestStatsHarness } from '../widgets/gio-request-stats/gio-request-stats.harness';

describe('HomeOverviewComponent', () => {
  let fixture: ComponentFixture<HomeOverviewComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, HomeModule, MatIconTestingModule],
      providers: [{ provide: CurrentUserService, useValue: { currentUser } }],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeOverviewComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show request stats', async () => {
    expect(loader).toBeTruthy();
    expectLoadRequestStats();

    const stats = await loader.getHarness(GioRequestStatsHarness);
    expect(await stats.getAverage()).toEqual('8.43 ms');
  });

  it('should load request stats when changing date range', async () => {
    expect(loader).toBeTruthy();
    expectLoadRequestStats();

    const timeRangeHarness = await loader.getHarness(GioQuickTimeRangeHarness);
    await timeRangeHarness.selectTimeRangeByText('last hour');
    const req = expectLoadRequestStats();
    expect(req.request.url).toContain('interval=120000');
  });

  function expectLoadRequestStats(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=stats&field=response-time`);
    });
    req.flush({
      min: 0.02336,
      max: 23009.29032,
      avg: 8.4323,
      rps: 1.2012334,
      rpm: 72.074004,
      rph: 4324.44024,
      count: 332981092,
      sum: 4567115654.2,
    });
    return req;
  }
});
