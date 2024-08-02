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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SharedPolicyGroupsService } from './shared-policy-groups.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  CreateSharedPolicyGroup,
  fakeCreateSharedPolicyGroup,
  fakePagedResult,
  fakeSharedPolicyGroup,
  fakeUpdateSharedPolicyGroup,
  PagedResult,
  SharedPolicyGroup,
  UpdateSharedPolicyGroup,
} from '../entities/management-api-v2';

describe('SharedPolicyGroupsService', () => {
  let httpTestingController: HttpTestingController;
  let service: SharedPolicyGroupsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<SharedPolicyGroupsService>(SharedPolicyGroupsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('list', () => {
    it('should call the API', (done) => {
      service.list().subscribe((SPGs) => {
        expect(SPGs.data.length).toEqual(1);
        done();
      });

      expectListSharedPolicyGroupsRequest(httpTestingController, fakePagedResult([fakeSharedPolicyGroup()]));
    });
  });

  describe('create', () => {
    it('should call the API', (done) => {
      service.create(fakeCreateSharedPolicyGroup()).subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });

      expectCreateSharedPolicyGroupRequest(httpTestingController, fakeCreateSharedPolicyGroup());
    });
  });

  describe('get', () => {
    it('should call the API', (done) => {
      service.get(fakeSharedPolicyGroup().id).subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });

      expectGetSharedPolicyGroupRequest(httpTestingController, fakeSharedPolicyGroup());
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      service.update('spgId', fakeUpdateSharedPolicyGroup()).subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });

      expectUpdateSharedPolicyGroupRequest(httpTestingController, 'spgId', fakeUpdateSharedPolicyGroup());
    });
  });

  describe('delete', () => {
    it('should call the API', (done) => {
      service.delete('spgId').subscribe(() => {
        done();
      });
      expectDeleteSharedPolicyGroupRequest(httpTestingController, 'spgId');
    });
  });
});

export const expectListSharedPolicyGroupsRequest = (
  httpTestingController: HttpTestingController,
  sharedPolicyGroups: PagedResult<SharedPolicyGroup> = fakePagedResult([fakeSharedPolicyGroup()]),
  queryParams: string = '?page=1&perPage=25',
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups${queryParams}`);
  expect(req.request.method).toEqual('GET');
  req.flush(sharedPolicyGroups);
};

export const expectGetSharedPolicyGroupRequest = (
  httpTestingController: HttpTestingController,
  sharedPolicyGroup: SharedPolicyGroup = fakeSharedPolicyGroup(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups/${sharedPolicyGroup.id}`);
  expect(req.request.method).toEqual('GET');
  req.flush(sharedPolicyGroup);
};

export const expectCreateSharedPolicyGroupRequest = (
  httpTestingController: HttpTestingController,
  expectedCreateSharedPolicyGroup: CreateSharedPolicyGroup,
  sharedPolicyGroupCreated: SharedPolicyGroup = fakeSharedPolicyGroup(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups`);
  expect(req.request.method).toEqual('POST');
  expect(req.request.body).toStrictEqual(expectedCreateSharedPolicyGroup);
  req.flush(sharedPolicyGroupCreated);
};

export const expectUpdateSharedPolicyGroupRequest = (
  httpTestingController: HttpTestingController,
  sharedPolicyGroupId: string,
  expectedUpdateSharedPolicyGroup: UpdateSharedPolicyGroup,
  sharedPolicyGroupUpdated: SharedPolicyGroup = fakeSharedPolicyGroup(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups/${sharedPolicyGroupId}`);
  expect(req.request.method).toEqual('PUT');
  expect(req.request.body).toStrictEqual(expectedUpdateSharedPolicyGroup);
  req.flush(sharedPolicyGroupUpdated);
};

export const expectDeleteSharedPolicyGroupRequest = (httpTestingController: HttpTestingController, sharedPolicyGroupId: string) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups/${sharedPolicyGroupId}`);
  expect(req.request.method).toEqual('DELETE');
  req.flush({});
};
