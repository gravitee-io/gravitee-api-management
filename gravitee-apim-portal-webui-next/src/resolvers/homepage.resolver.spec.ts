/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { homepageResolver } from './homepage.resolver';
import { PortalPage } from '../entities/portal/portal-page';
import { PortalService } from '../services/portal.service';

describe('homepageResolver (Jest)', () => {
  let portalServiceMock: { getPortalHomepages: jest.Mock };
  let routerMock: { navigate: jest.Mock };

  beforeEach(() => {
    portalServiceMock = { getPortalHomepages: jest.fn() };
    routerMock = { navigate: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: PortalService, useValue: portalServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });
  });

  function runResolver() {
    const route = {} as unknown as import('@angular/router').ActivatedRouteSnapshot;
    const state = { url: '/' } as import('@angular/router').RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => homepageResolver(route, state));
  }

  it('returns the first homepage when available', done => {
    const pages = [{ id: 'id1' } as unknown as PortalPage, { id: 'id2' } as unknown as PortalPage];
    portalServiceMock.getPortalHomepages.mockReturnValue(of(pages));

    runResolver().subscribe(result => {
      expect(result).toBe(pages[0]);
      expect(routerMock.navigate).not.toHaveBeenCalled();
      done();
    });
  });

  it('navigates to catalog and returns null when no homepage', done => {
    portalServiceMock.getPortalHomepages.mockReturnValue(of([]));

    runResolver().subscribe(result => {
      expect(result).toBeNull();
      expect(routerMock.navigate).toHaveBeenCalledWith(['catalog']);
      done();
    });
  });

  it('navigates to catalog and returns null on error', done => {
    portalServiceMock.getPortalHomepages.mockReturnValue(throwError(() => new Error('network')));

    runResolver().subscribe(result => {
      expect(result).toBeNull();
      expect(routerMock.navigate).toHaveBeenCalledWith(['catalog']);
      done();
    });
  });

  it('handles undefined service result (nullish path)', done => {
    portalServiceMock.getPortalHomepages.mockReturnValue(of(undefined as unknown as PortalPage[]));

    runResolver().subscribe(result => {
      expect(result).toBeNull();
      expect(routerMock.navigate).toHaveBeenCalledWith(['catalog']);
      done();
    });
  });
});
