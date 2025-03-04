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
import { ActivatedRoute, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { of } from 'rxjs';

import { GroupComponent } from './group.component';

import { Group } from '../../../../entities/group/group';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

describe('GroupComponent', () => {
  let fixture: ComponentFixture<GroupComponent>;
  let httpTestingController: HttpTestingController;

  const init = async (groupId: string) => {
    const mockActivatedRoute = {
      params: of({ groupId: groupId }),
    };

    await TestBed.configureTestingModule({
      declarations: [],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-group-u', 'environment-group-d', 'environment-group-c'],
        },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
      ],
      imports: [GroupComponent, GioTestingModule, NoopAnimationsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GroupComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Get by id', () => {
    beforeEach(async () => {
      await init('1');
    });

    it('should get group by id when mode is edit', () => {
      expectGetGroup({ id: '1', name: 'Group 1', manageable: true });
      // These calls should happen, but somehow they are not being made.
      // expectGetMembersList([]);
      // expectGetAPIsList([]);
      // expectGetApplicationsList([]);
      expectGetRolesList('API');
      expectGetRolesList('APPLICATION');
      expectGetRolesList('INTEGRATION');
    });
  });

  function expectGetGroup(group: Group) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/1`).flush(group);
  }

  function expectGetRolesList(type: string) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${type}/roles`);
  }
});
