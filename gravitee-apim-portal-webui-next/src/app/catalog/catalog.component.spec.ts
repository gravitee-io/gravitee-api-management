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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardHarness } from '@angular/material/card/testing';

import { CatalogComponent } from './catalog.component';

describe('CatalogComponent', () => {
  let component: CatalogComponent;
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render banner text', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Welcome to Gravitee Developer Portal!');
  });

  it('should show empty API list', async () => {
    component.apis = [];
    const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
    expect(noApiCard).toBeTruthy();
    expect(await noApiCard.getText()).toContain('Sorry, there are no APIs listed yet.');
  });

  it('should show API list', async () => {
    component.apis = [
      {
        title: 'Test tile',
        version: 'v.1.2',
        content:
          'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
        id: 1,
      },
    ];
    const debugApiCardElement = fixture.debugElement.nativeElement.querySelector('app-api-card');
    expect(debugApiCardElement).toBeDefined();
  });
});
