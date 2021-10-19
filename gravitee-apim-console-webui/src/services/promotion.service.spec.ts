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

import { IHttpBackendService } from 'angular';

import { PromotionService } from './promotion.service';

import { PromotionTarget, fakePromotion, fakePromotionTarget } from '../entities/promotion';
import { setupAngularJsTesting } from '../../jest.setup.js';

setupAngularJsTesting();

describe('PromotionService', () => {
  let promotionService: PromotionService;
  let $httpBackend: IHttpBackendService;

  beforeEach(inject((_promotionService_, _$httpBackend_) => {
    promotionService = _promotionService_;
    $httpBackend = _$httpBackend_;
  }));

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('listPromotionTargets', () => {
    it('returns the data', (done) => {
      const environments: PromotionTarget[] = [fakePromotionTarget(), fakePromotionTarget()];

      $httpBackend
        .expectGET('https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/promotion-targets')
        .respond(environments);

      promotionService
        .listPromotionTargets()
        .then((result) => {
          expect(result).toEqual(environments);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });

  describe('promote', () => {
    it('call the endpoint', (done) => {
      const promotionTarget = fakePromotionTarget({
        installationId: 'inst#1',
        id: 'env#1',
      });
      const promotion = fakePromotion();
      $httpBackend
        .expectPOST('https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT/apis/apiId/_promote', {
          targetEnvCockpitId: 'env#1',
          targetEnvName: 'A name',
        })
        .respond(promotion);

      promotionService
        .promote('apiId', promotionTarget)
        .then((result) => {
          expect(result).toEqual(promotion);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });

  describe('processPromotion', () => {
    it('call the endpoint', (done) => {
      const promotion = fakePromotion();
      const isPromotionAccepted = true;

      $httpBackend
        .expectPOST(
          `https://url.test:3000/management/organizations/DEFAULT/promotions/${promotion.id}/_process`,
          // Need to cast because this function doesn't accept boolean
          isPromotionAccepted as any,
        )
        .respond(promotion);

      promotionService
        .processPromotion(promotion.id, isPromotionAccepted)
        .then((result) => {
          expect(result).toEqual(promotion);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });

  describe('listPromotion', () => {
    it('call the endpoint', (done) => {
      const promotion1 = fakePromotion();
      const promotion2 = fakePromotion();

      $httpBackend
        .expectPOST(
          'https://url.test:3000/management/organizations/DEFAULT/promotions/_search?apiId=api1&statuses=CREATED&statuses=TO_BE_VALIDATED',
        )
        .respond([promotion1, promotion2]);

      promotionService
        .listPromotion({ apiId: 'api1', statuses: ['CREATED', 'TO_BE_VALIDATED'] })
        .then((result) => {
          expect(result).toEqual([promotion1, promotion2]);
          done();
        })
        .catch(done.fail);

      $httpBackend.flush();
    });
  });
});
