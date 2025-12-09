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
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { documentationResolver } from './documentation.resolver';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from '../../../services/portal-navigation-items.service';

describe('documentationResolver', () => {
  const executeResolver = (route: ActivatedRouteSnapshot) => TestBed.runInInjectionContext(() => documentationResolver(route));

  const MOCK_ITEMS: PortalNavigationItem[] = [
    {
      id: 'l1',
      title: 'External link',
      type: 'LINK',
      order: 0,
      parentId: null,
      published: true,
    } as unknown as PortalNavigationItem,
    {
      id: 'p1',
      title: 'API',
      type: 'PAGE',
      order: 1,
      parentId: null,
      published: true,
    } as unknown as PortalNavigationItem,
  ];

  let routerSpy: jest.SpyInstance;

  beforeEach(() => {
    const mockRouter = { navigate: jest.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        {
          provide: PortalNavigationItemsService,
          useValue: {
            topNavbarItems: jest.fn().mockReturnValue(MOCK_ITEMS),
          },
        },
      ],
    });
    jest.clearAllMocks();
    routerSpy = jest.spyOn(mockRouter, 'navigate');
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });

  it('should navigate to root when navId is null', done => {
    const routeWithoutId = { params: {} } as unknown as ActivatedRouteSnapshot;
    const result$ = executeResolver(routeWithoutId);

    expect(routerSpy).toHaveBeenCalledWith(['/']);

    result$.subscribe(result => {
      expect(result).toBeNull();
      done();
    });
  });

  it('should navigate to 404 when item is not found', done => {
    const nonExistentRoute = { params: { navId: 'non-existent-id' } } as unknown as ActivatedRouteSnapshot;
    const result$ = executeResolver(nonExistentRoute);

    expect(routerSpy).toHaveBeenCalledWith(['/404']);

    result$.subscribe(result => {
      expect(result).toBeNull();
      done();
    });
  });

  it('should return item when it exists', done => {
    const existingNavId = MOCK_ITEMS[0].id;
    const existingRoute = { params: { navId: existingNavId } } as unknown as ActivatedRouteSnapshot;
    const result$ = executeResolver(existingRoute);

    expect(routerSpy).not.toHaveBeenCalled();

    result$.subscribe(result => {
      expect(result).toEqual(MOCK_ITEMS[0]);
      done();
    });
  });
});
