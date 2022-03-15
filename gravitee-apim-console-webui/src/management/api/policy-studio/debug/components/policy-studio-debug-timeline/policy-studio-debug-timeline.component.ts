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

import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';

import { DebugEventMetrics } from '../../models/DebugEvent';

@Component({
  selector: 'policy-studio-debug-timeline',
  template: require('./policy-studio-debug-timeline.component.html'),
  styles: [require('./policy-studio-debug-timeline.component.scss')],
})
export class PolicyStudioDebugTimelineComponent implements AfterViewInit, OnDestroy {
  @Input()
  nbPoliciesRequest: number;

  @Input()
  nbPoliciesResponse: number;

  @Input()
  metrics: DebugEventMetrics;

  @ViewChild('horizontalScroll', { static: false }) horizontalScroll: ElementRef<HTMLDivElement>;

  private scrollHorizontally = (evt: WheelEvent) => {
    evt.preventDefault();
    this.horizontalScroll.nativeElement.scrollLeft += evt.deltaY + evt.deltaX;
  };

  ngAfterViewInit() {
    this.horizontalScroll.nativeElement.addEventListener('wheel', this.scrollHorizontally);
  }

  ngOnDestroy() {
    this.horizontalScroll.nativeElement.removeEventListener('wheel', this.scrollHorizontally);
  }
}
