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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { EmptyStateComponent } from './empty-state.component';
import { EmptyStateComponentHarness } from './empty-state.component.harness';

@Component({
  template: ` <empty-state [iconName]="icon" [title]="title" [message]="message" /> `,
  standalone: true,
  imports: [EmptyStateComponent],
})
class TestHostComponent {
  public icon = 'test-icon';
  public title = 'Test Title';
  public message = 'Test Message';
}

describe('EmptyStateComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let harness: EmptyStateComponentHarness;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, EmptyStateComponent, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(EmptyStateComponentHarness);
    fixture.detectChanges(); // Trigger initial data binding
  });

  it('should create and be found by the harness', () => {
    expect(harness).toBeTruthy();
  });

  it('should display the initial title, message, and icon provided via inputs', async () => {
    expect(await harness.getTitle()).toBe('Test Title');
    expect(await harness.getMessage()).toBe('Test Message');
  });

  it('should update the display when inputs change', async () => {
    component.title = 'New Title';
    component.message = 'New Message';
    component.icon = 'new-icon';
    fixture.detectChanges(); // Trigger change detection

    expect(await harness.getTitle()).toBe('New Title');
    expect(await harness.getMessage()).toBe('New Message');
  });
});

describe('EmptyStateComponent (Defaults)', () => {
  let fixture: ComponentFixture<EmptyTestHostComponent>;
  let harness: EmptyStateComponentHarness;

  @Component({
    template: `<empty-state />`,
    standalone: true,
    imports: [EmptyStateComponent],
  })
  class EmptyTestHostComponent {}

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyTestHostComponent, EmptyStateComponent, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyTestHostComponent);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(EmptyStateComponentHarness);
    fixture.detectChanges();
  });

  it('should display empty strings and no icon when no inputs are provided', async () => {
    expect(await harness.getTitle()).toBe('');
    expect(await harness.getMessage()).toBe('');
  });
});
