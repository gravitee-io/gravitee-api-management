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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiSubscriptionEditPushConfigComponent } from './api-subscription-edit-push-config.component';
import { ApiSubscriptionEditPushConfigHarness } from './api-subscription-edit-push-config.harness';

import { GioTestingModule } from '../../../../../shared/testing';

describe('ApiSubscriptionEditPushConfigComponent', () => {
  let component: ApiSubscriptionEditPushConfigComponent;
  let fixture: ComponentFixture<ApiSubscriptionEditPushConfigComponent>;
  let componentHarness: ApiSubscriptionEditPushConfigHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiSubscriptionEditPushConfigComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiSubscriptionEditPushConfigComponent);
    component = fixture.componentInstance;
  });

  it('should display infos', async () => {
    component.consumerConfiguration = {
      entrypointId: 'entrypointId',
      channel: 'myChannel',
      entrypointConfiguration: {
        callbackUrl: 'myCallbackUrl',
        auth: {
          type: 'authType',
        },
      },
    };
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiSubscriptionEditPushConfigHarness);
    fixture.detectChanges();

    expect(await componentHarness.getContentText()).toContain('myChannel');
    expect(await componentHarness.getContentText()).toContain('myCallbackUrl');
    expect(await componentHarness.getContentText()).toContain('authType');
  });
});
