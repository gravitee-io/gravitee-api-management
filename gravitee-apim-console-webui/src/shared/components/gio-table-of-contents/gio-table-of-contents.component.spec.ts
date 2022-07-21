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
import { LocationStrategy } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { camelCase } from 'lodash';
import { MockLocationStrategy } from '@angular/common/testing';

import { TocSectionLink } from './TocSection';
import { GioTableOfContentsComponent } from './gio-table-of-contents.component';
import { GioTableOfContentsModule } from './gio-table-of-contents.module';
import { GioTableOfContentsService } from './gio-table-of-contents.service';

describe('GioConfirmDialogComponent', () => {
  let component: GioTableOfContentsComponent;
  let fixture: ComponentFixture<GioTableOfContentsComponent>;
  let gioTableOfContentsService: GioTableOfContentsService;
  let locationStrategy: MockLocationStrategy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTableOfContentsModule],
      providers: [{ provide: LocationStrategy, useClass: MockLocationStrategy }],
    });
    fixture = TestBed.createComponent(GioTableOfContentsComponent);
    component = fixture.componentInstance;

    gioTableOfContentsService = TestBed.inject(GioTableOfContentsService);
    locationStrategy = TestBed.inject(LocationStrategy) as MockLocationStrategy;
    fixture.nativeElement.getBoundingClientRect = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should display section with links dynamically', () => {
    fixture.detectChanges();
    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '2️⃣']);

    gioTableOfContentsService.addLink('', fakeLink({ name: '3️⃣' }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '2️⃣', '3️⃣']);
    expect(getSectionName()).toEqual(undefined);

    gioTableOfContentsService.addSection('', 'Section 🔢');
    fixture.detectChanges();

    expect(getSectionName()).toEqual('Section 🔢');
  });

  it('should order links by top', () => {
    fixture.detectChanges();
    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣', top: 10 }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '3️⃣', top: 30 }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '3️⃣']);

    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣', top: 20 }));
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['1️⃣', '2️⃣', '3️⃣']);
    expect(getSectionName()).toEqual(undefined);
  });

  it('should keep order when top change', () => {
    fixture.detectChanges();
    const foxLink = fakeLink({ name: '🦊', top: 10 });
    gioTableOfContentsService.addLink('', foxLink);
    const dogLink = fakeLink({ name: '🐶', top: 20 });
    gioTableOfContentsService.addLink('', dogLink);
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['🦊', '🐶']);

    // @ts-expect-error - Change top of foxLink, TS is throwing as it's a readonly property
    foxLink.top = 30;
    fixture.detectChanges();

    expect(getLinksText()).toEqual(['🐶', '🦊']);
    expect(getSectionName()).toEqual(undefined);
  });

  it('should active link on scroll', async () => {
    component.scrollingContainer = document.body;
    fixture.detectChanges();

    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣', top: 42 }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣', top: 666 }));
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

    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    gioTableOfContentsService.addLink('🦊', fakeLink({ name: '🦊' }));

    component.sectionNames = { '': 'Section name', '🦊': 'Fox section' };
    fixture.detectChanges();

    expect(getSectionsName()).toEqual(['Section name', 'Fox section']);
  });

  it('should remove section if links become empty', async () => {
    fixture.detectChanges();

    gioTableOfContentsService.addLink('', fakeLink({ name: '1' }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2' }));
    gioTableOfContentsService.addLink('🦊', fakeLink({ name: '🦊' }));

    component.sectionNames = { '': 'Section name', '🦊': 'Fox section' };
    fixture.detectChanges();

    expect(getSectionsName()).toEqual(['Section name', 'Fox section']);

    gioTableOfContentsService.removeLink('', '1️');

    fixture.detectChanges();
    expect(getSectionsName()).toEqual(['Section name', 'Fox section']);

    gioTableOfContentsService.removeLink('', '2');

    fixture.detectChanges();
    expect(getSectionsName()).toEqual(['Fox section']);
  });

  it('should update location by clicking on link', async () => {
    fixture.detectChanges();
    locationStrategy.simulatePopState('');

    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    fixture.detectChanges();

    component.onClick(({ stopPropagation: jest.fn() } as unknown) as PointerEvent, '1');

    expect(locationStrategy.path()).toBe('#1');
  });

  it('should update scroll position', async () => {
    component.scrollingContainer = document.body;

    // Init Dom with elements
    const element_1 = document.createElement('h2');
    element_1.id = 'toc-1';
    element_1.scrollIntoView = jest.fn();
    const element_1_2 = document.createElement('h2');
    element_1_2.id = 'toc-1-2';
    element_1_2.scrollIntoView = jest.fn();
    document.body.appendChild(element_1);
    document.body.appendChild(element_1_2);
    fixture.detectChanges();

    // Init links
    gioTableOfContentsService.addLink('', fakeLink({ name: '1️⃣' }));
    gioTableOfContentsService.addLink('', fakeLink({ name: '2️⃣' }));
    fixture.detectChanges();

    // Simulate location change to link 2️⃣
    locationStrategy.simulatePopState('#2');
    locationStrategy.simulatePopState('#1-2');
    expect(element_1_2.scrollIntoView).toHaveBeenCalledTimes(1);

    locationStrategy.simulatePopState('#1');

    expect(element_1.scrollIntoView).toHaveBeenCalledTimes(1);
  });

  const getLinksText = () => [...fixture.nativeElement.querySelectorAll('.toc__link')].map((el) => el.innerHTML);
  const getActiveLinks = () => [...fixture.nativeElement.querySelectorAll('.toc__link.active')].map((el) => el.innerHTML);
  const getSectionName = () => fixture.nativeElement.querySelector('.toc__section-name')?.innerHTML;
  const getSectionsName = () => [...fixture.nativeElement.querySelectorAll('.toc__section-name')].map((e) => e?.innerHTML);
});

const fakeLink = (attr: Partial<TocSectionLink>): TocSectionLink => {
  const baseName = attr.name ?? 'Fake Link';
  const base = { active: false, id: camelCase(baseName), name: 'Fake Link', top: 10, type: 'h2' };

  return { ...base, ...attr } as TocSectionLink;
};
