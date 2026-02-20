/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { Router } from '@angular/router';

import { DocumentationSubscribeComponent } from './documentation-subscribe.component';
import { fakeApi } from '../../../../entities/api/api.fixtures';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('DocumentationSubscribeComponent', () => {
  let fixture: ComponentFixture<DocumentationSubscribeComponent>;
  let component: DocumentationSubscribeComponent;
  let routerSpy: jest.Mocked<Router>;

  const MOCK_API = fakeApi({ id: 'api-1', name: 'Test API' });

  const init = async () => {
    routerSpy = { navigate: jest.fn().mockReturnValue(Promise.resolve(true)) } as unknown as jest.Mocked<Router>;

    await TestBed.configureTestingModule({
      imports: [DocumentationSubscribeComponent, MatIconTestingModule, AppTestingModule],
      providers: [{ provide: Router, useValue: routerSpy }],
      schemas: [NO_ERRORS_SCHEMA],
    })
      .overrideComponent(DocumentationSubscribeComponent, {
        set: { imports: [MatIconTestingModule], schemas: [NO_ERRORS_SCHEMA] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DocumentationSubscribeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('api', MOCK_API);
    fixture.componentRef.setInput('navId', 'nav-1');
    fixture.detectChanges();
  };

  it('should create', async () => {
    await init();
    expect(component).toBeTruthy();
  });
});
