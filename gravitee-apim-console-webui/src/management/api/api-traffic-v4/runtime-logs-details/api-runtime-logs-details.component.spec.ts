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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiRuntimeLogsDetailsComponent } from './api-runtime-logs-details.component';
import { ApiRuntimeLogsDetailsModule } from './api-runtime-logs-details.module';
import { ApiRuntimeLogsMessagesHarness } from './components/runtime-logs-messages/api-runtime-logs-messages.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';

describe('ApiRuntimeLogsDetailsComponent', () => {
  const API_ID = 'an-api-id';

  let fixture: ComponentFixture<ApiRuntimeLogsDetailsComponent>;
  let httpTestingController: HttpTestingController;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiRuntimeLogsDetailsModule, GioTestingModule, MatSnackBarModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display message logs details component', async () => {
    await initComponent();
    expectApi(fakeApiV4({ id: API_ID, type: 'MESSAGE' }));

    const loader = TestbedHarnessEnvironment.loader(fixture);
    expect(await loader.getHarness(ApiRuntimeLogsMessagesHarness)).toBeTruthy();
  });

  function expectApi(api: ApiV4) {
    const reqs = httpTestingController.match({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
    expect(reqs.length).toBe(2);
    reqs.forEach(r => r.flush(api));
    fixture.detectChanges();
  }
});
