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
import * as moment from 'moment';

function portalI18nConfig($translateProvider, $windowProvider) {
  'ngInject';

  $translateProvider.useLoader('i18nCustomLoader', {
    prefix: 'portal/i18n/',
    suffix: '.json'
  });

  const $window = $windowProvider.$get();
  const lang = $window.navigator.language || $window.navigator.userLanguage;

  $translateProvider.preferredLanguage(lang.substring(0, 2));
  $translateProvider.fallbackLanguage('en');
  $translateProvider.useSanitizeValueStrategy('escape');

  const locale = (window.navigator as any).userLanguage || window.navigator.language;
  moment.locale(locale);

}

export default portalI18nConfig;
