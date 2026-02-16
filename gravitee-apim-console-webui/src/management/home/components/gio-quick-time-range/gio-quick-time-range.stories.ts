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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs/operators';
import { BehaviorSubject } from 'rxjs';

import { GioQuickTimeRangeComponent } from './gio-quick-time-range.component';
import { GioQuickTimeRangeModule } from './gio-quick-time-range.module';

export default {
  title: 'Home / Components / Quick time range',
  component: GioQuickTimeRangeComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioQuickTimeRangeModule, ReactiveFormsModule],
    }),
  ],
  argTypes: {
    onRefreshClicked: { action: 'onRefreshClicked' },
  },
  render: ({ onRefreshClicked }) => {
    const formControl = new FormControl('1M');
    const formGroup = new FormGroup({
      range: formControl,
    });

    const onRefresh$ = new BehaviorSubject(undefined);

    const timeFrameRangesParams$ = onRefresh$.pipe(
      switchMap(() => formControl.valueChanges.pipe(startWith(formControl.value))),
      map(value => GioQuickTimeRangeComponent.getTimeFrameRangesParams(value)),
    );

    return {
      template: `
      <form [formGroup]="formGroup">
          <gio-quick-time-range [formControl]="formControl" (onRefreshClicked)="onRefreshClicked() ; onRefresh$.next()"></gio-quick-time-range>
      </form>
      <br>
      Form value : {{ formGroup?.getRawValue() | json }}<br>
      Selected timeFrameRangesParams : {{ timeFrameRangesParams$ | async | json }}`,
      props: { formControl, formGroup, onRefreshClicked, timeFrameRangesParams$, onRefresh$ },
    };
  },
} as Meta;

export const Default: StoryObj = {};
Default.args = {};
