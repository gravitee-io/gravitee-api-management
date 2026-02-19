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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { of } from 'rxjs';

import { ApiProductNavigationComponent } from './api-product-navigation.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiProductNavigationComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  const fakeApiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    apiIds: ['api-1'],
  };

  let fixture: ComponentFixture<ApiProductNavigationComponent>;
  let _loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductNavigationComponent, RouterOutlet, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ apiProductId: API_PRODUCT_ID }),
            snapshot: { params: { apiProductId: API_PRODUCT_ID } },
            parent: null,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductNavigationComponent);
    _loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display API product name when loaded', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(req.request.method).toBe('GET');
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Test API Product');
  });

  it('should display Configuration menu item', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Configuration');
  });

  it('should show snackbar on load error', async () => {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });
});
