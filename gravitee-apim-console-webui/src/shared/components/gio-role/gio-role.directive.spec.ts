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

import { GioRoleModule } from './gio-role.module';
import { GioRoleCheckOptions } from './gio-role.directive';
import { GioRoleService } from './gio-role.service';

import { User } from '../../../entities/user/user';
import { fakeAdminUser } from '../../../entities/user/user.fixture';

@Component({ template: `<div *gioRole="role">A Content</div>`, standalone: false })
class TestRoleComponent {
  @Input()
  role: GioRoleCheckOptions;
}

describe('GioRoleDirective', () => {
  let fixture: ComponentFixture<TestRoleComponent>;
  let gioRoleService: GioRoleService;
  const currentUser: User = fakeAdminUser();
  currentUser.roles = [];

  function prepareTestRoleComponent(role: GioRoleCheckOptions) {
    fixture = TestBed.configureTestingModule({
      declarations: [TestRoleComponent],
      imports: [GioRoleModule],
    }).createComponent(TestRoleComponent);

    gioRoleService = TestBed.inject(GioRoleService);
    gioRoleService.loadCurrentUserRoles(currentUser);

    fixture.componentInstance.role = role;
    fixture.detectChanges();
  }

  afterEach(() => {
    currentUser.roles = [];
  });

  describe('anyOf', () => {
    it('should hide element if role is not matching', () => {
      currentUser.roles = [{ scope: 'ENVIRONMENT', name: 'ADMIN' }];

      prepareTestRoleComponent({ anyOf: [{ scope: 'ORGANIZATION', name: 'ADMIN' }] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should display element if role is matching', () => {
      currentUser.roles = [
        { scope: 'ENVIRONMENT', name: 'ADMIN' },
        { scope: 'ORGANIZATION', name: 'ADMIN' },
      ];

      prepareTestRoleComponent({ anyOf: [{ scope: 'ORGANIZATION', name: 'ADMIN' }] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should display element if at least one role is matching', () => {
      currentUser.roles = [
        { scope: 'ENVIRONMENT', name: 'ADMIN' },
        { scope: 'ORGANIZATION', name: 'ADMIN' },
      ];

      prepareTestRoleComponent({
        anyOf: [
          { scope: 'ORGANIZATION', name: 'ADMIN' },
          { scope: 'ENVIRONMENT', name: 'USER' },
        ],
      });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });
  });

  describe('noneOf', () => {
    it('should display element if no role is matching', () => {
      currentUser.roles = [
        { scope: 'ENVIRONMENT', name: 'ADMIN' },
        { scope: 'ORGANIZATION', name: 'ADMIN' },
      ];

      prepareTestRoleComponent({ noneOf: [{ scope: 'ENVIRONMENT', name: 'USER' }] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeDefined();
    });

    it('should hide element if a role is matching', () => {
      currentUser.roles = [
        { scope: 'ENVIRONMENT', name: 'ADMIN' },
        { scope: 'ORGANIZATION', name: 'ADMIN' },
      ];

      prepareTestRoleComponent({ noneOf: [{ scope: 'ENVIRONMENT', name: 'ADMIN' }] });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });

    it('should hide element if at least one role is matching', () => {
      currentUser.roles = [
        { scope: 'ENVIRONMENT', name: 'ADMIN' },
        { scope: 'ORGANIZATION', name: 'ADMIN' },
      ];

      prepareTestRoleComponent({
        noneOf: [
          { scope: 'ORGANIZATION', name: 'ADMIN' },
          { scope: 'ENVIRONMENT', name: 'USER' },
        ],
      });
      fixture.detectChanges();

      const inputEl = fixture.nativeElement.querySelector('div');
      expect(inputEl).toBeNull();
    });
  });
});
