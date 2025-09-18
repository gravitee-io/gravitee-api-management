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
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ConfigureTestingGraviteeMarkdownEditor, GraviteeMarkdownEditorHarness, GraviteeMarkdownEditorModule } from './public-api';
import { GraviteeMarkdownViewerHarness } from '../gravitee-markdown-viewer/public-api';

@Component({
  selector: 'gmd-test-component',
  imports: [GraviteeMarkdownEditorModule, ReactiveFormsModule],
  template: `<gmd-editor [formControl]="editorControl" />`,
})
class TestComponent {
  public editorControl = new FormControl('');
}

describe('GraviteeMarkdownEditorComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let editorHarness: GraviteeMarkdownEditorHarness;
  let viewerHarness: GraviteeMarkdownViewerHarness;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent],
    }).compileComponents();

    // Swap the mock component for the monaco editor before each test
    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    editorHarness = await loader.getHarness(GraviteeMarkdownEditorHarness);
    viewerHarness = await loader.getHarness(GraviteeMarkdownViewerHarness);

    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should pass initial values to Monaco editor', async () => {
      expect(await editorHarness.getEditorValue()).toBe('');
      expect(await editorHarness.isEditorReadOnly()).toBe(false);
    });
  });

  describe('Value Flow from Form Control to Monaco Editor', () => {
    it('should pass value from writeValue to Monaco editor', async () => {
      const testValue = 'cats rule';
      await editorHarness.setEditorValue(testValue);

      expect(await editorHarness.getEditorValue()).toBe(testValue);
    });

    it('should pass value from editor to viewer', async () => {
      const testValue = '# Hello, World!';
      await editorHarness.setEditorValue(testValue);

      expect(await editorHarness.getEditorValue()).toBe(testValue);
      expect(await viewerHarness.getRenderedHtml()).toContain('<h1 id="hello-world">Hello, World!</h1>');
    });
  });

  describe('Disabled State Flow from Form Control to Monaco Editor', () => {
    it('should pass disabled state to Monaco editor as readOnly', async () => {
      component.editorControl.disable();
      expect(await editorHarness.isEditorReadOnly()).toBe(true);
    });
  });

  describe('Value Flow from Monaco Editor to Form Control', () => {
    it('should emit value changes from Monaco editor', async () => {
      const newValue = 'Updated content';
      await editorHarness.setEditorValue(newValue);

      expect(component.editorControl.value).toBe(newValue);
    });
  });
});
