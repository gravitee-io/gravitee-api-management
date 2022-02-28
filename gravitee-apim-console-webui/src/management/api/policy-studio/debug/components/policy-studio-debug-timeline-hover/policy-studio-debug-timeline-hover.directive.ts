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

import { Directive, ElementRef, HostListener, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { PolicyStudioDebugTimelineHoverService } from './policy-studio-debug-timeline-hover.service';

@Directive({
  selector: '[timelineHover]',
})
export class PolicyStudioDebugTimelineHoverComponent implements OnInit, OnDestroy {
  @Input()
  public timelineHover: string;

  private unsubscribe$ = new Subject();

  constructor(
    private readonly renderer: Renderer2,
    private readonly hostElement: ElementRef,
    private readonly policyStudioDebugTimelineHoverService: PolicyStudioDebugTimelineHoverService,
  ) {}

  ngOnInit(): void {
    if (this.timelineHover) {
      this.policyStudioDebugTimelineHoverService
        .hoveredChanges(this.timelineHover)
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe((isHover) => {
          isHover
            ? this.renderer.addClass(this.hostElement.nativeElement, 'hover')
            : this.renderer.removeClass(this.hostElement.nativeElement, 'hover');
        });
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  @HostListener('mouseenter') onMouseEnter() {
    if (this.timelineHover) {
      this.policyStudioDebugTimelineHoverService.setHover(this.timelineHover, true);
    }
  }

  @HostListener('mouseleave') onMouseLeave() {
    if (this.timelineHover) {
      this.policyStudioDebugTimelineHoverService.setHover(this.timelineHover, false);
    }
  }
}
