import { kebabCase } from 'lodash';

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
export interface TocSection {
  name: string;
  links: TocSectionLink[];
}

export class TocSectionLink {
  /**
   * Id of the section
   */
  public get id(): string {
    const name = (this.options.name ?? this.element.innerText)?.trim();
    return kebabCase(name);
  }

  /**
   * Header type h3/h4
   */
  public get type(): string {
    return String(this.element.tagName).toLowerCase();
  }

  /**
   * If the anchor is in view of the page
   */
  active = false;

  /**
   * Name of the anchor
   */
  public get name(): string {
    return (this.options.name ?? this.element.innerText)?.trim();
  }

  /**
   * Top offset of the anchor in px
   */
  public get top(): number {
    return this.element.getBoundingClientRect().top;
  }

  constructor(private element: HTMLElement, private options?: { name?: string }) {}
}
