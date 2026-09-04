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
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { SidePanelComponent } from './side-panel.component';
import { SidePanelComponentHarness } from './side-panel.harness';
import { AppTestingModule } from '../../testing/app-testing.module';

@Component({
  imports: [SidePanelComponent],
  template: `<app-side-panel panelTestId="test-panel" ariaLabel="Test panel" (closed)="closeCount.set(closeCount() + 1)">
    <p class="projected-content">content</p>
  </app-side-panel>`,
})
class HostComponent {
  closeCount = signal(0);
}

describe('SidePanelComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let harness: SidePanelComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent, MatIconTestingModule, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SidePanelComponentHarness);
  });

  it('projects its content', () => {
    expect(fixture.nativeElement.querySelector('.projected-content')).not.toBeNull();
  });

  it('carries the test id and the accessible label it was given', async () => {
    expect(await harness.getPanelTestId()).toBe('test-panel');
    expect(await harness.getPanelAriaLabel()).toBe('Test panel');
  });

  it('asks to close when the backdrop is clicked', async () => {
    await harness.clickBackdrop();

    expect(fixture.componentInstance.closeCount()).toBe(1);
  });

  it('asks to close when the close button is clicked', async () => {
    await harness.clickClose();

    expect(fixture.componentInstance.closeCount()).toBe(1);
  });

  it('asks to close on escape', async () => {
    await harness.pressEscape();

    expect(fixture.componentInstance.closeCount()).toBe(1);
  });
});
