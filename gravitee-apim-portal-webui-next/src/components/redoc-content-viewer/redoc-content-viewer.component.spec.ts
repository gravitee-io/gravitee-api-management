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

import { RedocContentViewerComponent } from './redoc-content-viewer.component';
import { RedocContentViewerHarness } from './redoc-content-viewer.harness';
import { RedocService } from '../../services/redoc.service';

describe('RedocContentViewerComponent', () => {
  let fixture: ComponentFixture<RedocContentViewerComponent>;
  let harness: RedocContentViewerHarness;
  let redocServiceInitSpy: ReturnType<typeof jest.fn>;

  const openApiSpec = 'openapi: 3.0.0\ninfo:\n  title: Test\n  version: 1.0.0';

  beforeEach(async () => {
    redocServiceInitSpy = jest.fn();
    const redocService = { init: redocServiceInitSpy };

    await TestBed.configureTestingModule({
      imports: [RedocContentViewerComponent],
      providers: [{ provide: RedocService, useValue: redocService }],
    }).compileComponents();

    fixture = TestBed.createComponent(RedocContentViewerComponent);
    fixture.componentRef.setInput('content', openApiSpec);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, RedocContentViewerHarness);
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should call RedocService.init with content and options', () => {
    jest.advanceTimersByTime(0); // Flush setTimeout so RedocService.init is called
    expect(redocServiceInitSpy).toHaveBeenCalledWith(
      openApiSpec,
      expect.objectContaining({
        hideDownloadButton: false,
        theme: { breakpoints: { medium: '50rem', large: '75rem' } },
      }),
      expect.anything(),
    );
  });

  it('should render redoc container', async () => {
    const redoc = await harness.getRedoc();
    expect(redoc).toBeTruthy();
  });
});
