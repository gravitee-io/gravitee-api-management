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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';

import { IntegrationConfigurationComponent } from './integration-configuration.component';
import { IntegrationConfigurationHarness } from './integration-configuration.harness';

import { GioTestingModule } from '../../../shared/testing';
import { IntegrationsModule } from '../integrations.module';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { IntegrationNavigationItem } from '../integrations.model';

describe('IntegrationConfigurationComponent', (): void => {
  let fixture: ComponentFixture<IntegrationConfigurationComponent>;
  let componentHarness: IntegrationConfigurationHarness;
  let httpTestingController: HttpTestingController;
  const integrationId: string = '123TestID';
  let routerNavigateSpy: jest.SpyInstance;

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const init = async (
    tabs: IntegrationNavigationItem[],
    permissions: GioTestingPermission = ['integration-member-c', 'integration-member-r', 'integration-member-u', 'integration-member-d'],
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationConfigurationComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { integrationId: integrationId } } },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
    fixture = TestBed.createComponent(IntegrationConfigurationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationConfigurationHarness);
    fixture.componentInstance.configurationTabs = tabs;
    fixture.componentInstance.ngOnInit();
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('tabs', () => {
    it('should have correct number of tabs', async () => {
      await init([
        {
          displayName: 'OneTab',
          routerLink: 'One',
          permissions: ['integration-member-r'],
        },
        {
          displayName: 'TwoTab',
          routerLink: 'Two',
          permissions: ['integration-member-r'],
        },
        {
          displayName: 'ThreeTab',
          routerLink: 'Three',
          permissions: ['integration-member-r'],
        },
        {
          displayName: 'FourTab',
          routerLink: 'Four',
          permissions: ['integration-definition-r'],
        },
      ]);
      const tabNavBar = await componentHarness.getTabNavBar();
      expect(tabNavBar.length).toBe(3);
    });

    it('should redirect to first allowed path', async () => {
      await init(
        [
          {
            displayName: 'OneTab',
            routerLink: 'one',
            permissions: ['integration-definition-u', 'integration-definition-d'],
          },
          {
            displayName: 'TwoTab',
            routerLink: 'two',
            permissions: ['integration-member-r'],
          },
        ],
        ['integration-member-r'],
      );
      const tabNavBar = await componentHarness.getTabNavBar();

      fixture.componentInstance.ngOnInit();

      expect(routerNavigateSpy).toHaveBeenCalledWith(['two'], { relativeTo: expect.anything() });
      expect(tabNavBar.length).toBe(1);
    });
  });
});
