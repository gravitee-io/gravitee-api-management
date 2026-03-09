/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import { homepageContentResolver } from './homepage-content.resolver';
import { PortalNavigationItem } from '../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../entities/portal-navigation/portal-page-content';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('homepageContentResolver', () => {
  let routerSpy: jest.SpyInstance;
  let httpTestingController: HttpTestingController;
  let activatedRoute: ActivatedRouteSnapshot;
  let routerStateSnapshot: RouterStateSnapshot;

  beforeEach(() => {
    routerStateSnapshot = {} as RouterStateSnapshot;
    activatedRoute = {} as ActivatedRouteSnapshot;

    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [
        {
          provide: ActivatedRouteSnapshot,
          useValue: activatedRoute,
        },
        {
          provide: RouterStateSnapshot,
          useValue: routerStateSnapshot,
        },
      ],
    });

    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const executeResolver = () => TestBed.runInInjectionContext(() => homepageContentResolver(activatedRoute, routerStateSnapshot));

  it('returns the first homepage when available', done => {
    const mockHomepageItem: PortalNavigationItem = {
      published: false,
      order: 0,
      title: 'Home',
      id: 'page-123',
      area: 'HOMEPAGE',
      environmentId: 'env-id',
      organizationId: 'org-id',
      type: 'PAGE',
      portalPageContentId: 'homepage-content',
    };

    const mockPageContent: PortalPageContent = {
      content: '<h1>Welcome</h1>',
      type: 'GRAVITEE_MARKDOWN',
    };

    executeResolver().subscribe(result => {
      expect(result).toEqual(mockPageContent);
      done();
    });

    expectGetHomepagePortalNavigationItems([mockHomepageItem]);
    expectGetHomepageContent(mockHomepageItem.id, mockPageContent);
  });

  it('navigates to catalog and returns empty when no homepage items found', done => {
    executeResolver().subscribe({
      next: () => {
        // Should not emit a value if returning EMPTY
      },
      complete: () => {
        expect(routerSpy).toHaveBeenCalledWith(['catalog']);
        done();
      },
    });

    expectGetHomepagePortalNavigationItems([]);
  });

  it('navigates to catalog and returns empty on backend error', done => {
    executeResolver().subscribe({
      complete: () => {
        expect(routerSpy).toHaveBeenCalledWith(['catalog']);
        done();
      },
    });

    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/portal-navigation-items?area=HOMEPAGE&loadChildren=false`)
      .flush('Network Error', { status: 500, statusText: 'Server Error' });
  });

  it('handles undefined service result (nullish path)', done => {
    executeResolver().subscribe({
      complete: () => {
        expect(routerSpy).toHaveBeenCalledWith(['catalog']);
        done();
      },
    });

    const nullishResponse: unknown = null;
    expectGetHomepagePortalNavigationItems(nullishResponse as PortalNavigationItem[]);
  });

  function expectGetHomepagePortalNavigationItems(response: PortalNavigationItem[]) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-navigation-items?area=HOMEPAGE&loadChildren=false`).flush(response);
  }

  function expectGetHomepageContent(navId: string, response: PortalPageContent) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-navigation-items/${navId}/content`).flush(response);
  }
});
