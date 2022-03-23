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
import { transition, trigger, query, style, animate, group } from '@angular/animations';

export function isSlide(previous, next) {
  return previous && previous.type === 'slide' && next && next.type === 'slide' && previous.group === next.group;
}

export function toLeft(previous, next) {
  if (isSlide(previous, next)) {
    return previous.index < next.index;
  }
  return false;
}

export function toRight(previous, next) {
  if (isSlide(previous, next)) {
    return previous.index > next.index;
  }
  return false;
}

export function fade(previous, next) {
  if (next && next.type === 'fade') {
    return true;
  } else if (next && next.type === 'slide') {
    if (previous == null || previous.type !== 'slide' || (previous.type === 'slide' && previous.group !== next.group)) {
      return true;
    }
  }
  return false;
}

export const animation = trigger('routeAnimations', [
  transition(toRight, [
    style({ position: 'relative' }),
    query(
      ':enter, :leave',
      [
        style({
          position: 'absolute',
          left: 0,
          width: '100%',
        }),
      ],
      { optional: true },
    ),
    query(':enter', [style({ left: '-100%' })]),
    group([
      query(':leave', [animate('500ms ease-in-out', style({ left: '100%' }))], { optional: true }),
      query(':enter', [animate('500ms ease-in-out', style({ left: '0%' }))]),
    ]),
    // Required only if you have child animations on the page
    // query(':leave', animateChild()),
    // query(':enter', animateChild()),
  ]),
  transition(toLeft, [
    style({ position: 'relative' }),
    query(
      ':enter, :leave',
      [
        style({
          position: 'absolute',
          right: 0,
          width: '100%',
        }),
      ],
      { optional: true },
    ),
    query(':enter', [style({ right: '-100%' })]),
    group([
      query(':leave', [animate('500ms ease-in-out', style({ right: '100%' }))], { optional: true }),
      query(':enter', [animate('500ms ease-in-out', style({ right: '0%' }))]),
    ]),
    // Required only if you have child animations on the page
    // query(':leave', animateChild()),
    // query(':enter', animateChild()),
  ]),
  transition(fade, [
    style({ position: 'relative' }),
    query(
      ':enter, :leave',
      [
        style({
          position: 'absolute',
          width: '100%',
        }),
      ],
      { optional: true },
    ),
    query(':enter', [style({ opacity: 0 })], { optional: true }),
    query(':leave', [style({ opacity: 0 }), animate('0ms', style({ opacity: 0 }))], { optional: true }),

    query(':enter', [style({ opacity: 0 }), animate('200ms', style({ opacity: 1 }))], { optional: true }),
  ]),
]);
