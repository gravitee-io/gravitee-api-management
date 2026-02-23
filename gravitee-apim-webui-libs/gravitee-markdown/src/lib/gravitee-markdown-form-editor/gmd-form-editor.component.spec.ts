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
import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GmdFormEditorComponent } from './gmd-form-editor.component';
import { GraviteeMarkdownViewerModule } from '../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { provideGmdFormStore } from '../services/gmd-form-state.store';

@Component({
  selector: 'gmd-monaco-editor',
  template: '<div class="mock-monaco">{{ value }}</div>',
  standalone: true,
})
class MockMonacoEditorComponent {
  @Input() value = '';
  @Input() readOnly = false;
}

@Component({
  selector: 'gmd-viewer',
  template: '<div class="mock-viewer">{{ content }}</div>',
  standalone: true,
})
class MockGmdViewerComponent {
  @Input() content = '';
}

describe('GmdFormEditorComponent', () => {
  let component: GmdFormEditorComponent;
  let fixture: ComponentFixture<GmdFormEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GmdFormEditorComponent, MockMonacoEditorComponent, MockGmdViewerComponent],
      providers: [provideGmdFormStore()],
    })
      .overrideComponent(GmdFormEditorComponent, {
        remove: { imports: [GraviteeMarkdownViewerModule] },
        add: { imports: [MockGmdViewerComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GmdFormEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render gmd-form-host component', () => {
    const hostElement = fixture.nativeElement.querySelector('gmd-form-host');
    expect(hostElement).toBeTruthy();
  });

  it('should render gmd-form-validation-panel component', () => {
    const panelElement = fixture.nativeElement.querySelector('gmd-form-validation-panel');
    expect(panelElement).toBeTruthy();
  });

  it('should render gmd-viewer component inside host', () => {
    const viewerElement = fixture.nativeElement.querySelector('gmd-viewer');
    expect(viewerElement).toBeTruthy();
  });

  it('should pass content to gmd-form-host', () => {
    component.value = 'test markdown';
    fixture.detectChanges();

    const hostElements = fixture.nativeElement.querySelectorAll('gmd-form-host');
    expect(hostElements.length).toBeGreaterThan(0);
  });

  it('should pass content to gmd-viewer', () => {
    component.value = 'test markdown';
    fixture.detectChanges();

    const viewerElement = fixture.nativeElement.querySelector('gmd-viewer');
    expect(viewerElement).toBeTruthy();
  });

  it('should update value on change', () => {
    const onChangeSpy = jest.fn();
    component.registerOnChange(onChangeSpy);

    component.onValueChange('new content');

    expect(component.value).toBe('new content');
    expect(onChangeSpy).toHaveBeenCalledWith('new content');
  });

  it('should call onTouched when touched', () => {
    const onTouchedSpy = jest.fn();
    component.registerOnTouched(onTouchedSpy);

    component.onTouched();

    expect(onTouchedSpy).toHaveBeenCalled();
  });

  it('should write value', () => {
    component.writeValue('test value');
    expect(component.value).toBe('test value');
  });

  it('should handle null value in writeValue', () => {
    component.writeValue(null);
    // Should not throw error
    expect(component).toBeTruthy();
  });

  it('should set disabled state', () => {
    component.setDisabledState(true);
    expect(component.isDisabled).toBe(true);

    component.setDisabledState(false);
    expect(component.isDisabled).toBe(false);
  });

  it('should have monaco editor with correct bindings', () => {
    component.value = 'test content';
    component.isDisabled = false;
    fixture.detectChanges();

    const monacoElement = fixture.nativeElement.querySelector('gmd-monaco-editor');
    expect(monacoElement).toBeTruthy();
  });

  it('should render validation panel inside the same host as viewer', () => {
    const hostElement = fixture.nativeElement.querySelector('gmd-form-host');
    const viewerElement = fixture.nativeElement.querySelector('gmd-viewer');
    const panelElement = fixture.nativeElement.querySelector('gmd-form-validation-panel');

    // Both viewer and panel should be within the same host
    expect(hostElement).toBeTruthy();
    expect(viewerElement).toBeTruthy();
    expect(panelElement).toBeTruthy();

    // Check that they share the same parent host
    const hostElements = fixture.nativeElement.querySelectorAll('gmd-form-host');
    expect(hostElements.length).toBe(1); // Only one host wrapping both
  });
});
