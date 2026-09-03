/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';

import { ApiCardComponent } from './api-card.component';
import { ApiCardHarness } from './api-card.harness';
import { stubOverflowLabelsLayout } from '../../testing/overflow-labels-layout';

describe('CardComponent', () => {
  let component: ApiCardComponent;
  let fixture: ComponentFixture<ApiCardComponent>;
  let harness: ApiCardHarness;
  const api = {
    title: 'Test title',
    version: 'v.1',
    content:
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    id: '1',
  };
  const init = async (inputs: Record<string, unknown> = {}) => {
    fixture = TestBed.createComponent(ApiCardComponent);
    component = fixture.componentInstance;
    component.apiId = api.id;
    component.title = api.title;
    component.version = api.version;
    component.content = api.content;
    fixture.componentRef.setInput('typeLabel', 'API');
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));

    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiCardHarness);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiCardComponent, RouterModule.forRoot([])],
    }).compileComponents();
    await init();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display data in card', async () => {
    expect(await harness.getTitle()).toEqual('Test title');
    expect(await harness.getVersion()).toEqual('v.1');
    expect(await harness.getDescription()).toContain(
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    );
    expect(await harness.getType()).toBe('API');
  });

  it('should emit the API id when selected', async () => {
    const selected = jest.fn();
    component.cardSelect.subscribe(selected);

    await harness.select();

    expect(selected).toHaveBeenCalledWith(api.id);
  });

  it('should show no access token until the access state is known', async () => {
    expect(await harness.getAccess()).toBeNull();

    fixture.componentRef.setInput('access', 'NO_KEY');
    fixture.componentRef.setInput('accessLabel', 'No key needed');
    fixture.detectChanges();

    expect(await harness.getAccess()).toBe('No key needed');
  });

  it('should show the capabilities it was given', async () => {
    fixture.destroy();
    stubOverflowLabelsLayout({ containerWidth: 500 });
    await init({ capabilities: ['triage_alert', 'draft_postmortem'] });

    expect(await harness.getCapabilities()).toEqual(['triage_alert', 'draft_postmortem']);
    expect(await harness.getCapabilityOverflow()).toBeNull();
  });

  it('should fold the capabilities that do not fit into a counter', async () => {
    fixture.destroy();
    stubOverflowLabelsLayout({ containerWidth: 260 });
    await init({ capabilities: ['one', 'two', 'three', 'four', 'five'] });

    expect(await harness.getCapabilities()).toEqual(['one', 'two']);
    expect(await harness.getCapabilityOverflow()).toBe('+3');
  });

  it('should keep the details closed until asked, then show them', async () => {
    fixture.componentRef.setInput('endpoint', 'https://gw.example.com/incident-commander');
    fixture.componentRef.setInput('protocol', 'A2A');
    fixture.componentRef.setInput('published', '3 months ago');
    fixture.detectChanges();

    expect(await harness.isExpanded()).toBe(false);

    await harness.toggleDetails();

    expect(await harness.isExpanded()).toBe(true);
    expect(await harness.getPublished()).toBe('3 months ago');
  });

  it('should not select the card when the details are toggled', async () => {
    const selected = jest.fn();
    component.cardSelect.subscribe(selected);

    await harness.toggleDetails();

    expect(selected).not.toHaveBeenCalled();
  });

  it.each(['Enter', ' '])('should leave the card alone when %s bubbles out of a control inside it', key => {
    const selected = jest.fn();
    component.cardSelect.subscribe(selected);

    fixture.nativeElement
      .querySelector('[data-testid="api-card-more"]')
      .dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));

    expect(selected).not.toHaveBeenCalled();
  });

  it('should keep the details out of the page until they are asked for', async () => {
    fixture.componentRef.setInput('endpoint', 'https://gw.example.com/incident-commander');
    fixture.detectChanges();

    expect(await harness.hasDetailsContent()).toBe(false);

    await harness.toggleDetails();

    expect(await harness.hasDetailsContent()).toBe(true);
    expect(await harness.getEndpoint()).toBe('https://gw.example.com/incident-commander');

    await harness.toggleDetails();

    expect(await harness.hasDetailsContent()).toBe(false);
  });

  it('should show what the agent can do, and how to reach it', async () => {
    fixture.componentRef.setInput('skills', [{ name: 'triage_alert', description: 'Reads an alert.' }]);
    fixture.componentRef.setInput('endpoint', 'https://gw.example.com/incident-commander');
    fixture.componentRef.setInput('protocol', 'A2A');
    fixture.componentRef.setInput('plans', 'Keyless · API Key');
    fixture.detectChanges();
    await harness.toggleDetails();

    expect(await harness.getSkills()).toEqual([['triage_alert', 'Reads an alert.']]);
    expect(await harness.getFacts()).toEqual(
      expect.arrayContaining([
        ['Gateway endpoint', 'https://gw.example.com/incident-commander'],
        ['Plans', 'Keyless · API Key'],
        ['Protocol', 'A2A'],
      ]),
    );
  });

  it.each([
    ['subscribe', 'subscribeSelect'],
    ['documentation', 'documentationSelect'],
  ])('should emit %s from the details', async (action, output) => {
    const emitted = jest.fn();
    (component as unknown as Record<string, { subscribe: (fn: jest.Mock) => void }>)[output].subscribe(emitted);
    await harness.toggleDetails();

    await harness.clickDetailAction(action as 'subscribe' | 'documentation');

    expect(emitted).toHaveBeenCalledWith('1');
  });
});
