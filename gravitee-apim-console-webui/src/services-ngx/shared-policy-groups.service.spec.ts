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

import { GioTestingModule } from '../shared/testing';
import { fakeCreateSharedPolicyGroup, fakeUpdateSharedPolicyGroup } from '../entities/management-api-v2';

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
    });
  });

  describe('create', () => {
    it('should call the API', (done) => {
      service.create(fakeCreateSharedPolicyGroup()).subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });
    });
  });

  describe('get', () => {
    it('should call the API', (done) => {
      service.get('SEARCH_SPG').subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });
    });
  });

  describe('update', () => {
    it('should call the API', (done) => {
      service.update('SEARCH_SPG', fakeUpdateSharedPolicyGroup()).subscribe((spg) => {
        expect(spg).toBeTruthy();
        done();
      });
    });
  });
});
