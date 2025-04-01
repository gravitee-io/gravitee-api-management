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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { License } from '@gravitee/ui-particles-angular';

@Component({
  selector: 'gio-license-banner',
  templateUrl: './gio-license-banner.component.html',
  styleUrls: ['./gio-license-banner.component.scss'],
  standalone: false,
  host: {},
})
export class GioLicenseBannerComponent implements OnInit {
  @Input()
  license: License;

  @Input()
  isOEM = false;

  @Output() onRequestUpgrade = new EventEmitter<MouseEvent>();

  // Default values for OSS or when license is null
  header = 'This configuration requires an enterprise license';
  body =
    'Request a license to unlock enterprise functionality, such as support for connecting to event brokers, connecting to APIs via Websocket and Server-Sent Events, and publishing Webhooks.';
  showCta = true;

  ngOnInit(): void {
    if (!this.license) {
      return;
    }

    if (this.isOEM) {
      // OEM
      this.header = 'This configuration requires a license upgrade';
      this.body = 'Your platform license does not support this feature. Please contact your platform administrator to find out more.';
      this.showCta = false;
    } else if (this.license.scope === 'ORGANIZATION' || this.license.tier !== 'oss') {
      // Cloud + EE
      this.header = 'This configuration requires a license upgrade';
      this.body =
        'Your organization’s license does not support some features used in this API. Request an upgrade to enable the selected features.';
      this.showCta = this.license.scope !== 'ORGANIZATION'; // Hide for Cloud
    }
  }

  requestUpgrade($event: MouseEvent) {
    this.onRequestUpgrade.emit($event);
  }
}
