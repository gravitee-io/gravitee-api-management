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

import { HttpTestingController } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { GuidesPageComponent } from './guides-page.component';
import { Page } from '../../../entities/page/page';
import { fakePage } from '../../../entities/page/page.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

@Component({
  selector: 'app-test-component',
  template: `<app-guides-page #guidesPage [pages]="pages"></app-guides-page>`,
  standalone: true,
  imports: [GuidesPageComponent],
})
class TestComponent {
  pages: Page[] = [];
}

describe('GuidesPageComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let paramMapSubject: BehaviorSubject<Map<string, string>>;

  beforeEach(async () => {
    paramMapSubject = new BehaviorSubject(new Map());

    await TestBed.configureTestingModule({
      imports: [TestComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMapSubject.asObservable(),
            snapshot: { paramMap: paramMapSubject.getValue() },
            routeConfig: {},
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Page loading', () => {
    it('should display loader when no page is loaded', () => {
      const loader = fixture.debugElement.nativeElement.querySelector('app-loader');
      expect(loader).toBeTruthy();
    });

    it('should load and display markdown page when pageId is provided', async () => {
      const testPage = fakePage({ id: 'valid-page', type: 'MARKDOWN', content: '# Test Content' });
      fixture.componentInstance.pages = [testPage];
      fixture.detectChanges();

      paramMapSubject.next(new Map([['pageId', testPage.id]]));
      fixture.detectChanges();

      expectGetPageContent(testPage);
    });

    it('should load first page when no pageId is provided', async () => {
      const testPage = fakePage({ id: 'first-page', type: 'MARKDOWN', content: '# First Page Content' });
      fixture.componentInstance.pages = [testPage, fakePage({ id: 'second-page', type: 'MARKDOWN', content: '# Second Page Content' })];
      fixture.detectChanges();

      paramMapSubject.next(new Map([['foo', 'bar']]));
      fixture.detectChanges();

      expectGetPageContent(testPage);
    });

    it('should not display page when pages array is empty', () => {
      const pageComponent = fixture.debugElement.nativeElement.querySelector('app-page');
      expect(pageComponent).toBeFalsy();
    });
  });

  function expectGetPageContent(page: Page) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/pages/${page.id}?include=content`).flush(page);
    fixture.detectChanges();
  }
});
