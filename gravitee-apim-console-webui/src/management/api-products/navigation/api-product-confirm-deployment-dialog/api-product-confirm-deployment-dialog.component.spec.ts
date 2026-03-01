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
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiProductConfirmDeploymentDialogComponent } from './api-product-confirm-deployment-dialog.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('ApiProductConfirmDeploymentDialogComponent', () => {
  const API_PRODUCT_ID = 'api-product-deploy-test';

  let fixture: ComponentFixture<ApiProductConfirmDeploymentDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let dialogRefClose: jest.Mock;
  const snackBarService = { success: jest.fn(), error: jest.fn() };

  beforeEach(() => {
    dialogRefClose = jest.fn();
    TestBed.configureTestingModule({
      imports: [ApiProductConfirmDeploymentDialogComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: SnackBarService, useValue: snackBarService },
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        { provide: MAT_DIALOG_DATA, useValue: { apiProductId: API_PRODUCT_ID } },
      ],
    });
    fixture = TestBed.createComponent(ApiProductConfirmDeploymentDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should display dialog title and deploy confirmation message', () => {
    expect(fixture.nativeElement.textContent).toContain('Deploy your API Product');
    expect(fixture.nativeElement.textContent).toContain('You are about to deploy your API Product to the gateway.');
  });

  it('should call deploy endpoint and on success close dialog and show success snackbar', async () => {
    const deployButton = await loader.getHarness(MatButtonHarness.with({ text: 'Deploy' }));
    await deployButton.click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/deployments`,
      method: 'POST',
    });
    req.flush({ id: API_PRODUCT_ID, name: 'Product', version: '1.0' });
    await fixture.whenStable();

    expect(dialogRefClose).toHaveBeenCalled();
    expect(snackBarService.success).toHaveBeenCalledWith('API Product successfully deployed.');
  });

  it('should show error snackbar and not close dialog when deploy fails', async () => {
    const deployButton = await loader.getHarness(MatButtonHarness.with({ text: 'Deploy' }));
    await deployButton.click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/deployments`,
      method: 'POST',
    });
    req.flush({ message: 'Deployment not allowed' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();

    expect(snackBarService.error).toHaveBeenCalled();
    expect(dialogRefClose).not.toHaveBeenCalled();
  });
});
