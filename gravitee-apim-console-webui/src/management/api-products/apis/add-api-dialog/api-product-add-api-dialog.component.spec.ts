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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ApiProductAddApiDialogComponent, ApiProductAddApiDialogData } from './api-product-add-api-dialog.component';

import { GioTestingModule } from '../../../../shared/testing';
import { fakeProxyApiV4 } from '../../../../entities/management-api-v2';

describe('ApiProductAddApiDialogComponent', () => {
  let fixture: ComponentFixture<ApiProductAddApiDialogComponent>;
  let loader: HarnessLoader;
  let dialogRef: MatDialogRef<ApiProductAddApiDialogComponent>;

  const API_PRODUCT_ID = 'product-1';

  const dialogData: ApiProductAddApiDialogData = {
    apiProductId: API_PRODUCT_ID,
    existingApiIds: ['api-1'],
  };

  const fakeApi = fakeProxyApiV4({
    id: 'api-2',
    name: 'New API',
    apiVersion: '1.0',
    listeners: [
      {
        type: 'HTTP',
        paths: [{ path: '/new-api' }],
        entrypoints: [{ type: 'http-proxy' }],
      },
    ] as any,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiProductAddApiDialogComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductAddApiDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    dialogRef = TestBed.inject(MatDialogRef);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should close dialog on cancel', async () => {
    fixture.detectChanges();
    const cancelButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="cancel-button"]' }));
    await cancelButton.click();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('should disable submit when no APIs selected', async () => {
    fixture.detectChanges();
    const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="submit-button"]' }));
    expect(await submitButton.isDisabled()).toBe(true);
  });

  it('should close with selected APIs on successful submit', async () => {
    fixture.componentInstance.selectedApis.set([fakeApi]);
    fixture.detectChanges();

    const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="submit-button"]' }));
    await submitButton.click();

    expect(dialogRef.close).toHaveBeenCalledWith([fakeApi]);
  });

  it('should show validation error when API already in product', async () => {
    const existingApi = fakeProxyApiV4({
      id: 'api-1',
      name: 'Existing API',
      apiVersion: '1.0',
      listeners: [{ type: 'HTTP', paths: [{ path: '/existing-api' }], entrypoints: [{ type: 'http-proxy' }] }] as any,
    });
    fixture.componentInstance.selectedApis.set([existingApi]);
    fixture.detectChanges();

    const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="submit-button"]' }));
    await submitButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.validationError()).toContain('already in this API Product');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
