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
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ApiRuntimeAlertsComponent } from './api-runtime-alerts.component';
import { ApiRuntimeAlertsModule } from './api-runtime-alerts.module';

import { RuntimeAlertListHarness } from '../../../components/runtime-alerts/runtime-alert-list/runtime-alert-list.harness';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity.fixtures';
import { RuntimeAlertListEmptyStateHarness } from '../../../components/runtime-alerts/runtime-alert-list-empty-state/runtime-alert-list-empty-state.harness';

describe('ApiRuntimeAlertsComponent', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envId';
  const ALERT = fakeAlertTriggerEntity();
  let fixture: ComponentFixture<ApiRuntimeAlertsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiRuntimeAlertsComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiRuntimeAlertsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiId: API_ID, envId: ENVIRONMENT_ID } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiRuntimeAlertsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get alerts and display the table list', async () => {
    expectAlertsGetRequest();
    const listComponentHarness = await loader.getHarness(RuntimeAlertListHarness);
    expect(listComponentHarness).toBeTruthy();

    const { rowCells } = await listComponentHarness.computeTableCells();
    expect(rowCells).toHaveLength(1);
  });

  it('should get alerts and display empty state page', async () => {
    expectAlertsGetRequest([]);
    expect(await loader.getHarness(RuntimeAlertListEmptyStateHarness)).toBeTruthy();
  });

  function expectAlertsGetRequest(response = [ALERT]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/alerts?event_counts=true`).flush(response);
    fixture.detectChanges();
  }
});
