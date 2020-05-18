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
  providedIn: 'root'
})
export class ScrollService {

  constructor() {
  }

  static getHeaderHeight(): number {
    const header = document.querySelector('header');
    if (header) {
      const { height } = window.getComputedStyle(header);
      return parseInt(height.replace('px', ''), 10);
    }
    return 0;
  }

  scrollToAnchor(anchor) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const element = document.getElementById(anchor);
        if (element) {
          this.scrollToStickyMenu();
          setTimeout(() => {
            const { top, left } = element.getBoundingClientRect();
            window.scrollBy({
              top: top - ScrollService.getHeaderHeight(),
              left,
              behavior: 'smooth'
            });
            setTimeout(() => {
              resolve();
            }, 750);
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
