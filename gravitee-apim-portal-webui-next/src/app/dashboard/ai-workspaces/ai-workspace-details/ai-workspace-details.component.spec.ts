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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import AiWorkspaceDetailsComponent from './ai-workspace-details.component';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('AiWorkspaceDetailsComponent', () => {
  let fixture: ComponentFixture<AiWorkspaceDetailsComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiWorkspaceDetailsComponent, AppTestingModule],
      providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ aiWorkspaceId: 'ws-1' })) } }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiWorkspaceDetailsComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should load details and consumption independently', fakeAsync(() => {
    fixture.detectChanges();
    const details = http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`);
    const consumption = http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`);

    details.flush({
      id: 'ws-1',
      name: 'Alpha',
      description: 'A workspace',
      budget: { amount: 5, period: 'DAY' },
      endpointUrl: '/llm-proxy',
      key: { value: 'live-key', status: 'ACTIVE', createdAt: '2026-01-02T03:04:05Z' },
      models: [{ name: 'gpt-4o', inputPrice: 2.5, outputPrice: 10 }],
    });
    consumption.flush({ tokens: 12, requests: 3, cost: 1.5, from: '2026-01-01T00:00:00Z', to: '2026-01-31T00:00:00Z' });
    tick();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Alpha');
    expect(text).toContain('5 / DAY');
    expect(text).toContain('/llm-proxy');
    expect(text).toContain('live-key');
    expect(text).toContain('ACTIVE');
    expect(text).toContain('gpt-4o');
    expect(text).toContain('12');
    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-consumption"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Revoke');
    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-revoke"]')).toBeNull();

    const breadcrumbs = TestBed.inject(BreadcrumbService).breadcrumbs();
    expect(breadcrumbs[0]).toEqual(expect.objectContaining({ id: 'ai-workspaces', url: '/dashboard/ai-workspaces' }));
    expect(breadcrumbs[1]).toEqual(expect.objectContaining({ label: 'Alpha' }));
  }));

  it('should still render details when consumption fails', fakeAsync(() => {
    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`).flush({
      id: 'ws-1',
      name: 'Alpha',
      description: 'A workspace',
      budget: { amount: 5, period: 'DAY' },
      endpointUrl: '/llm-proxy',
      key: { value: 'live-key', status: 'ACTIVE', createdAt: '2026-01-02T03:04:05Z' },
      models: [{ name: 'gpt-4o', inputPrice: 2.5, outputPrice: 10 }],
    });
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`).flush('unavailable', { status: 500, statusText: 'Server Error' });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Alpha');
    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-consumption-error"]')).toBeTruthy();
  }));

  it('should show empty key, models and endpoint states', fakeAsync(() => {
    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`).flush({
      id: 'ws-1',
      name: 'Alpha',
      description: 'A workspace',
    });
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`).flush({
      tokens: 0,
      requests: 0,
      cost: 0,
      from: '2026-01-01T00:00:00Z',
      to: '2026-01-31T00:00:00Z',
    });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-key-empty"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-models-empty"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('No endpoint URL is available.');
  }));

  it('should copy the endpoint URL', fakeAsync(() => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });

    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`).flush({
      id: 'ws-1',
      name: 'Alpha',
      endpointUrl: '/llm-proxy',
    });
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`).flush({ tokens: 0, requests: 0, cost: 0 });
    tick();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="copy-endpoint"]').click();
    tick();
    fixture.detectChanges();

    expect(writeText).toHaveBeenCalledWith('/llm-proxy');
    expect(fixture.nativeElement.querySelector('[data-testid="ai-workspace-endpoint-copied"]')).toBeTruthy();
  }));

  it('should page the models table', fakeAsync(() => {
    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`).flush({
      id: 'ws-1',
      name: 'Alpha',
      models: Array.from({ length: 11 }, (_, index) => ({ name: `model-${index}`, inputPrice: 1, outputPrice: 2 })),
    });
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`).flush({ tokens: 0, requests: 0, cost: 0 });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('model-0');
    expect(fixture.nativeElement.textContent).not.toContain('model-10');

    fixture.nativeElement.querySelector('[data-testid="ai-workspace-models-next"]').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('model-10');
    expect(fixture.nativeElement.textContent).not.toContain('model-0');
  }));
});
