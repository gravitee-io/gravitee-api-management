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
import { provideRouter } from '@angular/router';

import AiWorkspacesComponent from './ai-workspaces.component';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('AiWorkspacesComponent', () => {
  let fixture: ComponentFixture<AiWorkspacesComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiWorkspacesComponent, AppTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AiWorkspacesComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should render a tile from the list response', fakeAsync(() => {
    fixture.detectChanges();
    const request = http.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=10`);
    request.flush({
      data: [{ id: 'ws-1', name: 'Alpha', description: 'A workspace', budget: { amount: 5, period: 'DAY' } }],
      page: 1,
      size: 10,
      total: 1,
    });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Alpha');
    expect(fixture.nativeElement.textContent).toContain('A workspace');
    expect(fixture.nativeElement.textContent).toContain('5 / DAY');
  }));

  it('should send the name filter to the server', fakeAsync(() => {
    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=10`).flush({ data: [], page: 1, size: 10, total: 0 });
    tick();

    fixture.componentInstance.name.setValue('alp');
    tick(300);
    fixture.detectChanges();

    const request = http.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=10&name=alp`);
    request.flush({ data: [], page: 1, size: 10, total: 0 });
  }));

  it('should request the selected page size from the server', fakeAsync(() => {
    fixture.detectChanges();
    http.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=10`).flush({ data: [], page: 1, size: 10, total: 0 });
    tick();

    fixture.componentInstance.setPageSize(20);
    fixture.detectChanges();
    tick();

    const request = http.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=20`);
    request.flush({ data: [], page: 1, size: 20, total: 0 });
  }));
});
