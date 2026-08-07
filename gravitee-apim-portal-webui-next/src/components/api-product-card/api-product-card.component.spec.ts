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

import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ApiProductCardComponent } from './api-product-card.component';
import { ApiProductCardHarness } from './api-product-card.harness';

describe('ApiProductCardComponent', () => {
  let fixture: ComponentFixture<ApiProductCardComponent>;
  let harness: ApiProductCardHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiProductCardComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductCardComponent);
    fixture.componentRef.setInput('apiProductId', 'product-1');
    fixture.componentRef.setInput('title', 'AI Workspace');
    fixture.componentRef.setInput('content', 'APIs for AI applications');
    fixture.componentRef.setInput('apiNames', ['Chat API', 'Embedding API']);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductCardHarness);
  });

  it('should display API Product information', async () => {
    expect(await harness.getTitle()).toBe('AI Workspace');
    expect(await harness.getDescription()).toBe('APIs for AI applications');
    expect(await harness.getType()).toBe('API PRODUCT');
    expect(await harness.getApiCount()).toContain('2 APIS INCLUDED');
    expect(await harness.hasApiLabels()).toBe(true);
  });

  it('should use a singular included API label', async () => {
    fixture.componentRef.setInput('apiNames', ['Chat API']);
    fixture.detectChanges();

    expect(await harness.getApiCount()).toContain('1 API INCLUDED');
  });

  it('should emit the API Product id when selected', async () => {
    const selected = jest.fn();
    fixture.componentInstance.cardSelect.subscribe(selected);

    await harness.select();

    expect(selected).toHaveBeenCalledWith('product-1');
  });
});
