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

class ThemeElementController {
  constructor (private Theme) {
    'ngInject';
  }
}

const ThemeElementDirective: ng.IDirective = ({
  restrict: 'A',
  link: function (scope, elem, attr, ctr: {Theme}) {
    let splittedTheme: string[] = attr.gvThemeElement.split('.');
    let style: any = ctr.Theme;

    splittedTheme.forEach((value: string) => {
      style = style[value];
    });

    elem.css(style);
  },
  controller: ThemeElementController,
});

export default ThemeElementDirective;
