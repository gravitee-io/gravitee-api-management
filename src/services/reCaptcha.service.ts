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
declare var grecaptcha: any;
class ReCaptchaService {

  private readonly scriptId: string = 'reCaptcha';
  private readonly siteKey: string;
  private readonly enabled: Boolean;

  constructor(private $http, Constants) {
    'ngInject';
    this.enabled = Constants.reCaptcha && !!Constants.reCaptcha.enabled;
    if (this.enabled) {
      this.siteKey = Constants.reCaptcha.siteKey;
    }
  }

  load() {
    return new Promise((resolve, reject) => {
      if (this.enabled && !document.getElementById(this.scriptId)) {
        if (!this.siteKey) {
          reject('[reCaptchaService] Missing public site_key');
        } else {
          const script = document.createElement('script');
          script.id = this.scriptId;
          script.src = `https://www.google.com/recaptcha/api.js?render=${this.siteKey}`;
          script.async = true;
          script.onload = () => {
            grecaptcha.ready(resolve);
          };
          document.head.appendChild(script);
        }
      } else {
        resolve();
      }
    });
  }

  async execute(action: string) {
    if (this.enabled) {
      await this.load();
      return grecaptcha.execute(this.siteKey, { action });
    }
    return Promise.resolve(null);
  }

}

export default ReCaptchaService;
