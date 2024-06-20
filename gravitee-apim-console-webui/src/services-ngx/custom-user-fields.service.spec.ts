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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { CustomUserFieldsService } from './custom-user-fields.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeCustomUserField } from '../entities/custom-user-fields/custom-user-fields.fixture';
import { CustomUserField } from '../entities/customUserFields';

describe('CustomUserFieldsService', () => {
  let httpTestingController: HttpTestingController;
  let service: CustomUserFieldsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(CustomUserFieldsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get custom user fields', () => {
    it('should return the user fields', (done) => {
      const fakeRes: CustomUserField[] = [fakeCustomUserField(), fakeCustomUserField()];

      service.list().subscribe((res) => {
        expect(res).toEqual(fakeRes);
        done();
      });

      expectGetCustomUserFieldsRequest(fakeRes);
    });
  });

  it('should create custom user fields', (done) => {
    const customUserField: CustomUserField = fakeCustomUserField();

    service.create(customUserField).subscribe((_) => {
      done();
    });

    expectPostCustomUserFieldsRequest(customUserField);
  });

  it('should update custom user fields', (done) => {
    const customUserField: CustomUserField = fakeCustomUserField();

    service.update(customUserField).subscribe((_) => {
      done();
    });

    expectPutCustomUserFieldsRequest(customUserField);
  });

  it('should delete custom user fields', (done) => {
    const customUserField: CustomUserField = fakeCustomUserField();

    service.delete(customUserField.key).subscribe((_) => {
      done();
    });

    expectDeleteCustomUserFieldsRequest(customUserField.key);
  });

  function expectGetCustomUserFieldsRequest(fakeRes: CustomUserField[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields`,
      })
      .flush(fakeRes);
  }

  function expectPostCustomUserFieldsRequest(payload: CustomUserField) {
    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields`,
    });

    expect(req.request.body).toEqual(payload);

    req.flush(null);
  }

  function expectPutCustomUserFieldsRequest(payload: CustomUserField) {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields/${payload.key}`,
    });

    expect(req.request.body).toEqual(payload);

    req.flush(null);
  }

  function expectDeleteCustomUserFieldsRequest(key: string) {
    const req = httpTestingController.expectOne({
      method: 'DELETE',
      url: `${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields/${key}`,
    });

    req.flush(null);
  }
});
