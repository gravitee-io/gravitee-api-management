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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UIRouterModule } from '@uirouter/angular';

import { OrgNavigationComponent } from './org-navigation.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';

describe('OrgNavigationComponent', () => {
  let fixture: ComponentFixture<OrgNavigationComponent>;
  let component: OrgNavigationComponent;
  const fakeUiRouter = {
    go: jest.fn(),
    includes: jest.fn(),
  };

  function createComponent(hasAnyMatching: boolean) {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioUiRouterTestingModule,
        GioHttpTestingModule,
        OrganizationSettingsModule,
        UIRouterModule.forRoot({ useHash: true }),
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: { includes: () => true } },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => hasAnyMatching,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrgNavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    jest.resetAllMocks();
  });

  describe('with all permissions', () => {
    beforeEach(() => {
      createComponent(true);
    });

    it('should build group items', () => {
      expect(component.groupItems.length).toEqual(6);
      expect(component.groupItems.map((item) => item.title)).toEqual([
        'Console',
        'User Management',
        'Gateway',
        'Notifications',
        'Audit',
        'Cockpit',
      ]);
    });
  });

  describe('without any permission', () => {
    beforeEach(() => {
      createComponent(false);
    });

    it('should build group items', () => {
      expect(component.groupItems.length).toEqual(1);
      expect(component.groupItems.map((item) => item.title)).toEqual(['Audit']);
    });
  });
});
