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
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiRuntimeAlertsComponent } from './api-runtime-alerts.component';
import { ApiRuntimeAlertsModule } from './api-runtime-alerts.module';

import { RuntimeAlertListHarness } from '../../../components/runtime-alerts/runtime-alert-list/runtime-alert-list.harness';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity.fixtures';
import { RuntimeAlertListEmptyStateHarness } from '../../../components/runtime-alerts/runtime-alert-list-empty-state/runtime-alert-list-empty-state.harness';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiRuntimeAlertsComponent', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envId';
  const ALERT = fakeAlertTriggerEntity();
  const ACTIVATED_ROUTE = {
    snapshot: { params: { apiId: API_ID, envId: ENVIRONMENT_ID } },
  };
  let fixture: ComponentFixture<ApiRuntimeAlertsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let router: Router;

  async function createComponent(hasAnyMatching = true) {
    await TestBed.configureTestingModule({
      declarations: [ApiRuntimeAlertsComponent],
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ApiRuntimeAlertsModule],
      providers: [
        { provide: ActivatedRoute, useValue: ACTIVATED_ROUTE },
        { provide: GioPermissionService, useValue: { hasAnyMatching: () => hasAnyMatching } },
      ],
    })
      .overrideProvider(InteractivityChecker, { useValue: { isFocusable: () => true, isTabbable: () => true } })
      .compileComponents();

    fixture = TestBed.createComponent(ApiRuntimeAlertsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get alerts and display the table list', async () => {
    await createComponent();
    expectAlertsGetRequest();
    const listComponentHarness = await loader.getHarness(RuntimeAlertListHarness);
    expect(listComponentHarness).toBeTruthy();

    const { rowCells } = await listComponentHarness.computeTableCells();
    expect(rowCells).toHaveLength(1);
  });

  it('should get alerts and display empty state page', async () => {
    await createComponent();
    expectAlertsGetRequest([]);
    expect(await loader.getHarness(RuntimeAlertListEmptyStateHarness)).toBeTruthy();
  });

  it('should navigate to alert creation page', async () => {
    await createComponent();
    expectAlertsGetRequest();
    const routerSpy = jest.spyOn(router, 'navigate');

    const createAlertButton = loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add alert"]' }));
    await createAlertButton.then(btn => btn.click());

    expect(routerSpy).toHaveBeenCalledWith(['./new'], { relativeTo: ACTIVATED_ROUTE });
  });

  it('should not not have permission to navigate to alert creation page', async () => {
    await createComponent(false);
    expectAlertsGetRequest();

    const createAlertButton = loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add alert"]' }));

    expect(await createAlertButton.then(btn => btn.isDisabled())).toBeTruthy();
  });

  it('should delete an alert', async () => {
    await createComponent();
    expectAlertsGetRequest();

    const listComponentHarness = await loader.getHarness(RuntimeAlertListHarness);
    await listComponentHarness.deleteAlert(0);

    const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    await dialog.confirm();

    expectAlertDeleteRequest(ALERT.id);
    expectAlertsGetRequest([]);
  });

  function expectAlertsGetRequest(response = [ALERT]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/alerts?event_counts=true`).flush(response);
    fixture.detectChanges();
  }

  function expectAlertDeleteRequest(alertId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/alerts/${alertId}`,
        method: 'DELETE',
      })
      .flush(null);
    fixture.detectChanges();
  }
});
