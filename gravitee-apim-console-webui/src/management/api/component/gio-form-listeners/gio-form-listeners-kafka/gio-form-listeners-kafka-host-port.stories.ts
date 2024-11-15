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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { GioFormListenersKafkaHostPortComponent } from './gio-form-listeners-kafka-host-port.component';

export default {
  title: 'API / Listeners / Kafka / Form listeners Kafka host + port',
  component: GioFormListenersKafkaHostPortComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioFormListenersKafkaHostPortComponent, FormsModule, ReactiveFormsModule, HttpClientModule],
      providers: [],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `<gio-form-listeners-kafka-host-port [ngModel]="listeners" />`,
    props: args,
  }),
  args: {
    listeners: {},
  },
} as Meta;

export const Default: StoryObj = {};
Default.args = {};
