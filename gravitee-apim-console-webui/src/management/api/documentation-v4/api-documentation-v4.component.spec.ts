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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiDocumentationV4Component } from './api-documentation-v4.component';
import { ApiDocumentationV4Module } from './api-documentation-v4.module';
import { ApiDocumentationV4EmptyStateHarness } from './documentation-empty-state/api-documentation-v4-empty-state.harness';

describe('ApiDocumentationV4', () => {
  let fixture: ComponentFixture<ApiDocumentationV4Component>;
  let harnessLoader: HarnessLoader;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4Component],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4Component);
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(async () => await init());

  it('should show empty state when no documentation for API', async () => {
    const emptyState = await harnessLoader.getHarness(ApiDocumentationV4EmptyStateHarness);
    expect(emptyState).toBeDefined();
  });
});
