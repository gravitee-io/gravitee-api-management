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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiProductConfirmDeploymentDialogComponent } from './api-product-confirm-deployment-dialog.component';
import { ApiProductConfirmDeploymentDialogHarness } from './api-product-confirm-deployment-dialog.component.harness';

import { GioTestingModule } from '../../../../shared/testing';

describe('ApiProductConfirmDeploymentDialogComponent', () => {
  const API_PRODUCT_ID = 'api-product-deploy-test';

  let fixture: ComponentFixture<ApiProductConfirmDeploymentDialogComponent>;
  let harness: ApiProductConfirmDeploymentDialogHarness;
  let dialogRefClose: jest.Mock;

  beforeEach(async () => {
    dialogRefClose = jest.fn();
    await TestBed.configureTestingModule({
      imports: [ApiProductConfirmDeploymentDialogComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        { provide: MAT_DIALOG_DATA, useValue: { apiProductId: API_PRODUCT_ID } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductConfirmDeploymentDialogComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductConfirmDeploymentDialogHarness);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should close dialog with true when user confirms deploy', async () => {
    await harness.clickDeploy();

    expect(dialogRefClose).toHaveBeenCalledWith(true);
  });
});
