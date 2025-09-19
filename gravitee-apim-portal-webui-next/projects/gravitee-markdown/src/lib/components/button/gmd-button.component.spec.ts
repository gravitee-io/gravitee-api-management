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
import { GmdButtonComponent } from './gmd-button.component';

describe('ButtonComponent', () => {
  let component: GmdButtonComponent;
  let fixture: ComponentFixture<GmdButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GmdButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GmdButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default to filled appearance', () => {
    expect(component.appearance).toBe('filled');
  });

  it('should set appearance to outlined', () => {
    component.appearance = 'outlined';
    expect(component.appearance).toBe('outlined');
  });

  it('should set appearance to text', () => {
    component.appearance = 'text';
    expect(component.appearance).toBe('text');
  });
});

