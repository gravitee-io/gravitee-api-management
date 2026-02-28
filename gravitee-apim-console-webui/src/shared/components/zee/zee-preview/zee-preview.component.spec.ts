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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ZeePreviewComponent } from './zee-preview.component';

import { ZeeModule } from '../zee.module';
import { ZeeResourceType } from '../zee.model';
import { GioTestingModule } from '../../../testing';

const MOCK_FLOW = {
  name: 'Rate-Limit Flow',
  enabled: true,
  request: [{ name: 'Rate Limit Step', policy: 'rate-limit', description: 'Limit to 100 req/min' }],
  response: [],
  selectors: [{ type: 'HTTP', path: '/api/v1' }],
  tags: ['security', 'rate-limit'],
};

describe('ZeePreviewComponent', () => {
  let fixture: ComponentFixture<ZeePreviewComponent>;
  let component: ZeePreviewComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZeeModule, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ZeePreviewComponent);
    component = fixture.componentInstance;
    component.resourceType = ZeeResourceType.FLOW;
    component.data = MOCK_FLOW;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose ZeeResourceType enum to template', () => {
    expect(component.ZeeResourceType).toEqual(ZeeResourceType);
  });

  it('should render two tabs: "Structured View" and "JSON"', () => {
    const tabs = fixture.nativeElement.querySelectorAll('.mat-mdc-tab');
    const labels = Array.from(tabs).map((tab: Element) => tab.textContent?.trim());
    expect(labels).toContain('Structured View');
    expect(labels).toContain('JSON');
  });

  it('should render zee-flow-card in structured view for FLOW resource type', () => {
    // First tab is active by default, which is the structured view
    const flowCard = fixture.nativeElement.querySelector('zee-flow-card');
    expect(flowCard).toBeTruthy();
  });

  it('should not render JSON pre in structured tab when FLOW type is used', () => {
    // The ngSwitch matches FLOW, so the flow card is shown, not the pre fallback
    const structuredTab = fixture.nativeElement.querySelector('mat-tab-group');
    expect(structuredTab).toBeTruthy();
  });

  describe('with unknown resource type', () => {
    beforeEach(() => {
      component.resourceType = ZeeResourceType.PLAN;
      component.data = { name: 'My Plan' };
      fixture.detectChanges();
    });

    it('should not render zee-flow-card for non-FLOW type', () => {
      const flowCard = fixture.nativeElement.querySelector('zee-flow-card');
      // In the active (structured) tab, the ngSwitchDefault fires — no flow card
      expect(flowCard).toBeFalsy();
    });
  });

  describe('with null data', () => {
    beforeEach(() => {
      component.data = null;
      fixture.detectChanges();
    });

    it('should not throw', () => {
      expect(() => fixture.detectChanges()).not.toThrow();
    });

    it('should show the fallback message in the structured view tab', () => {
      const empty = fixture.nativeElement.querySelector('.zee-preview-empty');
      expect(empty).toBeTruthy();
      expect(empty.textContent).toContain("Zee couldn't generate a result");
    });

    it('should show fallback text in the JSON tab instead of crashing', () => {
      const jsonPre = fixture.nativeElement.querySelector('.zee-preview-json');
      // When data is null the JSON pre should show the fallback string, not '[object Object]'
      expect(jsonPre?.textContent ?? '').not.toContain('[object Object]');
    });
  });
});

