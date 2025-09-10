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
import { of } from 'rxjs';

import { GioPermissionModule } from './gio-permission.module';
import { GioPermissionCheckOptions, GioPermissionRoleContext } from './gio-permission.directive';
import { GioTestingPermissionProvider, GioPermissionService } from './gio-permission.service';

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
    it('should display element if no permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should hide element if a permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-r'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should hide element if at least one permission is matching', () => {
      prepareTestPermissionComponent({ noneOf: ['api-rating-r', 'api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });
  });

  describe('allOf', () => {
    it('should display element if all permissions are matching', () => {
      prepareTestPermissionComponent({ allOf: ['api-rating-r', 'api-rating-c'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should hide element if only one permission is missing', () => {
      prepareTestPermissionComponent({ allOf: ['api-rating-r', 'api-rating-u'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should hide element if all permissions are missing', () => {
      prepareTestPermissionComponent({ allOf: ['api-rating-u', 'api-definition-r'] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });
    it('should throw error if allOf and anyOf are both set', () => {
      expect(() => {
        prepareTestPermissionComponent({ allOf: ['api-rating-r'], anyOf: ['api-rating-c'] });
      }).toThrowError('You should only set one of `anyOf`, `noneOf`, or `allOf`, but not more than one.');
    });
  });

  describe('else', () => {
    @Component({
      template: `
        <ng-template #elseTpl><span>Else Content</span></ng-template>
        <div *gioPermission="permissions; else elseTpl">Main Content</div>
      `,
      standalone: false,
    })
    class TestElsePermissionComponent {
      @Input()
      permissions: GioPermissionCheckOptions;
    }
    let elseFixture: ComponentFixture<TestElsePermissionComponent>;

    function prepareTestElsePermissionComponent(permission: GioPermissionCheckOptions) {
      elseFixture = TestBed.configureTestingModule({
        declarations: [TestElsePermissionComponent],
        imports: [GioTestingModule, GioPermissionModule],
        providers: [
          {
            provide: GioTestingPermissionProvider,
            useValue: ['api-rating-r', 'api-rating-c'],
          },
        ],
      }).createComponent(TestElsePermissionComponent);

      elseFixture.componentInstance.permissions = permission;
      elseFixture.detectChanges();
    }

    it('should render else template when permissions do not match', () => {
      prepareTestElsePermissionComponent({ anyOf: ['api-definition-u'] });

      const mainEl = elseFixture.nativeElement.querySelector('div');
      const elseEl = elseFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeNull();
      expect(elseEl).toBeDefined();
      expect(elseEl.textContent).toContain('Else Content');
    });

    it('should render main template and not render else when permissions match', () => {
      prepareTestElsePermissionComponent({ anyOf: ['api-rating-r'] });

      const mainEl = elseFixture.nativeElement.querySelector('div');
      const elseEl = elseFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeDefined();
      expect(mainEl.textContent).toContain('Main Content');
      expect(elseEl).toBeNull();
    });
  });

  describe('gioPermissionRoleContext', () => {
    @Component({
      template: `
        <div *gioPermission="permissions; else elseTpl; roleContext: roleContext">Role Context Main</div>
        <ng-template #elseTpl><span>Else Content</span></ng-template>
      `,
      standalone: false,
    })
    class TestRoleContextPermissionComponent {
      @Input()
      permissions: GioPermissionCheckOptions;

      @Input()
      roleContext = null;
    }

    let roleCtxFixture: ComponentFixture<TestRoleContextPermissionComponent>;

    function prepareRoleContextComponent(permission: GioPermissionCheckOptions, roleContext: GioPermissionRoleContext) {
      roleCtxFixture = TestBed.configureTestingModule({
        declarations: [TestRoleContextPermissionComponent],
        imports: [GioTestingModule, GioPermissionModule],
        providers: [
          {
            provide: GioTestingPermissionProvider,
            useValue: [],
          },
        ],
      }).createComponent(TestRoleContextPermissionComponent);

      // Spy on service to provide role-context permissions
      const permissionService = TestBed.inject(GioPermissionService);
      jest.spyOn(permissionService, 'getPermissionsByRoleScope').mockImplementation((roleScope, roleId) => {
        if (roleScope === 'CLUSTER' && roleId === 'my-cluster-id') {
          return of(['cluster-permissions-r', 'cluster-permissions-c']);
        }
        return of([]);
      });

      roleCtxFixture.componentInstance.permissions = permission;
      roleCtxFixture.componentInstance.roleContext = roleContext;
      roleCtxFixture.detectChanges();
    }

    it('should display element when anyOf matches role-context permissions', () => {
      prepareRoleContextComponent({ anyOf: ['cluster-permissions-r'] }, { role: 'CLUSTER', id: 'my-cluster-id' });

      const mainEl = roleCtxFixture.nativeElement.querySelector('div');
      const elseEl = roleCtxFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeDefined();
      expect(mainEl.textContent).toContain('Role Context Main');
      expect(elseEl).toBeNull();
    });

    it('should hide element when noneOf matches role-context permissions', () => {
      prepareRoleContextComponent({ noneOf: ['cluster-permissions-r'] }, { role: 'CLUSTER', id: 'my-cluster-id' });

      const mainEl = roleCtxFixture.nativeElement.querySelector('div');
      const elseEl = roleCtxFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeNull();
      expect(elseEl).toBeDefined();
    });

    it('should display element when allOf are satisfied by role-context permissions', () => {
      prepareRoleContextComponent({ allOf: ['cluster-permissions-r', 'cluster-permissions-c'] }, { role: 'CLUSTER', id: 'my-cluster-id' });

      const mainEl = roleCtxFixture.nativeElement.querySelector('div');
      const elseEl = roleCtxFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeDefined();
      expect(elseEl).toBeNull();
    });

    it('should not display element when id return an empty permission set', () => {
      prepareRoleContextComponent({ anyOf: ['cluster-permissions-r'] }, { role: 'CLUSTER', id: 'unknown-id' });

      const mainEl = roleCtxFixture.nativeElement.querySelector('div');
      const elseEl = roleCtxFixture.nativeElement.querySelector('span');

      expect(mainEl).toBeNull();
      expect(elseEl).toBeDefined();
    });
  });
});
