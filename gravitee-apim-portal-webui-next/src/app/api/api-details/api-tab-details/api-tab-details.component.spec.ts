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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { ApiTabDetailsComponent } from './api-tab-details.component';
import { fakeApi } from '../../../../entities/api/api.fixtures';
import { CategoriesService } from '../../../../services/categories.service';
import { PageService } from '../../../../services/page.service';
import { PortalService } from '../../../../services/portal.service';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('ApiTabDetailsComponent', () => {
  let component: ApiTabDetailsComponent;
  let fixture: ComponentFixture<ApiTabDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiTabDetailsComponent, AppTestingModule, HttpClientTestingModule],
      providers: [
        { provide: PageService, useValue: { listByApiId: () => of({ data: [] }), content: () => of({}) } },
        { provide: CategoriesService, useValue: { categories: () => of({ data: [] }) } },
        {
          provide: PortalService,
          useValue: {
            getApiInformations: () =>
              of([
                { name: 'api.publishedAt', value: '15 Oct 2024' },
                { name: 'api.version', value: '2' },
                { name: 'OneTwoThree', value: '2' },
                { name: 'api.Team', value: 'Fr' },
                { name: 'myTeam', value: 'Fr' },
              ]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabDetailsComponent);
    component = fixture.componentInstance;
    component.api = fakeApi();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display "Api Last Updated" if present', () => {
    component.api.updated_at = new Date();
    fixture.detectChanges();
    let element = fixture.debugElement.queryAll(By.css('.m3-body-large')).find(el => el.nativeElement.textContent.includes('Published at'));
    expect(element).toBeDefined();

    element = fixture.debugElement.queryAll(By.css('.m3-body-large')).find(el => el.nativeElement.textContent.includes('Version'));
    expect(element).toBeDefined();

    element = fixture.debugElement.queryAll(By.css('.m3-body-large')).find(el => el.nativeElement.textContent.includes('One two three'));
    expect(element).toBeDefined();

    element = fixture.debugElement.queryAll(By.css('.m3-body-large')).find(el => el.nativeElement.textContent.includes('Team'));
    expect(element).toBeDefined();

    element = fixture.debugElement.queryAll(By.css('.m3-body-large')).find(el => el.nativeElement.textContent.includes('My team'));
    expect(element).toBeDefined();
  });
});
