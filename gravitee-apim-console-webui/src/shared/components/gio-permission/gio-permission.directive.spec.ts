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
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Component, Input } from '@angular/core';

import { GioPermissionModule } from './gio-permission.module';
import { GioPermissionCheckOptions } from './gio-permission.directive';
import { GioTestingPermissionProvider } from './gio-permission.service';

import { GioTestingModule } from '../../testing';

@Component({ template: `<div *gioPermission="permissions">A Content</div>`, standalone: false })
class TestPermissionComponent {
  @Input()
  permissions: GioPermissionCheckOptions;
}

describe('GioPermissionDirective', () => {
  let fixture: ComponentFixture<TestPermissionComponent>;

  function prepareTestPermissionComponent(permission: GioPermissionCheckOptions) {
    fixture = TestBed.configureTestingModule({
      declarations: [TestPermissionComponent],
      imports: [GioTestingModule, GioPermissionModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-rating-r', 'api-rating-c'],
        },
      ],
    }).createComponent(TestPermissionComponent);

    fixture.componentInstance.permissions = permission;
    fixture.detectChanges();
  }

  describe('anyOf', () => {
    it('should hide element if permission is not matching', () => {
      prepareTestPermissionComponent({ anyOf: ['api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should display element if permission is matching', () => {
      prepareTestPermissionComponent({ anyOf: ['api-rating-r'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should display element if at least one permission is matching', () => {
      prepareTestPermissionComponent({ anyOf: ['api-rating-r', 'api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });
  });

  describe('noneOf', () => {
    it('should hide element if no permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should display element if a permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-r'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should display element if at least one permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-r', 'api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });
  });
});
