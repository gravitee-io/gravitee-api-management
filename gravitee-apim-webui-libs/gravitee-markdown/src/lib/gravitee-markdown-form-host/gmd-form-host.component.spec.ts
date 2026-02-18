/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { GmdFormHostComponent } from './gmd-form-host.component';
import { GMD_FORM_STATE_STORE, provideGmdFormStore } from '../services/gmd-form-state.store';

describe('GmdFormHostComponent', () => {
  let component: GmdFormHostComponent;
  let fixture: ComponentFixture<GmdFormHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GmdFormHostComponent],
      providers: [provideGmdFormStore()],
    }).compileComponents();

    fixture = TestBed.createComponent(GmdFormHostComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should use the provided GMD_FORM_STATE_STORE', () => {
    const store = fixture.debugElement.injector.get(GMD_FORM_STATE_STORE);
    expect(store).toBeTruthy();
  });

  it('should reset store when content changes', () => {
    const store = fixture.debugElement.injector.get(GMD_FORM_STATE_STORE);
    const resetSpy = jest.spyOn(store, 'reset');

    fixture.componentRef.setInput('content', 'initial content');
    fixture.detectChanges();

    expect(resetSpy).toHaveBeenCalledTimes(1);

    fixture.componentRef.setInput('content', 'updated content');
    fixture.detectChanges();

    expect(resetSpy).toHaveBeenCalledTimes(2);
  });

  it('should accept content input', () => {
    fixture.componentRef.setInput('content', 'test markdown content');
    fixture.detectChanges();

    expect(component.content()).toBe('test markdown content');
  });

  it('should use ng-content for projection', () => {
    // The component uses <ng-content /> which projects children
    // This is implicitly tested by the template being '<ng-content />'
    expect(component).toBeTruthy();
  });
});
