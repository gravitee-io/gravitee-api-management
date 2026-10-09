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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';

import WorkspaceDetailsComponent from './workspace-details.component';
import { AiWorkspace, AiWorkspaceModel } from '../../../../entities/ai-workspace/ai-workspace';
import { AiWorkspaceService } from '../../../../services/ai-workspace.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';

describe('WorkspaceDetailsComponent', () => {
  let fixture: ComponentFixture<WorkspaceDetailsComponent>;
  let component: WorkspaceDetailsComponent;
  const params = new BehaviorSubject(convertToParamMap({ workspaceId: 'ws-1' }));
  const navigate = jest.fn();
  const models: AiWorkspaceModel[] = [
    { name: 'gpt-4o-mini', inputPrice: '0.15', outputPrice: '0.6' },
    { name: 'gpt-4o', inputPrice: '2.5', outputPrice: '10' },
  ];
  const workspace: AiWorkspace = {
    id: 'ws-1',
    name: 'test-local-spf',
    endpointUrl: '/test-local-spf/',
    budget: { amount: 50.05, period: 'WEEK' },
    key: { value: 'key-1', status: 'ACTIVE', createdAt: '2026-10-01T11:33:46Z' },
    models,
  };

  beforeEach(async () => {
    params.next(convertToParamMap({ workspaceId: 'ws-1' }));
    navigate.mockClear();

    await TestBed.configureTestingModule({
      imports: [WorkspaceDetailsComponent],
      providers: [
        {
          provide: AiWorkspaceService,
          useValue: {
            get: jest.fn(() => of(workspace)),
            getConsumption: jest.fn(() => of({ tokens: 4, requests: 2, cost: 1.5, from: 'a', to: 'b' })),
          },
        },
        { provide: Router, useValue: { navigate } },
        { provide: ActivatedRoute, useValue: { paramMap: params } },
      ],
    })
      .overrideComponent(WorkspaceDetailsComponent, { set: { template: '', imports: [] } })
      .compileComponents();

    fixture = TestBed.createComponent(WorkspaceDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows the workspace, consumption, key, models, and curl snippet', () => {
    expect(component.loading()).toBe(false);
    expect(component.workspace()?.name).toBe('test-local-spf');
    expect(component.consumption()).toEqual({ tokens: 4, requests: 2, cost: 1.5, from: 'a', to: 'b' });
    expect(component.keys()).toEqual([workspace.key]);
    expect(component.pagedKeys()).toEqual([workspace.key]);
    expect(component.keyStatusIcon(workspace.key!)).toBe('check_circle');
    expect(component.keyStatusIcon({ value: 'paused', status: 'PAUSED' })).toBe('pause_circle');
    expect(component.models().map(model => model.name)).toEqual(['gpt-4o-mini', 'gpt-4o']);
    expect(component.modelsTotal()).toBe(2);
    expect(component.modelsCurrentPage()).toBe(1);
    expect(component.showCallSnippet()).toBe(true);
    expect(component.selectedModelName()).toBe('gpt-4o-mini');
    expect(component.codeSnippet()).toContain('gpt-4o-mini');
    expect(TestBed.inject(BreadcrumbService).breadcrumbs()[1]).toEqual({ id: 'workspace-ws-1', label: 'test-local-spf' });
  });

  it('changes the model, the client, and the model page', () => {
    component.onSnippetModelChange('gpt-4o');
    component.onSnippetFormatChange('python-openai');
    fixture.detectChanges();

    expect(component.codeSnippet()).toContain('from openai import OpenAI');
    expect(component.codeSnippet()).toContain('gpt-4o');

    component.onModelsPageChange(2);
    fixture.detectChanges();
    expect(component.modelsCurrentPage()).toBe(2);
    expect(component.models().map(model => model.name)).toEqual([]);

    component.onModelsPageSizeChange(10);
    fixture.detectChanges();
    expect(component.modelsPage()).toBe(1);
    expect(component.models().map(model => model.name)).toEqual(['gpt-4o-mini', 'gpt-4o']);

    component.onKeysPageChange(2);
    expect(component.pagedKeys()).toEqual([]);
  });

  it('marks the snippet copied and clears the flag', () => {
    jest.useFakeTimers();
    component.onSnippetCopied();
    expect(component.snippetCopied()).toBe(true);
    jest.advanceTimersByTime(2000);
    expect(component.snippetCopied()).toBe(false);
    jest.useRealTimers();
  });

  it('hides the snippet when the workspace has no endpoint and no key', () => {
    TestBed.inject(AiWorkspaceService).get = jest.fn(() => of({ id: 'ws-1', name: 'bare' }));
    params.next(convertToParamMap({ workspaceId: 'ws-2' }));
    fixture.detectChanges();

    expect(component.showCallSnippet()).toBe(false);
    expect(component.keys()).toEqual([]);
    expect(component.codeSnippet()).toBe('');
  });

  it('clears the selected model when the model list becomes empty', () => {
    TestBed.inject(AiWorkspaceService).get = jest.fn(() => of({ id: 'ws-3', name: 'bare' }));
    params.next(convertToParamMap({ workspaceId: 'ws-3' }));
    fixture.detectChanges();

    expect(component.selectedModelName()).toBeNull();
    expect(component.modelsTotal()).toBe(0);
    expect(component.modelsCurrentPage()).toBe(1);
  });

  it('returns to the list when the workspace cannot be loaded', () => {
    TestBed.inject(AiWorkspaceService).get = jest.fn(() => throwError(() => new Error('missing')));
    params.next(convertToParamMap({ workspaceId: 'missing' }));
    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith(['/dashboard', 'workspaces']);
    expect(component.workspace()).toBeNull();
    expect(component.loading()).toBe(false);
  });

  it('uses empty consumption and models when the route has no workspace id', () => {
    params.next(convertToParamMap({}));
    fixture.detectChanges();

    expect(component.workspace()).toBeNull();
    expect(component.consumption()).toEqual({ tokens: 0, requests: 0, cost: 0, from: '', to: '' });
    expect(component.snippetModels()).toEqual([]);
    expect(component.models()).toEqual([]);
  });
});
