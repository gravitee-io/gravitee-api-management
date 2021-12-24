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

@Injectable({
  providedIn: 'root',
})
export class ScrollService {
  constructor() {}

  static pxToInt(size: string) {
    const intValue = parseInt(size.replace('px', ''), 10);
    return isNaN(intValue) ? 0 : intValue;
  }

  static getHeaderHeight(): number {
    const homepageHeader = document.querySelector('.layout__header__homepage__background');
    const header = document.querySelector('header');
    const homepageHeaderHeight = homepageHeader ? this.pxToInt(window.getComputedStyle(homepageHeader).height) - window.scrollY : 0;
    const headerHeight = header ? this.pxToInt(window.getComputedStyle(header).height) : 0;
    return Math.max(homepageHeaderHeight, headerHeight);
  }

  static getAnchorElement(anchor: string): Element {
    if (!anchor.startsWith('#')) {
      return document.getElementById(anchor);
    }
    return document.querySelector(anchor) || document.querySelector(`a[name=${anchor.substr(1)}]`);
  }

  scrollToAnchor(anchor) {
    return new Promise<void>((resolve, reject) => {
      setTimeout(() => {
        const element = ScrollService.getAnchorElement(anchor);

        if (element) {
          this.scrollToStickyMenu();
          setTimeout(() => {
            const { top, left } = element.getBoundingClientRect();
            const scrollY = top - ScrollService.getHeaderHeight();
            let resolutionTime = 500;
            let behavior = 'smooth';
            if (Math.abs(scrollY) > 5000) {
              resolutionTime = 250;
              behavior = 'auto';
            }
            // @ts-ignore
            window.scrollBy({ top: scrollY, left, behavior });
            setTimeout(() => {
              resolve();
            }, resolutionTime);
          }, 50);
        } else {
          reject();
        }
      }, 0);
    });
  }

  scrollToStickyMenu() {
    if (window.pageYOffset < 100) {
      window.scrollBy({ top: 100, left: 0 });
    }
  }
}
