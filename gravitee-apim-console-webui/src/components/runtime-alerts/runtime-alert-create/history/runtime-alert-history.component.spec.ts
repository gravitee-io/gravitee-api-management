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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { RuntimeAlertHistoryComponent } from './runtime-alert-history.component';
import { RuntimeAlertHistoryHarness } from './runtime-alert-history.harness';

import { AlertHistory } from '../../../../entities/alerts/history';
import { fakeAlertHistory } from '../../../../entities/alerts/history.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

describe('RuntimeAlertHistoryComponent', () => {
  const API_ID = 'apiId';
  const ALERT_ID = 'alertId123';
  const ENVIRONMENT_ID = 'envId';

  let componentHarness: RuntimeAlertHistoryHarness;
  let fixture: ComponentFixture<RuntimeAlertHistoryComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuntimeAlertHistoryComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID, envId: ENVIRONMENT_ID, alertId: ALERT_ID },
              data: { referenceType: 'API' },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RuntimeAlertHistoryComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, RuntimeAlertHistoryHarness);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should refresh history', async () => {
    expectGetAlertHistory();
    await componentHarness.refreshHistory();
    expectGetAlertHistory();
  });

  function expectGetAlertHistory(history: AlertHistory = fakeAlertHistory()) {
    const url = `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/alerts/${ALERT_ID}/events?&page=1&size=10`;
    const req = httpTestingController.expectOne({ method: 'GET', url });
    req.flush(history);
  }
});
