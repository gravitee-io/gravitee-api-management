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
import { switchMap, tap } from 'rxjs/operators';

import { GioLicenseService } from './gio-license.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../testing';
import { AjsRootScope } from '../../../ajs-upgraded-providers';
import { License } from '../../../entities/license/License';

describe('GioLicenseService', () => {
  let httpTestingController: HttpTestingController;
  let gioLicenseService: GioLicenseService;
  const fakeRootScope = { $broadcast: jest.fn(), $on: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
      providers: [{ provide: AjsRootScope, useValue: fakeRootScope }],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    gioLicenseService = TestBed.inject<GioLicenseService>(GioLicenseService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const mockLicense: License = {
    tier: 'tier',
    packs: [],
    features: [],
  };

  it('should call the API', (done) => {
    gioLicenseService.loadLicense().subscribe((response) => {
      expect(response).toMatchObject(mockLicense);
      done();
    });

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.v2BaseURL}/license`,
    });

    req.flush(mockLicense);
  });

  it('should get default license', (done) => {
    gioLicenseService.getLicense().subscribe((response) => {
      expect(response).toBeNull();
      done();
    });
  });

  it('should get license', (done) => {
    gioLicenseService
      .loadLicense()
      .pipe(
        switchMap(() => gioLicenseService.getLicense()),
        tap((license) => {
          expect(license).toMatchObject(mockLicense);
          done();
        }),
      )
      .subscribe();

    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.v2BaseURL}/license`,
    });

    req.flush(mockLicense);
  });

  it('should get feature more information', () => {
    expect(gioLicenseService.getFeatureMoreInformation('apim-custom-roles')).not.toBeNull();
  });

  it('should throw error when get more information with wrong feature', () => {
    expect(() => gioLicenseService.getFeatureMoreInformation('bad')).toThrow();
  });
});
