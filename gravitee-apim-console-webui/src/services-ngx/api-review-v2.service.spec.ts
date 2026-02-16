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

import { ApiReviewV2Service } from './api-review-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('ApiReviewV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiReviewV2Service: ApiReviewV2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiReviewV2Service = TestBed.inject<ApiReviewV2Service>(ApiReviewV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('ask', () => {
    it('should call the API', done => {
      const apiId = 'api-id';
      const reviewMessage = 'review-message';

      apiReviewV2Service.ask(apiId, reviewMessage).subscribe({
        error: err => {
          done.fail(err);
        },
        complete: () => {
          done();
        },
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/reviews/_ask`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ message: reviewMessage });

      req.flush({});
    });
  });

  describe('accept', () => {
    it('should call the API', done => {
      const apiId = 'api-id';
      const reviewMessage = 'review-message';

      apiReviewV2Service.accept(apiId, reviewMessage).subscribe({
        error: err => {
          done.fail(err);
        },
        complete: () => {
          done();
        },
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/reviews/_accept`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ message: reviewMessage });

      req.flush({});
    });
  });

  describe('reject', () => {
    it('should call the API', done => {
      const apiId = 'api-id';
      const reviewMessage = 'review-message';

      apiReviewV2Service.reject(apiId, reviewMessage).subscribe({
        error: err => {
          done.fail(err);
        },
        complete: () => {
          done();
        },
      });

      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/reviews/_reject`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ message: reviewMessage });

      req.flush({});
    });
  });
});
