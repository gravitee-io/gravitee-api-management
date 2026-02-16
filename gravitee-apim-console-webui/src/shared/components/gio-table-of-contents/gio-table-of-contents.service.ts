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

import { Injectable } from '@angular/core';
import { isEmpty } from 'lodash';
import { Subject } from 'rxjs';
import { mapTo, startWith } from 'rxjs/operators';

import { TocSection, TocSectionLink } from './TocSection';

@Injectable({ providedIn: 'root' })
export class GioTableOfContentsService {
  private sectionsLinks: Record<string, TocSection> = {};
  private sectionsLinksChanges$ = new Subject<void>();

  getSections$() {
    return this.sectionsLinksChanges$.pipe(mapTo(this.sectionsLinks), startWith(this.sectionsLinks));
  }

  markAsChanged() {
    this.sectionsLinksChanges$.next();
  }

  addSection(sectionId: string, name?: string) {
    if (!this.sectionsLinks[sectionId]) {
      this.sectionsLinks[sectionId] = { links: [], name };
    }

    this.sectionsLinks[sectionId].name = name;
    this.sectionsLinksChanges$.next();
  }

  addLink(sectionId: string, link: TocSectionLink) {
    if (!this.sectionsLinks[sectionId]) {
      this.addSection(sectionId);
    }
    const section = this.sectionsLinks[sectionId];

    section.links = [...section.links, link];
    this.sectionsLinksChanges$.next();
  }

  removeLink(sectionId: string, linkId: string) {
    if (!this.sectionsLinks[sectionId]) {
      return;
    }
    const section = this.sectionsLinks[sectionId];

    section.links.splice(
      section.links.findIndex(i => i.id === linkId),
      1,
    );

    // If is the last link remove section
    if (isEmpty(section.links)) {
      delete this.sectionsLinks[sectionId];
    }
    this.sectionsLinksChanges$.next();
  }
}
