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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { FlowCardComponent } from './flow-card.component';

import { ZeeModule } from '../zee.module';
import { GioTestingModule } from '../../../testing';

const FULL_FLOW = {
  name: 'Bot-blocker Flow',
  enabled: true,
  request: [
    { name: 'Bot Detection Step', policy: 'bot-detection', description: 'Blocks bot traffic' },
    { name: 'Rate Limit Step', policy: 'rate-limit', description: 'Limits to 100 req/min' },
  ],
  response: [{ name: 'Cache Step', policy: 'cache', description: 'Cache responses' }],
  subscribe: [],
  publish: [],
  selectors: [
    { type: 'HTTP', path: '/api/v1' },
    { type: 'CHANNEL', channel: 'my-topic' },
  ],
  tags: ['security', 'rate-limit', 'bot'],
};

describe('FlowCardComponent', () => {
  let fixture: ComponentFixture<FlowCardComponent>;
  let component: FlowCardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZeeModule, GioTestingModule, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FlowCardComponent);
    component = fixture.componentInstance;
    component.flow = FULL_FLOW;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the flow name', () => {
    const title = fixture.nativeElement.querySelector('mat-card-title');
    expect(title.textContent).toContain('Bot-blocker Flow');
  });

  it('should render "Enabled" chip for enabled flow', () => {
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.textContent.trim()).toContain('Enabled');
  });

  it('should render "Disabled" chip for disabled flow', () => {
    component.flow = { ...FULL_FLOW, enabled: false };
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('mat-chip');
    expect(chip.textContent.trim()).toContain('Disabled');
  });

  it('should render request steps', () => {
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Request Steps');
    expect(content).toContain('Bot Detection Step');
    expect(content).toContain('bot-detection');
    expect(content).toContain('Blocks bot traffic');
    expect(content).toContain('Rate Limit Step');
  });

  it('should render response steps', () => {
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Response Steps');
    expect(content).toContain('Cache Step');
    expect(content).toContain('cache');
  });

  it('should render selectors', () => {
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Selectors');
    expect(content).toContain('HTTP');
    expect(content).toContain('/api/v1');
    expect(content).toContain('CHANNEL');
    expect(content).toContain('my-topic');
  });

  it('should render tags', () => {
    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Tags');
    expect(content).toContain('security');
    expect(content).toContain('rate-limit');
    expect(content).toContain('bot');
  });

  it('should use the step section key definitions', () => {
    expect(component.stepSections.map((s) => s.key)).toEqual(['request', 'response', 'subscribe', 'publish']);
  });

  describe('with minimal flow (no optionals)', () => {
    beforeEach(() => {
      component.flow = { name: 'Bare Flow', enabled: true };
      fixture.detectChanges();
    });

    it('should not render sections headings when arrays are empty', () => {
      const content = fixture.nativeElement.textContent;
      expect(content).not.toContain('Request Steps');
      expect(content).not.toContain('Selectors');
      expect(content).not.toContain('Tags');
    });

    it('should show empty state message', () => {
      const content = fixture.nativeElement.textContent;
      expect(content).toContain('No flow details to display');
    });
  });

  describe('with undefined flow name', () => {
    beforeEach(() => {
      component.flow = { enabled: true };
      fixture.detectChanges();
    });

    it('should show fallback name', () => {
      const title = fixture.nativeElement.querySelector('mat-card-title');
      expect(title.textContent).toContain('Unnamed Flow');
    });
  });
});
