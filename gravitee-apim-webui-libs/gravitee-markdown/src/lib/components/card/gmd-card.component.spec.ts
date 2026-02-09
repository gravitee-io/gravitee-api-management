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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdCardHarness } from './gmd-card.harness';
import { GmdCardModule } from './gmd-card.module';
import { GmdMdComponent } from '../block/gmd-md.component';
import { GmdMdHarness } from '../block/gmd-md.harness';

@Component({
  selector: 'gmd-test-component',
  imports: [GmdCardModule, GmdMdComponent],
  template: `
    <gmd-card [backgroundColor]="backgroundColor" [textColor]="textColor">
      <gmd-card-title>{{ title }}</gmd-card-title>
      <gmd-card-subtitle>{{ subtitle }}</gmd-card-subtitle>
      <gmd-md>{{ mdContent }}</gmd-md>
    </gmd-card>
  `,
})
class TestComponent {
  public backgroundColor: string = '';
  public textColor: string = '';
  public title = '';
  public subtitle = '';
  public mdContent = '';
}

describe('GmdCardComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let harness: GmdCardHarness;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GmdCardHarness);
    fixture.detectChanges();
  });

  it('should create', async () => {
    expect(component).toBeTruthy();
    expect(harness).toBeTruthy();
  });

  it('should display title, subtitle and content', async () => {
    component.title = 'Test Title';
    component.subtitle = 'Test Subtitle';
    component.mdContent = 'Test block content';
    fixture.detectChanges();

    expect(await harness.getCardTitleText()).toBe('Test Title');
    expect(await harness.getCardSubtitleText()).toBe('Test Subtitle');
    expect(await getCardContent()).toContain('Test block content');
  });

  it('should apply background color when provided', async () => {
    component.backgroundColor = '#ff0000';
    component.textColor = '#00ff00';
    fixture.detectChanges();

    expect(await harness.getBackgroundColor()).toBe('#ff0000');
    expect(await harness.getTextColor()).toBe('#00ff00');
  });

  describe('Input validation and edge cases', () => {
    it('should handle long text content', async () => {
      component.title = 'This is a very long title that might wrap to multiple lines';
      component.subtitle = 'This is a very long subtitle that might also wrap to multiple lines';
      component.mdContent =
        'This is a very long block content that contains multiple sentences and might wrap to multiple lines in the card component.';
      fixture.detectChanges();

      expect(await harness.getCardTitleText()).toBe('This is a very long title that might wrap to multiple lines');
      expect(await harness.getCardSubtitleText()).toBe('This is a very long subtitle that might also wrap to multiple lines');
      expect(await getCardContent()).toContain('This is a very long block content');
    });

    it('should handle special characters in content', async () => {
      component.title = 'Title with special chars: @#$%^&*()';
      component.subtitle = 'Subtitle with unicode: 🚀✨🎉';
      component.mdContent = 'Block with HTML: <script>alert("test")</script>';
      fixture.detectChanges();

      expect(await harness.getCardTitleText()).toBe('Title with special chars: @#$%^&*()');
      expect(await harness.getCardSubtitleText()).toBe('Subtitle with unicode: 🚀✨🎉');
      expect(await getCardContent()).toContain('Block with HTML: <script>alert("test")</script>');
    });
  });

  describe('Complete card with all elements', () => {
    it('should render complete card with title, subtitle, and content', async () => {
      component.title = 'Complete Card';
      component.subtitle = 'With all elements';
      component.mdContent = 'This is the card content';
      component.backgroundColor = '#f0f0f0';
      component.textColor = '#333333';
      fixture.detectChanges();

      await fixture.whenStable();

      expect(await harness.getCardTitleText()).toBe('Complete Card');
      expect(await harness.getCardSubtitleText()).toBe('With all elements');
      expect(await harness.getBackgroundColor()).toBe('#f0f0f0');
      expect(await harness.getTextColor()).toBe('#333333');
      expect(await getCardContent()).toContain('This is the card content');
    });

    it('should handle dynamic content changes', async () => {
      component.title = 'Initial Title';
      component.subtitle = 'Initial Subtitle';
      component.mdContent = 'Initial Content';
      fixture.detectChanges();

      expect(await harness.getCardTitleText()).toBe('Initial Title');
      expect(await harness.getCardSubtitleText()).toBe('Initial Subtitle');
      expect(await getCardContent()).toContain('Initial Content');

      // Change the content
      component.title = 'Updated Title';
      component.subtitle = 'Updated Subtitle';
      component.mdContent = 'Updated Content';
      fixture.detectChanges();

      expect(await harness.getCardTitleText()).toBe('Updated Title');
      expect(await harness.getCardSubtitleText()).toBe('Updated Subtitle');
      expect(await getCardContent()).toContain('Updated Content');
    });
  });

  async function getCardContent(): Promise<string> {
    const mdBlock = await loader.getHarness(GmdMdHarness);
    return mdBlock.getContent();
  }
});
