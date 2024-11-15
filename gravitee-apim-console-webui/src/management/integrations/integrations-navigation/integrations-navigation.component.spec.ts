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
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestElement } from '@angular/cdk/testing';
import { of } from 'rxjs';

import { IntegrationsNavigationComponent } from './integrations-navigation.component';
import { IntegrationsNavigationHarness } from './integrations-navigation.harness';

import { IntegrationsModule } from '../integrations.module';
import { GioTestingModule } from '../../../shared/testing';
import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { fakeIntegration } from '../../../entities/integrations/integration.fixture';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('IntegrationsNavigationComponent', () => {
  let fixture: ComponentFixture<IntegrationsNavigationComponent>;
  let componentHarness: IntegrationsNavigationHarness;
  let httpTestingController: HttpTestingController;

  const init = async (hasAnyMatching): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationsNavigationComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: IntegrationsService,
          useValue: {
            currentIntegration: () => of(fakeIntegration()),
            resetCurrentIntegration: (): void => {},
          },
        },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => hasAnyMatching,
          },
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

    fixture = TestBed.createComponent(IntegrationsNavigationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationsNavigationHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('menu items without permissions', () => {
    beforeEach(async () => {
      await init(false);
    });

    it('should not be in view', async (): Promise<void> => {
      fixture.componentInstance.items = [
        {
          routerLink: `/boo`,
          displayName: 'test1',
          permissions: [''],
          icon: 'info',
        },
        {
          routerLink: `/bla`,
          displayName: 'test2',
          permissions: [''],
          icon: 'info',
        },
      ];
      fixture.detectChanges();

      const menuItems: TestElement[] = await componentHarness.getMenuItems();
      expect(menuItems.length).toEqual(0);
    });
  });

  describe('menu items with permissions', () => {
    beforeEach(async () => {
      await init(true);
    });

    it('should be in view', async (): Promise<void> => {
      fixture.componentInstance.items = [
        {
          routerLink: `/boo`,
          displayName: 'test1',
          permissions: [''],
          icon: 'info',
        },
        {
          routerLink: `/bla`,
          displayName: 'test2',
          permissions: [''],
          icon: 'info',
        },
        {
          routerLink: `/boo2`,
          displayName: 'test12',
          permissions: [''],
          icon: 'info',
        },
        {
          routerLink: `/bla3`,
          displayName: 'test23',
          permissions: [''],
          icon: 'info',
        },
      ];
      fixture.detectChanges();
      fixture.componentInstance.ngOnInit();

      const menuItems: TestElement[] = await componentHarness.getMenuItems();
      expect(menuItems.length).toEqual(4);
    });
  });
});
