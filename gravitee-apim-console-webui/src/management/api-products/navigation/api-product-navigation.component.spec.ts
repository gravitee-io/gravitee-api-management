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
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';

describe('ApiProductNavigationComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  const fakeApiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    apiIds: ['api-1'],
  };

  let fixture: ComponentFixture<ApiProductNavigationComponent>;
  let httpTestingController: HttpTestingController;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  function flushRequests(product: ApiProduct, verifyOk = true, verifyReason?: string): void {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`).flush(product);
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/deployments/_verify`)
      .flush({ ok: verifyOk, ...(verifyReason ? { reason: verifyReason } : {}) });
  }

  function createFixture(): void {
    fixture = TestBed.createComponent(ApiProductNavigationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductNavigationComponent, RouterOutlet, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        { provide: GioTestingPermissionProvider, useValue: ['api_product-definition-u'] },
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
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', async () => {
    createFixture();
    flushRequests(fakeApiProduct);
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display API product name when loaded', async () => {
    createFixture();
    flushRequests(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Test API Product');
  });

  it('should display Configuration menu item', async () => {
    createFixture();
    flushRequests(fakeApiProduct);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Configuration');
  });

  it('should show snackbar on load error', async () => {
    createFixture();
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`)
      .flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/deployments/_verify`)
      .flush({ ok: true });
    await fixture.whenStable();

    expect(fakeSnackBarService.error).toHaveBeenCalled();
  });

  it('should not show out-of-sync banner when API Product is deployed', async () => {
    createFixture();
    flushRequests({ ...fakeApiProduct, deploymentState: 'DEPLOYED' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('out of sync');
  });

  it('should show out-of-sync banner with Deploy button when NEED_REDEPLOY and license ok', async () => {
    createFixture();
    flushRequests({ ...fakeApiProduct, deploymentState: 'NEED_REDEPLOY' }, true);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This API Product is out of sync.');
    expect(fixture.nativeElement.textContent).toContain('Deploy API Product');
  });

  it('should show out-of-sync banner without Deploy button when user lacks update permission', async () => {
    TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: [] });
    createFixture();
    flushRequests({ ...fakeApiProduct, deploymentState: 'NEED_REDEPLOY' }, true);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This API Product is out of sync.');
    expect(fixture.nativeElement.textContent).not.toContain('Deploy API Product');
  });

  it('should show cannot-be-deployed banner with body when license check fails', async () => {
    createFixture();
    flushRequests(fakeApiProduct, false, 'API Product deployment requires a universe license.');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This API Product cannot be deployed.');
    expect(fixture.nativeElement.innerHTML).toContain('API Product deployment requires a universe license.');
  });

  it('should re-fetch api product and show banner after planStateVersion changes', async () => {
    createFixture();
    flushRequests({ ...fakeApiProduct, deploymentState: 'DEPLOYED' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('out of sync');

    TestBed.inject(ApiProductV2Service).notifyPlanStateChanged();
    fixture.detectChanges();

    flushRequests({ ...fakeApiProduct, deploymentState: 'NEED_REDEPLOY' }, true);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('This API Product is out of sync.');
  });

  describe('Consumers menu item visibility', () => {
    it('should show consumers menu item when user has api product plan read permission', async () => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api_product-plan-r'] });
      createFixture();
      flushRequests(fakeApiProduct);
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Consumers');
    });

    it('should hide consumers menu item when user lacks api product plan read permission', async () => {
      TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: ['api_product-definition-r'] });
      createFixture();
      flushRequests(fakeApiProduct);
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).not.toContain('Consumers');
    });
  });
});
