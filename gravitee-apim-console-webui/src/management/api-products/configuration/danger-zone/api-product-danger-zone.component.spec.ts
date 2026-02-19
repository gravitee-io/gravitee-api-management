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

import { ApiProductDangerZoneComponent } from './api-product-danger-zone.component';

import { GioTestingModule } from '../../../../shared/testing';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';

@Component({
  standalone: true,
  template: `<api-product-danger-zone
    [apiProduct]="apiProduct()"
    [isReadOnly]="isReadOnly()"
    (removeApisClick)="onRemoveApis()"
    (deleteApiProductClick)="onDeleteApiProduct()"
  ></api-product-danger-zone>`,
  imports: [ApiProductDangerZoneComponent],
})
class TestHostComponent {
  apiProduct = signal<ApiProduct>({
    id: 'product-1',
    name: 'Test Product',
    version: '1.0',
    apiIds: ['api-1', 'api-2'],
  });
  isReadOnly = signal(false);
  removeApisEmitted = false;
  deleteApiProductEmitted = false;

  onRemoveApis(): void {
    this.removeApisEmitted = true;
  }

  onDeleteApiProduct(): void {
    this.deleteApiProductEmitted = true;
  }
}

describe('ApiProductDangerZoneComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;
  let hostComponent: TestHostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(hostComponent).toBeTruthy();
  });

  it('should emit removeApisClick when Remove APIs button is clicked', async () => {
    const removeButton = await loader.getHarness(MatButtonHarness.with({ text: /Remove APIs/i }));
    await removeButton.click();

    expect(hostComponent.removeApisEmitted).toBe(true);
  });

  it('should emit deleteApiProductClick when Delete API Product button is clicked', async () => {
    const deleteButton = await loader.getHarness(MatButtonHarness.with({ text: /Delete API Product/i }));
    await deleteButton.click();

    expect(hostComponent.deleteApiProductEmitted).toBe(true);
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
