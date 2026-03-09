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
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SubscriptionMetadataViewerComponent } from './subscription-metadata-viewer.component';

@Component({
  template: `<subscription-metadata-viewer [metadata]="metadata()" />`,
  standalone: true,
  imports: [SubscriptionMetadataViewerComponent],
})
class TestHostComponent {
  metadata = input<Record<string, string> | null | undefined>();
}

describe('SubscriptionMetadataViewerComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  const getEditor = () => fixture.nativeElement.querySelector('.metadata-editor');
  const getText = () => (fixture.nativeElement.textContent as string).trim();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should show dash when metadata is undefined', () => {
    expect(getEditor()).toBeNull();
    expect(getText()).toBe('-');
  });

  it('should show dash when metadata is null', async () => {
    fixture.componentRef.setInput('metadata', null);
    fixture.detectChanges();

    expect(getEditor()).toBeNull();
    expect(getText()).toBe('-');
  });

  it('should show dash when metadata is an empty object', () => {
    fixture.componentRef.setInput('metadata', {});
    fixture.detectChanges();

    expect(getEditor()).toBeNull();
    expect(getText()).toBe('-');
  });

  it('should show monaco editor when metadata has values', () => {
    fixture.componentRef.setInput('metadata', { key1: 'value1', key2: 'value2' });
    fixture.detectChanges();

    expect(getEditor()).not.toBeNull();
  });

  it('should update when metadata input changes', () => {
    fixture.componentRef.setInput('metadata', { key: 'value' });
    fixture.detectChanges();
    expect(getEditor()).not.toBeNull();

    fixture.componentRef.setInput('metadata', null);
    fixture.detectChanges();
    expect(getEditor()).toBeNull();
    expect(getText()).toBe('-');
  });
});
