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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { OpenApiEditorComponent } from './openapi-editor.component';
import { OpenApiEditorHarness } from './openapi-editor.harness';

@Component({
  template: ` <openapi-editor [formControl]="contentControl" /> `,
  standalone: true,
  imports: [OpenApiEditorComponent, ReactiveFormsModule],
})
class TestHostComponent {
  contentControl = new FormControl('openapi: 3.0.0\ninfo:\n  title: Test');
}

describe('OpenApiEditorComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should create and render', async () => {
    const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
    expect(openApiEditor).toBeTruthy();
  });

  it('should reflect value from form control', async () => {
    const openApiEditor = await loader.getHarness(OpenApiEditorHarness);
    expect(openApiEditor).toBeTruthy();
    expect(host.contentControl.value).toBe('openapi: 3.0.0\ninfo:\n  title: Test');
  });

  it('should accept updated value from parent via writeValue', async () => {
    host.contentControl.setValue('openapi: 3.0.0\ninfo:\n  title: Updated');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(host.contentControl.value).toBe('openapi: 3.0.0\ninfo:\n  title: Updated');
  });
});
