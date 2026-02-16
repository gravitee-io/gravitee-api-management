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
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';

import { OrgNavigationComponent } from './org-navigation.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { GioTestingModule } from '../../../shared/testing';
import { GioPermissionService, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('OrgNavigationComponent', () => {
  let fixture: ComponentFixture<OrgNavigationComponent>;
  let component: OrgNavigationComponent;

  function createComponent(hasAnyMatching: boolean) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, GioLicenseTestingModule],
      providers: [
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

  function createComponentWithPermissions(permissions: string[]) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, GioLicenseTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
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
      expect(component.groupItems.map(item => item.title)).toEqual([
        'Console',
        'User Management',
        'Gateway',
        'Notifications',
        'Audit',
        'Gravitee Cloud',
      ]);
    });
  });

  describe('without any permission', () => {
    beforeEach(() => {
      createComponentWithPermissions(['organization-settings-r']);
    });

    it('should build group items', () => {
      expect(component.groupItems.length).toEqual(2);
      expect(component.groupItems.map(item => item.title)).toEqual(['Console', 'Audit']);
    });
  });
});
