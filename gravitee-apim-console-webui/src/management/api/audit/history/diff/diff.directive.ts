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

// eslint-disable-next-line @typescript-eslint/no-var-requires
const JsDiff = require('diff/dist/diff.min.js');

const DiffDirective: ng.IDirective = {
  restrict: 'AE',
  scope: {
    oldValue: '=',
    newValue: '=',
  },
  link: (scope: any, elem) => {
    scope.$watch('oldValue', () => {
      const oldValue = scope.oldValue;
      const newValue = scope.newValue;

      if (oldValue && newValue) {
        elem.html('');
        const diff = JsDiff.diffJson(oldValue, newValue);
        diff.forEach(part => {
          // green for additions, red for deletions
          // grey for common parts

          let classname = null;
          if (part.added) {
            classname = 'gv-diff-added';
          } else if (part.removed) {
            classname = 'gv-diff-removed';
          } else {
            classname = 'gv-diff-no-changes';
          }
          const group = document.createElement('div');
          group.style.position = 'relative';
          const lines = part.value.split('\n');
          lines.pop();
          lines.forEach(lineValue => {
            const line = document.createElement('div');
            line.appendChild(document.createTextNode(lineValue));
            line.classList.add(classname);
            group.appendChild(line);
          });

          elem.append(group);
        });
      }
    });
  },
};

export default DiffDirective;
