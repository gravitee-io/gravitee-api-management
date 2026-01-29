/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { ApiProductNavigationComponent } from './api-product-navigation.component';
import { ApiProductNavigationModule } from './api-product-navigation.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioMenuService } from '@gravitee/ui-particles-angular';

describe('ApiProductNavigationComponent', () => {
  let fixture: ComponentFixture<ApiProductNavigationComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let router: Router;
  const API_PRODUCT_ID = 'api-product-id';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductNavigationModule, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            parent: {
              snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            },
            params: new Subject(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductNavigationComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load API product on init', fakeAsync(() => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(apiProduct);
    tick();
    fixture.detectChanges();

    expect(fixture.componentInstance.currentApiProduct).toEqual(apiProduct);
    expect(fixture.componentInstance.isLoading).toBe(false);
  }));

  it('should handle error when loading API product', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    tick();
    fixture.detectChanges();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
    expect(fixture.componentInstance.isLoading).toBe(false);
  }));

  it('should reload API product on route params change', fakeAsync(() => {
    const apiProduct: ApiProduct = {
      id: API_PRODUCT_ID,
      name: 'Test Product',
      version: '1.0',
      apiIds: [],
    };

    fixture.detectChanges();
    tick();

    const req1 = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req1.flush(apiProduct);
    tick();
    fixture.detectChanges();

    // Component should have loaded the API product
    expect(fixture.componentInstance.currentApiProduct).toEqual(apiProduct);
  }));

  it('should check if menu item is active', () => {
    fixture.detectChanges();

    const menuItem = {
      displayName: 'Configuration',
      routerLink: 'configuration',
      icon: 'gio:settings',
    };

    jest.spyOn(router, 'isActive').mockReturnValue(true);
    const isActive = fixture.componentInstance.isActive(menuItem);
    expect(isActive).toBe(true);
  });

  it('should have correct submenu items', () => {
    fixture.detectChanges();

    expect(fixture.componentInstance.subMenuItems).toEqual([
      {
        displayName: 'Configuration',
        routerLink: 'configuration',
        icon: 'gio:settings',
      },
      {
        displayName: 'APIs',
        routerLink: 'apis',
        icon: 'gio:cloud',
      },
      {
        displayName: 'Consumers',
        routerLink: 'consumers',
        icon: 'gio:users',
      },
    ]);
  });

  it('should update breadcrumb visibility based on menu service', () => {
    const gioMenuService = TestBed.inject(GioMenuService);
    fixture.detectChanges();

    gioMenuService.reduced$.next(true);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasBreadcrumb).toBe(true);
  });
});
