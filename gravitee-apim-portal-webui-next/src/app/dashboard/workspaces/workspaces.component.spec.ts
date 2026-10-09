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
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import WorkspacesComponent from './workspaces.component';
import { AiWorkspacesResponse } from '../../../entities/ai-workspace/ai-workspace';
import { AiWorkspaceService } from '../../../services/ai-workspace.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';

describe('WorkspacesComponent', () => {
  let fixture: ComponentFixture<WorkspacesComponent>;
  let component: WorkspacesComponent;
  let list: jest.Mock;
  const navigate = jest.fn();

  const page = (response: AiWorkspacesResponse) => {
    list.mockReturnValue(of(response));
  };

  beforeEach(async () => {
    list = jest.fn();
    page({
      data: [{ id: 'ws-1', name: 'test-local-spf', description: 'local', budget: { amount: 50.05, period: 'WEEK' } }],
      metadata: { pagination: { current_page: 1, total: 2 } },
    });

    await TestBed.configureTestingModule({
      imports: [WorkspacesComponent],
      providers: [
        { provide: AiWorkspaceService, useValue: { list } },
        { provide: Router, useValue: { navigate } },
        { provide: ActivatedRoute, useValue: {} },
      ],
    })
      .overrideComponent(WorkspacesComponent, { set: { template: '', imports: [] } })
      .compileComponents();

    fixture = TestBed.createComponent(WorkspacesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('sets the list breadcrumb and maps the first page', () => {
    expect(TestBed.inject(BreadcrumbService).breadcrumbs()).toEqual([{ id: 'workspaces', label: 'My Workspace', url: undefined }]);
    expect(component.loadingPage()).toBe(false);
    expect(component.loadError()).toBe(false);
    expect(component.workspacePaginator()).toEqual({
      data: [{ id: 'ws-1', name: 'test-local-spf', description: 'local', budget: { amount: 50.05, period: 'WEEK' } }],
      page: 1,
      totalResults: 2,
    });
    expect(list).toHaveBeenCalledWith(1, 20, '');
  });

  it('searches by name and resets to the first page', () => {
    page({ data: [], metadata: { pagination: { current_page: 1, total: 0 } } });

    component.onSearchTermChange('spf');

    expect(list).toHaveBeenLastCalledWith(1, 20, 'spf');
    expect(component.workspacePaginator().totalResults).toBe(0);
  });

  it('falls back when the response omits data and pagination', () => {
    page({});

    component.onSearchTermChange('missing');

    expect(component.workspacePaginator()).toEqual({ data: [], page: 1, totalResults: 0 });
  });

  it('shows the load error and clears the page when the request fails', () => {
    list.mockReturnValue(throwError(() => new Error('offline')));

    component.onSearchTermChange('offline');

    expect(component.loadError()).toBe(true);
    expect(component.loadingPage()).toBe(false);
    expect(component.workspacePaginator()).toEqual({ data: [], page: 1, totalResults: 0 });
  });

  it('loads the next search after a failed request', () => {
    list.mockReturnValueOnce(throwError(() => new Error('offline')));
    component.onSearchTermChange('offline');
    expect(component.loadError()).toBe(true);

    page({ data: [{ id: 'ws-2', name: 'again' }], metadata: { pagination: { current_page: 1, total: 1 } } });
    component.onSearchTermChange('again');

    expect(component.loadError()).toBe(false);
    expect(component.workspacePaginator().data).toEqual([{ id: 'ws-2', name: 'again' }]);
    expect(list).toHaveBeenLastCalledWith(1, 20, 'again');
  });

  it('requests the selected page and page size', () => {
    page({ data: [], metadata: { pagination: { current_page: 3, total: 40 } } });
    component.onPageChange(3);
    expect(list).toHaveBeenLastCalledWith(3, 20, '');

    page({ data: [], metadata: { pagination: { current_page: 1, total: 40 } } });
    component.onPageSizeChange(40);
    expect(component.pageSize).toBe(40);
    expect(list).toHaveBeenLastCalledWith(1, 40, '');
  });

  it('does not request the same page twice', () => {
    const calls = list.mock.calls.length;

    component.onPageChange(1);

    expect(list.mock.calls.length).toBe(calls);
  });

  it('opens the selected workspace', () => {
    component.navigateToWorkspace('ws-1');

    expect(navigate).toHaveBeenCalledWith(['ws-1'], { relativeTo: TestBed.inject(ActivatedRoute) });
  });
});
