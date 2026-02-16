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

import { PromotionService } from './promotion.service';

import { PromotionTarget, fakePromotion, fakePromotionTarget } from '../entities/promotion';
import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';

describe('PromotionService', () => {
  let httpTestingController: HttpTestingController;
  let promotionService: PromotionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    promotionService = TestBed.inject<PromotionService>(PromotionService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('listPromotionTargets', () => {
    it('should call the API', done => {
      const environments: PromotionTarget[] = [fakePromotionTarget(), fakePromotionTarget()];

      promotionService.listPromotionTargets().subscribe(result => {
        expect(result).toEqual(environments);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/promotion-targets`,
        })
        .flush(environments);
    });
  });

  describe('promote', () => {
    it('should ask for API promotion', done => {
      const promotionTarget = fakePromotionTarget({
        installationId: 'inst#1',
        id: 'env#1',
      });
      const promotion = fakePromotion();

      promotionService.promote('apiId', promotionTarget).subscribe(result => {
        expect(result).toEqual(promotion);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/apiId/_promote`,
      });
      expect(req.request.body).toEqual({
        targetEnvCockpitId: 'env#1',
        targetEnvName: 'A name',
      });
      req.flush(promotion);
    });
  });

  describe('processPromotion', () => {
    it('should call the API', done => {
      const promotion = fakePromotion();
      const isPromotionAccepted = true;

      promotionService.processPromotion(promotion.id, isPromotionAccepted).subscribe(result => {
        expect(result).toEqual(promotion);
        done();
      });

      httpTestingController
        .expectOne(
          `${CONSTANTS_TESTING.org.v2BaseURL}/promotions/${promotion.id}/_process`,
          // Need to cast because this function doesn't accept boolean
          isPromotionAccepted as any,
        )
        .flush(promotion);
    });
  });

  describe('listPromotion', () => {
    it('should call the API', done => {
      const promotion1 = fakePromotion();
      const promotion2 = fakePromotion();

      promotionService.listPromotion({ apiId: 'api1', statuses: ['CREATED', 'TO_BE_VALIDATED'] }).subscribe(result => {
        expect(result).toEqual([promotion1, promotion2]);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.org.baseURL}/promotions/_search?apiId=api1&statuses=CREATED&statuses=TO_BE_VALIDATED`,
        })
        .flush([promotion1, promotion2]);
    });
  });
});
