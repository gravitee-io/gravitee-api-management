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

import { SubscriptionEditPushConfigComponent } from './subscription-edit-push-config.component';
import { SubscriptionEditPushConfigHarness } from './subscription-edit-push-config.harness';

import { GioTestingModule } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

describe('SubscriptionEditPushConfigComponent', () => {
  let component: SubscriptionEditPushConfigComponent;
  let fixture: ComponentFixture<SubscriptionEditPushConfigComponent>;
  let componentHarness: SubscriptionEditPushConfigHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionEditPushConfigComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionEditPushConfigComponent);
    component = fixture.componentInstance;
  });

  it('should display infos in edit mode', async () => {
    const gioPermissionService = TestBed.inject(GioPermissionService);
    gioPermissionService._setPermissions(['api-subscription-u']);
    component.updatePermission = 'api-subscription-u';

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
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionEditPushConfigHarness);
    fixture.detectChanges();

    expect(component.canEdit).toBeTruthy();
    const openConfigurationButton = await componentHarness.getOpenConfigurationButton();
    expect(await openConfigurationButton.getText()).toContain('Edit');

    expect(await componentHarness.getContentText()).toContain('myChannel');
    expect(await componentHarness.getContentText()).toContain('myCallbackUrl');
    expect(await componentHarness.getContentText()).toContain('authType');
  });

  it('should be readonly with wrong permissions', async () => {
    const gioPermissionService = TestBed.inject(GioPermissionService);
    gioPermissionService._setPermissions(['api-subscription-r']);
    component.updatePermission = 'api-subscription-u';

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
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionEditPushConfigHarness);
    fixture.detectChanges();

    expect(component.canEdit).toBeFalsy();
    const openConfigurationButton = await componentHarness.getOpenConfigurationButton();
    expect(await openConfigurationButton.getText()).toContain('View');
  });

  it('should be readonly with right permissions but readonly is forced', async () => {
    const gioPermissionService = TestBed.inject(GioPermissionService);
    gioPermissionService._setPermissions(['api-subscription-u']);
    component.updatePermission = 'api-subscription-u';

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
    component.readonly = true;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SubscriptionEditPushConfigHarness);
    fixture.detectChanges();

    expect(component.canEdit).toBeFalsy();
    const openConfigurationButton = await componentHarness.getOpenConfigurationButton();
    expect(await openConfigurationButton.getText()).toContain('View');
  });
});
