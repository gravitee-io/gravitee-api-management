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
import { Directive, ElementRef, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Directive({
  selector: '[appInnerLink]',
  standalone: true,
})
export class InnerLinkDirective {
  constructor(
    private router: Router,
    private el: ElementRef<HTMLAnchorElement>,
  ) {}

  @HostListener('click', ['$event'])
  public onClick(e: PointerEvent) {
    const target = this.el.nativeElement;
    const href = target.getAttribute('href');

    // 1. Check for valid local link (no external protocols)
    if (href && !href.startsWith('https:') && !href.startsWith('http:')) {
      e.preventDefault();

      // 2. Use navigateByUrl.
      // We pass skipLocationChange: false (default), which ensures a new
      // state is pushed onto the history stack, which the back button relies on.
      this.router.navigateByUrl(href);
    }
  }
}
