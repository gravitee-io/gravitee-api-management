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
const IdentityProviderGraviteeioAmComponent: ng.IComponentOptions = {
  bindings: {
    identityProvider: '<'
  },
  template: require('./identity-provider-graviteeio-am.html'),
  controller: function(
  ) {
    'ngInject';

    this.buttonConfig = {};
    this.buttonConfig.backgroundOptions = {
      label: 'Text Background',
      icon: 'font_download',
      default: '#34A0D4',
      hasBackdrop: true,
      clickOutsideToClose: true,
      random: true,
      openOnInput: true,
      alphaChannel: false,
      genericPalette: false,
      history: false
    };
  }
};

export default IdentityProviderGraviteeioAmComponent;
