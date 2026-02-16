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
class CorsService {
  private allowOriginPattern =
    '^(?:(?:[htps\\(\\)?\\|]+):\\/\\/)*(?:[\\w\\(\\)\\[\\]\\{\\}?\\|.*-](?:(?:[?+*]|\\{\\d+(?:,\\d*)?\\}))?)+(?:[a-zA-Z0-9]{2,6})?(?::\\d{1,5})?$';

  constructor(private $mdDialog) {}

  getHttpMethods() {
    return ['GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD'];
  }

  controlAllowOrigin(chip, index, allowOriginArray) {
    if ('*' === chip) {
      this.$mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('html-loader!../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
          clickOutsideToClose: true,
          locals: {
            title: 'Are you sure you want to remove all cross-origin restrictions?',
            confirmButton: 'Yes, I want to allow all origins.',
          },
        })
        .then(response => {
          if (!response) {
            allowOriginArray.splice(index, 1);
          }
        });
    } else {
      const validator = new RegExp(this.allowOriginPattern, 'ig');

      if (!validator.test(chip)) {
        let invalidRegex = false;
        if (['{', '[', '(', '*'].some(v => this.allowOriginPattern.includes(v))) {
          try {
            new RegExp(chip);
          } catch (e) {
            invalidRegex = true;
          }
        }

        if (invalidRegex) {
          this.$mdDialog
            .show(
              this.$mdDialog.alert({
                title: 'Invalid regex',
                textContent: `${chip} is not a valid origin`,
                ok: 'Close',
              }),
            )
            .then(() => {
              allowOriginArray.splice(index, 1);
            });
        }
      }
    }
  }

  isRegexValid(allowOriginArray) {
    let isValid = true;
    if (allowOriginArray) {
      allowOriginArray.forEach(allowOrigin => {
        if ('*' !== allowOrigin && (allowOrigin.includes('(') || allowOrigin.includes('[') || allowOrigin.includes('*'))) {
          try {
            // eslint:disable-next-line:no-unused-expression
            new RegExp(allowOrigin);
          } catch (e) {
            isValid = false;
          }
        }
      });
    }
    return isValid;
  }

  querySearchHeaders = (query, headers) => {
    return query ? headers.filter(this.createFilterFor(query)) : [];
  };

  createFilterFor = (query: string) => {
    const lowercaseQuery = query.toLowerCase();

    return function filterFn(header) {
      return header.toLowerCase().indexOf(lowercaseQuery) === 0;
    };
  };
}
CorsService.$inject = ['$mdDialog'];

export default CorsService;
