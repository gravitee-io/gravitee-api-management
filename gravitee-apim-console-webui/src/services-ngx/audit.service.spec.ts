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

import { AuditService } from './audit.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { fakeMetadataPageAudit } from '../entities/audit/Audit.fixture';

describe('AuditService', () => {
  let httpTestingController: HttpTestingController;
  let auditService: AuditService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    auditService = TestBed.inject<AuditService>(AuditService);
  });

  describe('listByOrganization', () => {
    it('should call the API', (done) => {
      const fakeAuditPage = fakeMetadataPageAudit();

      auditService.listByOrganization().subscribe((response) => {
        expect(response).toEqual(fakeAuditPage);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit?page=1&size=10`,
        })
        .flush(fakeAuditPage);
    });

    it('should call the API with environment filters', (done) => {
      const fakeAuditPage = fakeMetadataPageAudit();

      auditService
        .listByOrganization({ event: 'HELLO', referenceType: 'ENVIRONMENT', apiId: 'NOOP', environmentId: 'envId' })
        .subscribe((response) => {
          expect(response).toEqual(fakeAuditPage);
          done();
        });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit?page=1&size=10&event=HELLO&type=ENVIRONMENT&environment=envId`,
        })
        .flush(fakeAuditPage);
    });

    it('should call the API with application filters', (done) => {
      const fakeAuditPage = fakeMetadataPageAudit();

      auditService.listByOrganization({ referenceType: 'APPLICATION', applicationId: 'appId' }).subscribe((response) => {
        expect(response).toEqual(fakeAuditPage);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit?page=1&size=10&type=APPLICATION&application=appId`,
        })
        .flush(fakeAuditPage);
    });

    it('should call the API with api filters', (done) => {
      const fakeAuditPage = fakeMetadataPageAudit();

      auditService.listByOrganization({ referenceType: 'API', apiId: 'apiId', environmentId: 'NOOP' }).subscribe((response) => {
        expect(response).toEqual(fakeAuditPage);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit?page=1&size=10&type=API&api=apiId`,
        })
        .flush(fakeAuditPage);
    });

    it('should call the API with from & to filters', (done) => {
      const fakeAuditPage = fakeMetadataPageAudit();

      auditService.listByOrganization({ from: 10000, to: 20000 }).subscribe((response) => {
        expect(response).toEqual(fakeAuditPage);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit?page=1&size=10&from=10000&to=20000`,
        })
        .flush(fakeAuditPage);
    });
  });

  describe('getAllEventsNameByOrganization', () => {
    it('should call the API', (done) => {
      const fakeEvents = ['HELLO', 'WORLD'];

      auditService.getAllEventsNameByOrganization().subscribe((response) => {
        expect(response).toEqual(fakeEvents);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/audit/events`,
        })
        .flush(fakeEvents);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
