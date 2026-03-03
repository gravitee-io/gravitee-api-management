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

import { PageRedocComponent } from './page-redoc.component';
import { PageRedocHarness } from './page-redoc.harness';
import { Page } from '../../../entities/page/page';
import { RedocService } from '../../../services/redoc.service';

describe('PageRedocComponent', () => {
  let fixture: ComponentFixture<PageRedocComponent>;
  let harness: PageRedocHarness;

  const pageWithContent: Page = {
    id: 'page-1',
    name: 'API Docs',
    type: 'SWAGGER',
    order: 0,
    content: 'openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0',
  };

  const pageWithoutContent: Page = {
    id: 'page-2',
    name: 'Empty',
    type: 'SWAGGER',
    order: 0,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageRedocComponent],
      providers: [
        {
          provide: RedocService,
          useValue: { init: jest.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PageRedocComponent);
    fixture.componentRef.setInput('page', pageWithContent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PageRedocHarness);
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render redoc content viewer when page has content', async () => {
    fixture.componentRef.setInput('page', pageWithContent);
    fixture.detectChanges();
    const redoc = await harness.getRedoc();
    expect(redoc).toBeTruthy();
  });

  it('should not render redoc container when page has no content', async () => {
    fixture.componentRef.setInput('page', pageWithoutContent);
    fixture.detectChanges();
    const redocElement = fixture.nativeElement.querySelector('#redoc');
    expect(redocElement).toBeFalsy();
  });
});
