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

import { GroupService } from './group.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeGroup } from '../entities/group/group.fixture';

describe('GroupService', () => {
  let httpTestingController: HttpTestingController;
  let groupService: GroupService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    groupService = TestBed.inject<GroupService>(GroupService);
  });

  describe('list', () => {
    it('should call the API', (done) => {
      const fakeGroups = [fakeGroup()];

      groupService.list().subscribe((groups) => {
        expect(groups).toMatchObject(fakeGroups);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeGroups);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
