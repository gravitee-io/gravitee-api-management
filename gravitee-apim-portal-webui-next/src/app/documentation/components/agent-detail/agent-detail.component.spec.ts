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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { AgentDetailComponent } from './agent-detail.component';
import { AgentCatalogItem } from '../../../../entities/agent/agent-catalog-info';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

function fakeAgent(overrides?: Partial<AgentCatalogItem>): AgentCatalogItem {
  return {
    id: 'agent-1',
    kind: 'agent',
    entityId: 'agent.test-agent',
    sourceId: 'src-1',
    sourceKind: 'manual',
    environmentId: 'DEFAULT',
    organizationId: 'DEFAULT',
    creationDate: '2026-04-20T10:00:00Z',
    updateDate: '2026-04-22T11:00:00Z',
    definition: {
      name: 'Test Agent',
      description: 'A test agent for unit tests.',
      url: 'https://agents.test/a2a',
      provider: { organization: 'Test Corp', url: 'https://test.com' },
      version: '1.2.0',
      documentationUrl: 'https://test.com/docs',
      capabilities: { streaming: true, pushNotifications: false, stateTransitionHistory: true },
      defaultInputModes: ['text'],
      defaultOutputModes: ['text', 'text/markdown'],
      skills: [
        {
          id: 'skill-1',
          name: 'Lookup',
          description: 'Find records.',
          tags: ['search', 'crm'],
          examples: ['Find user by email'],
          inputModes: ['text'],
          outputModes: ['text/markdown'],
        },
        {
          id: 'skill-2',
          name: 'Create',
          description: 'Create a record.',
        },
      ],
    },
    ...overrides,
  };
}

@Component({
  template: '<app-agent-detail [agentId]="agentId" [title]="title" [orgId]="orgId" [envId]="envId" />',
  imports: [AgentDetailComponent],
})
class TestHostComponent {
  agentId: string | undefined = 'agent-1';
  title = 'Test Agent';
  orgId = 'DEFAULT';
  envId = 'DEFAULT';
}

describe('AgentDetailComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let http: HttpTestingController;

  async function setup(agent: AgentCatalogItem | null = fakeAgent()) {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, MatIconTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    http = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    const req = http.expectOne(r => r.url.endsWith('/agents/agent-1'));
    req.flush(agent);

    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    http.verify();
  });

  it('should display the agent name and description', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.agent-detail__hero__name')?.textContent?.trim()).toBe('Test Agent');
    expect(el.querySelector('.agent-detail__hero__description')?.textContent?.trim()).toBe('A test agent for unit tests.');
  });

  it('should display the version badge', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const badges = el.querySelectorAll('.agent-detail__badge--outline');
    const versionBadge = Array.from(badges).find(b => b.textContent?.trim() === '1.2.0');
    expect(versionBadge).toBeTruthy();
  });

  it('should display the provider badge', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const badges = el.querySelectorAll('.agent-detail__badge--secondary');
    const providerBadge = Array.from(badges).find(b => b.textContent?.trim() === 'Test Corp');
    expect(providerBadge).toBeTruthy();
  });

  it('should display enabled capabilities', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const chips = el.querySelectorAll('.agent-detail__capability-chip');
    const labels = Array.from(chips).map(c => c.textContent?.trim());
    expect(labels.some(l => l?.includes('Streaming'))).toBe(true);
    expect(labels.some(l => l?.includes('State History'))).toBe(true);
    expect(labels.some(l => l?.includes('Push Notifications'))).toBe(false);
  });

  it('should display provider info with link', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const orgValue = el.querySelectorAll('.agent-detail__definition-value')[0];
    expect(orgValue?.textContent?.trim()).toBe('Test Corp');
    const link = el.querySelector('.agent-detail__link') as HTMLAnchorElement;
    expect(link?.href).toBe('https://test.com/');
  });

  it('should display I/O modes', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const modeChips = el.querySelectorAll('.agent-detail__mode-chip');
    const modes = Array.from(modeChips).map(c => c.textContent?.trim());
    expect(modes).toContain('text');
    expect(modes).toContain('text/markdown');
  });

  it('should display skills', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const skillCards = el.querySelectorAll('.agent-detail__skill-card');
    expect(skillCards.length).toBe(2);

    const firstSkill = skillCards[0];
    expect(firstSkill.querySelector('.next-gen-body-strong')?.textContent?.trim()).toBe('Lookup');
    expect(firstSkill.querySelector('.agent-detail__skill-description')?.textContent?.trim()).toBe('Find records.');

    const tags = firstSkill.querySelectorAll('.agent-detail__tag-chip');
    expect(Array.from(tags).map(t => t.textContent?.trim())).toEqual(['search', 'crm']);
  });

  it('should display documentation link when present', async () => {
    await setup();
    const el = fixture.nativeElement as HTMLElement;
    const docLink = el.querySelector('a[href="https://test.com/docs"]') as HTMLAnchorElement;
    expect(docLink).toBeTruthy();
    expect(docLink.target).toBe('_blank');
  });

  it('should not display documentation link when absent', async () => {
    const agent = fakeAgent();
    agent.definition.documentationUrl = undefined;
    await setup(agent);
    const el = fixture.nativeElement as HTMLElement;
    const docLinks = el.querySelectorAll('a[target="_blank"]');
    const docsLink = Array.from(docLinks).find(l => l.textContent?.includes('Documentation'));
    expect(docsLink).toBeFalsy();
  });

  it('should hide capabilities section when none are enabled', async () => {
    const agent = fakeAgent();
    agent.definition.capabilities = { streaming: false, pushNotifications: false, stateTransitionHistory: false };
    await setup(agent);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.agent-detail__capability-chip')).toBeFalsy();
  });

  it('should look up the agent by catalog id when agentId is set', async () => {
    await setup();
    http.expectNone(r => r.url.includes('/agents') && r.params.has('q'));
  });

  it('should fall back to name search when agentId is absent', async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, MatIconTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.agentId = undefined;
    http = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    const req = http.expectOne(r => r.url.includes('/agents') && r.params.get('q') === 'Test Agent');
    req.flush({
      data: [fakeAgent()],
      pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 },
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.agent-detail__hero__name')?.textContent?.trim()).toBe('Test Agent');
  });

  it('should show fallback details when the catalog request fails', async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, MatIconTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    http = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    const req = http.expectOne(r => r.url.endsWith('/agents/agent-1'));
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.agent-detail__hero__name')?.textContent?.trim()).toBe('Test Agent');
  });
});
