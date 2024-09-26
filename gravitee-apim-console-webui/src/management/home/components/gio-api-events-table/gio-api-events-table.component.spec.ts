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
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { SimpleChange } from '@angular/core';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { UIRouterModule } from '@uirouter/angular';

import { GioApiEventsTableModule } from './gio-api-events-table.module';
import { GioApiEventsTableComponent } from './gio-api-events-table.component';
import { SEARCH_RESPONSE } from './gio-api-events-table.stories';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';

describe('GioApiEventsTableComponent', () => {
  let fixture: ComponentFixture<GioApiEventsTableComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        GioUiRouterTestingModule,
        GioApiEventsTableModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GioApiEventsTableComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.componentInstance.timeRangeParams = {
      id: '1M',
      from: 1691565599784,
      to: 1694157599784,
      interval: 86400000,
    };
    fixture.componentInstance.ngOnChanges({ timeRangeParams: new SimpleChange(null, fixture.componentInstance.timeRangeParams, true) });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init', async () => {
    const tableHarness = await loader.getHarness(MatTableHarness);
    // Expect Loading state
    expect((await tableHarness.getRows()).length).toEqual(1);

    // Expect table with data
    expectSearchApiEventsRequest();
    expect((await tableHarness.getRows()).length).toEqual(10);

    expect(await tableHarness.getCellTextByIndex()).toEqual([
      ['LPI Kafka (1)', '# 3', 'Aug 30, 2023, 2:14:03 PM', 'Deploy'],
      ['LPI Kafka (1)', '# 2', 'Aug 30, 2023, 1:58:18 PM', 'Deploy'],
      ['LPI Kafka (1)', '# 1', 'Aug 30, 2023, 1:53:06 PM', 'Deploy'],
      ['LPI test v2 (1)', '', 'Aug 30, 2023, 12:24:29 PM', 'Start'],
      ['LPI test v2 (1)', '# 1', 'Aug 30, 2023, 12:24:28 PM', 'Deploy'],
      ['SSE &#43; Kafka (1)', '# 1', 'Aug 30, 2023, 12:22:02 PM', 'Deploy'],
      ['LPI test (1)', '', 'Aug 29, 2023, 10:14:00 AM', 'Stop'],
      ['LPI test (1)', '', 'Aug 29, 2023, 10:13:55 AM', 'Start'],
      ['LPI test (1)', '', 'Aug 29, 2023, 9:42:14 AM', 'Stop'],
      ['LPI test (1)', '', 'Aug 29, 2023, 9:42:04 AM', 'Start'],
    ]);
  });

  function expectSearchApiEventsRequest() {
    const req = httpTestingController.expectOne({
      method: 'GET',

      url: `${CONSTANTS_TESTING.env.baseURL}/platform/events?type=START_API,STOP_API,PUBLISH_API,UNPUBLISH_API&query=&api_ids=&from=1691565599784&to=1694157599784&page=0&size=5`,
    });
    req.flush(SEARCH_RESPONSE);
  }
});
