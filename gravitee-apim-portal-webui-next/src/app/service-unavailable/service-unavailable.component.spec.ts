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
import { MatButtonHarness } from '@angular/material/button/testing';
import { provideRouter, Router } from '@angular/router';

import { ServiceUnavailableComponent, ServiceUnavailableState } from './service-unavailable.component';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('ServiceUnavailableComponent', () => {
  let fixture: ComponentFixture<ServiceUnavailableComponent>;
  let harnessLoader: HarnessLoader;
  const originalLocation = window.location;
  const assign = jest.fn();

  async function init(state?: ServiceUnavailableState) {
    await TestBed.configureTestingModule({
      imports: [ServiceUnavailableComponent, AppTestingModule],
      providers: [provideRouter([{ path: '503', component: ServiceUnavailableComponent }])],
    }).compileComponents();

    await TestBed.inject(Router).navigate(['/503'], { state });

    Object.defineProperty(window, 'location', { configurable: true, value: { ...originalLocation, assign } });

    fixture = TestBed.createComponent(ServiceUnavailableComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  afterEach(() => {
    Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
    assign.mockReset();
  });

  it('should show the default unavailable message', async () => {
    await init({ status: 0 });

    expect(fixture.nativeElement.textContent).toContain(
      "Portal API unreachable or error occurs, please check logs. If the problem persists, try clearing this site's cookies and retry.",
    );
  });

  it('should show the maintenance message carried by the navigation state', async () => {
    await init({ status: 503, errors: [{ code: 'errors.maintenance.mode', message: 'Portal is under maintenance' }] });

    expect(fixture.nativeElement.textContent).toContain('Portal is under maintenance');
  });

  it('should load the portal home again when clicking retry', async () => {
    await init();
    const retryButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Retry' }));

    await retryButton.click();

    expect(assign).toHaveBeenCalledWith(document.baseURI);
  });
});
