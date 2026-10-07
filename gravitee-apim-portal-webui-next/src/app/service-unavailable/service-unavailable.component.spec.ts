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

import { ServiceUnavailableComponent } from './service-unavailable.component';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('ServiceUnavailableComponent', () => {
  let fixture: ComponentFixture<ServiceUnavailableComponent>;
  let harnessLoader: HarnessLoader;
  const assign = jest.fn();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceUnavailableComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ServiceUnavailableComponent);
    // The real document renders the component; only the navigation goes through the fake one
    Object.assign(fixture.componentInstance, { document: { location: { assign }, baseURI: 'http://localhost/portal/' } });
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    assign.mockReset();
  });

  it('should show the default unavailable message', () => {
    expect(fixture.nativeElement.textContent).toContain(
      "Portal API unreachable or error occurs, please check logs. If the problem persists, try clearing this site's cookies and retry.",
    );
  });

  it('should load the portal home again when clicking retry', async () => {
    const retryButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Retry' }));

    await retryButton.click();

    expect(assign).toHaveBeenCalledWith('http://localhost/portal/');
  });
});
