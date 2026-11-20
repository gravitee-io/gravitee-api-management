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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideRouter } from '@angular/router';

import { DocumentationComponent } from './documentation.component';
import { fakePortalNavigationPage } from '../../../entities/portal-navigation/portal-navigation-item.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('DocumentationComponent scroll position', () => {
  let fixture: ComponentFixture<DocumentationComponent>;
  let httpTestingController: HttpTestingController;
  const scrollTops = new WeakMap<Element, number>();
  let originalDescriptor: PropertyDescriptor | undefined;

  beforeEach(async () => {
    // jsdom does not keep scroll offsets: emulate them so that scrolling can be observed
    originalDescriptor = Object.getOwnPropertyDescriptor(Element.prototype, 'scrollTop');
    Object.defineProperty(Element.prototype, 'scrollTop', {
      configurable: true,
      get(this: Element) {
        return scrollTops.get(this) ?? 0;
      },
      set(this: Element, value: number) {
        scrollTops.set(this, value);
      },
    });

    await TestBed.configureTestingModule({
      imports: [DocumentationComponent, MatIconTestingModule, AppTestingModule],
      providers: [provideRouter([])],
    }).compileComponents();
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    if (originalDescriptor) {
      Object.defineProperty(Element.prototype, 'scrollTop', originalDescriptor);
    }
    httpTestingController.verify();
  });

  async function openPage(pageId: string) {
    fixture.componentRef.setInput('navItem', fakePortalNavigationPage({ id: pageId }));
    fixture.detectChanges();
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/portal-navigation-items/${pageId}/content`)
      .flush({ content: `content of ${pageId}`, type: 'GRAVITEE_MARKDOWN' });
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function pageScrollContainer(): HTMLElement {
    return fixture.nativeElement.querySelector('app-navigation-page-full-width');
  }

  it('should open the next page at the top after the previous page was scrolled', async () => {
    fixture = TestBed.createComponent(DocumentationComponent);
    await openPage('page-1');

    pageScrollContainer().scrollTop = 500;

    await openPage('page-2');

    expect(pageScrollContainer().scrollTop).toEqual(0);
  });
});
