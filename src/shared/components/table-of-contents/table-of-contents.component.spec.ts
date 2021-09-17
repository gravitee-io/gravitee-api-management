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
import { camelCase } from 'lodash';

import { TocSectionLink } from './LinkSection';
import { TableOfContentsComponent } from './table-of-contents.component';
import { TableOfContentsModule } from './table-of-contents.module';
import { TableOfContentsService } from './table-of-contents.service';

describe('GioConfirmDialogComponent', () => {
  let component: TableOfContentsComponent;
  let fixture: ComponentFixture<TableOfContentsComponent>;
  let tableOfContentsService: TableOfContentsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TableOfContentsModule],
      // providers: [{ provide: DOCUMENT, useValue: 'aa' }],
    });
    fixture = TestBed.createComponent(TableOfContentsComponent);
    component = fixture.componentInstance;

    tableOfContentsService = TestBed.inject(TableOfContentsService);
    fixture.nativeElement.getBoundingClientRect = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should display section with links dynamically', () => {
    fixture.detectChanges();
    tableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    tableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '2️⃣']);

    tableOfContentsService.addLink('', fakeLink({ name: '3️⃣' }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '2️⃣', '3️⃣']);
    expect(getSectionName()).toEqual(undefined);

    tableOfContentsService.addSection('', 'Section 🔢');
    fixture.detectChanges();

    expect(getSectionName()).toEqual('Section 🔢');
  });

  it('should active link on scroll', async () => {
    component.scrollingContainer = document.body;
    fixture.detectChanges();

    tableOfContentsService.addLink('', fakeLink({ name: '1️⃣', top: 42 }));
    tableOfContentsService.addLink('', fakeLink({ name: '2️⃣', top: 666 }));
    fixture.detectChanges();

    // Simulate scroll to link 1
    fixture.nativeElement.getBoundingClientRect.mockReturnValue({ top: 50 });
    document.body.dispatchEvent(new Event('scroll'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getActiveLinks()).toEqual(['1️⃣']);

    // Simulate scroll to link 2
    fixture.nativeElement.getBoundingClientRect.mockReturnValue({ top: 1000 });
    document.body.dispatchEvent(new Event('scroll'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getActiveLinks()).toEqual(['2️⃣']);
  });

  it('should set section name', async () => {
    fixture.detectChanges();

    tableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    tableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    tableOfContentsService.addLink('🦊', fakeLink({ name: '🦊' }));

    component.sectionNames = { '': 'Section name', '🦊': 'Fox section' };
    fixture.detectChanges();

    expect(getSectionsName()).toEqual(['Section name', 'Fox section']);
  });

  it('should remove section if links become empty', async () => {
    fixture.detectChanges();

    tableOfContentsService.addLink('', fakeLink({ name: '1' }));
    tableOfContentsService.addLink('', fakeLink({ name: '2' }));
    tableOfContentsService.addLink('🦊', fakeLink({ name: '🦊' }));

    component.sectionNames = { '': 'Section name', '🦊': 'Fox section' };
    fixture.detectChanges();

    expect(getSectionsName()).toEqual(['Section name', 'Fox section']);

    tableOfContentsService.removeLink('', '1️');
    tableOfContentsService.removeLink('', '2');

    fixture.detectChanges();
    expect(getSectionsName()).toEqual(['Fox section']);
  });

  const getLinksText = () => [...fixture.nativeElement.querySelectorAll('.toc__link')].map((el) => el.innerHTML);
  const getActiveLinks = () => [...fixture.nativeElement.querySelectorAll('.toc__link.active')].map((el) => el.innerHTML);
  const getSectionName = () => fixture.nativeElement.querySelector('.toc__section-name')?.innerHTML;
  const getSectionsName = () => [...fixture.nativeElement.querySelectorAll('.toc__section-name')].map((e) => e?.innerHTML);
});

const fakeLink = (attr: Partial<TocSectionLink>): TocSectionLink => {
  const baseName = attr.name ?? 'Fake Link';
  const base = { active: false, id: camelCase(baseName), name: 'Fake Link', top: 10, type: 'h2' };

  return { ...base, ...attr };
};
