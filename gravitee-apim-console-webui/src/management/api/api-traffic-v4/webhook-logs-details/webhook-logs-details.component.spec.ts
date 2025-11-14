/// <reference types="jest" />
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
import { Router, ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { WebhookLogsDetailsComponent } from './webhook-logs-details.component';
import { WebhookLogsDetailsHarness } from './webhook-logs-details.harness';
import { WebhookLogsDetailsModule } from './webhook-logs-details.module';

import { GioTestingModule } from '../../../../shared/testing';

declare const describe: (...args: any[]) => void;
declare const beforeEach: (...args: any[]) => void;
declare const it: (...args: any[]) => void;
declare const expect: (...args: any[]) => any;
declare const jest: {
  fn: (...args: any[]) => any;
  spyOn: (...args: any[]) => any;
};

describe('WebhookLogsDetailsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsDetailsComponent>;
  let harness: WebhookLogsDetailsHarness;
  let routerNavigateSpy: ReturnType<typeof jest.spyOn>;
  let routerNavigateByUrlSpy: ReturnType<typeof jest.spyOn>;

  const activatedRouteStub = {
    snapshot: {
      params: { requestId: 'request-1' },
    },
  } as unknown as ActivatedRoute;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogsDetailsModule, RouterTestingModule, GioTestingModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: activatedRouteStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogsDetailsComponent);
    const component = fixture.componentInstance;
    component.selectedLog = null;
    component.overviewRequest = [];
    component.overviewResponse = [];
    component.deliveryAttempts = [];
    component.requestHeaders = [];
    component.responseHeaders = [];
    component.requestBody = '';
    component.responseBody = '';

    fixture.detectChanges();

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    routerNavigateByUrlSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsDetailsHarness);
  });

  it('should navigate back when the back button is clicked', async () => {
    await harness.clickBack();
    if (routerNavigateSpy.mock.calls.length) {
      expect(routerNavigateSpy).toHaveBeenCalledWith(['..'], { relativeTo: activatedRouteStub });
    } else {
      expect(routerNavigateByUrlSpy).toHaveBeenCalled();
    }
  });

  it('should open reporting settings when empty state action is clicked', async () => {
    expect(await harness.hasEmptyState()).toBe(true);

    await harness.clickOpenSettings();

    expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], {
      relativeTo: activatedRouteStub,
      queryParams: { openSettings: 'true' },
    });
  });
});
