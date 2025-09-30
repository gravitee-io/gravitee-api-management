/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdButtonComponent } from './gmd-button.component';
import { GmdButtonComponentHarness } from './gmd-button.component.harness';

@Component({
  template: `<gmd-button [appearance]="appearance()" [link]="link()" [target]="target()">{{ text() }}</gmd-button>`,
  standalone: true,
  imports: [GmdButtonComponent],
})
class TestHostComponent {
  text = input<string>();
  appearance = input<'filled' | 'outlined' | 'text'>('filled');
  link = input<string>();
  target = input<string>();
}

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: GmdButtonComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdButtonComponentHarness);
    fixture.detectChanges();
  });

  it('should get button text', async () => {
    fixture.componentRef.setInput('link', '/test');
    fixture.componentRef.setInput('text', 'Click Me');
    fixture.detectChanges();

    const text = await harness.getText();
    expect(text).toBe('Click Me');
  });

  it('should have default values', async () => {
    expect(await harness.getAppearance()).toBe('filled');
    expect(await harness.getHref()).toBe('/');
    expect(await harness.getTarget()).toBe('_self');
    expect(await harness.getText()).toBe(''); // Empty since no content is projected
  });

  it('should get appearance for different styles', async () => {
    // Test filled (default)
    let appearance = await harness.getAppearance();
    expect(appearance).toBe('filled');

    // Test outlined
    fixture.componentRef.setInput('appearance', 'outlined');
    fixture.detectChanges();
    appearance = await harness.getAppearance();
    expect(appearance).toBe('outlined');

    // Test text
    fixture.componentRef.setInput('appearance', 'text');
    fixture.detectChanges();
    appearance = await harness.getAppearance();
    expect(appearance).toBe('text');
  });

  it('should show filled button if input is an invalid value', async () => {
    fixture.componentRef.setInput('appearance', 'kitty-cat');
    fixture.detectChanges();
    const appearance = await harness.getAppearance();
    expect(appearance).toBe('filled');
  });

  it('should get href and target attributes through harness', async () => {
    // Test internal link
    fixture.componentRef.setInput('link', '/internal');
    fixture.componentRef.setInput('target', '_self');
    fixture.detectChanges();

    expect(await harness.getHref()).toBe('/internal');
    expect(await harness.getTarget()).toBe('_self');

    // Test external link
    fixture.componentRef.setInput('link', 'https://external.com');
    fixture.componentRef.setInput('target', '_blank');
    fixture.detectChanges();

    expect(await harness.getHref()).toBe('https://external.com');
    expect(await harness.getTarget()).toBe('_blank');
  });
});
