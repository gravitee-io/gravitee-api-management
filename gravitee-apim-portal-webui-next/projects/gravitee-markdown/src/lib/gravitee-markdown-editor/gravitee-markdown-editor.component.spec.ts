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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';

describe('GraviteeMarkdownEditorComponent', () => {
  let component: GraviteeMarkdownEditorComponent;
  let fixture: ComponentFixture<GraviteeMarkdownEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeMarkdownEditorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeMarkdownEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have content property', () => {
    expect(component.content).toBeDefined();
  });

  it('should have error property', () => {
    expect(component.error).toBeDefined();
  });

  it('should have contentChange output', () => {
    expect(component.contentChange).toBeDefined();
  });

  it('should have errorChange output', () => {
    expect(component.errorChange).toBeDefined();
  });

  it('should have darkTheme input', () => {
    expect(component.darkTheme).toBeDefined();
  });

  it('should have highlightTheme input', () => {
    expect(component.highlightTheme).toBeDefined();
  });
});
