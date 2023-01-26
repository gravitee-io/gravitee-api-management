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
const AlertEnginePromotionComponent: ng.IComponentOptions = {
  bindings: {},
  template: require('./alert-engine-promotion.html'),
  /* @ngInject */
  controller: function () {
    this.contactUrl = 'https://www.gravitee.io/contact-us-alerts/?utm_source=APIM&utm_medium=try_alert_engine&utm_campaign=contact_sales';
    this.learnUrl =
      'https://www.gravitee.io/platform/api-observability/?utm_source=APIM&utm_medium=try_alert_engine&utm_campaign=learn_more';
  },
};

export default AlertEnginePromotionComponent;
