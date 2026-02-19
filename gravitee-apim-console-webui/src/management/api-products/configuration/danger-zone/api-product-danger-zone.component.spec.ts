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
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { GioConfirmAndValidateDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiProductDangerZoneComponent } from './api-product-danger-zone.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';
import { Constants } from '../../../../entities/Constants';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  standalone: true,
  template: `<api-product-danger-zone [apiProduct]="apiProduct()" (reloadDetails)="onReload()"></api-product-danger-zone>`,
  imports: [ApiProductDangerZoneComponent],
})
class TestHostComponent {
  apiProduct = signal<ApiProduct>({
    id: 'product-1',
    name: 'Test Product',
    version: '1.0',
    apiIds: ['api-1', 'api-2'],
  });
  reloadEmitted = false;
  onReload(): void {
    this.reloadEmitted = true;
  }
}

describe('ApiProductDangerZoneComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  let hostComponent: TestHostComponent;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: {} },
            parent: { snapshot: { params: { apiProductId: API_PRODUCT_ID } } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    routerNavigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should create', () => {
    expect(hostComponent).toBeTruthy();
  });

  it('should remove all APIs when confirmed', async () => {
    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Yes, remove them/i }));
    await confirmButton.click();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/apis`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('All APIs have been removed from the API Product.');
    expect(hostComponent.reloadEmitted).toBe(true);
  });

  it('should delete API product when confirmed', async () => {
    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /Delete API Product/i }));
    await deleteButton.click();

    const dialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await dialog.confirm();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('The API Product has been deleted.');
    expect(routerNavigateSpy).toHaveBeenCalledWith(['../../'], expect.anything());
  });

  it('should always show Remove APIs button', async () => {
    const removeButtons = await loader.getAllHarnesses(MatButtonHarness.with({ text: /Remove APIs/i }));
    expect(removeButtons.length).toBe(1);
  });

  it('should show Remove APIs button even when product has no APIs', async () => {
    hostComponent.apiProduct.set({ id: 'product-2', name: 'Empty', version: '1.0', apiIds: [] });
    fixture.detectChanges();
    await fixture.whenStable();

    const removeButtons = await loader.getAllHarnesses(MatButtonHarness.with({ text: /Remove APIs/i }));
    expect(removeButtons.length).toBe(1);
  });
});
