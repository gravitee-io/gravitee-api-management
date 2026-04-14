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
import { By } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { ApiRuntimeLogsListComponent } from './api-runtime-logs-list.component';

import { fakeConnectionLog } from '../../../../../../entities/management-api-v2';
import { GioTestingModule } from '../../../../../../shared/testing';

describe('ApiRuntimeLogsListComponent', () => {
  const LOG = fakeConnectionLog();
  const PAGINATION = { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 1, totalCount: 1 };

  let fixture: ComponentFixture<ApiRuntimeLogsListComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsListComponent, GioTestingModule, NoopAnimationsModule],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsListComponent);

    fixture.componentRef.setInput('logs', [LOG]);
    fixture.componentRef.setInput('pagination', PAGINATION);
    fixture.componentRef.setInput('apiType', 'PROXY');
    fixture.detectChanges();
  });

  it('should render a row per log entry', () => {
    const rows = fixture.nativeElement.querySelectorAll('[data-testid="api_logs_table_row"]');
    expect(rows).toHaveLength(1);
  });

  describe('detail navigation links', () => {
    it('should set queryParamsHandling="preserve" on the timestamp link', () => {
      const timestampLink = fixture.debugElement.query(By.css('td a:not([data-testid])'));
      expect(timestampLink).toBeTruthy();
      expect(timestampLink.injector.get(RouterLink).queryParamsHandling).toBe('preserve');
    });

    it('should set queryParamsHandling="preserve" on the details button', () => {
      const detailsButton = fixture.debugElement.query(By.css('[data-testid="api_logs_details_button"]'));
      expect(detailsButton).toBeTruthy();
      expect(detailsButton.injector.get(RouterLink).queryParamsHandling).toBe('preserve');
    });
  });
});
