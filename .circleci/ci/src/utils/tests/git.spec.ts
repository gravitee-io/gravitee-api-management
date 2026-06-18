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
import { isOnlyGammaModuleVersionBump } from '../git';

describe('isOnlyGammaModuleVersionBump', () => {
  it('returns true when only a single gamma module version is bumped', () => {
    const diff = `--- a/pom.xml
+++ b/pom.xml
@@ -336 +336 @@
-        <gravitee-gamma-module-aim.version>1.0.0-alpha.168</gravitee-gamma-module-aim.version>
+        <gravitee-gamma-module-aim.version>1.0.0-alpha.169</gravitee-gamma-module-aim.version>`;
    expect(isOnlyGammaModuleVersionBump(diff)).toBe(true);
  });

  it('returns true when several gamma module versions are bumped', () => {
    const diff = `--- a/pom.xml
+++ b/pom.xml
-        <gravitee-gamma-module-aim.version>1.0.0-alpha.168</gravitee-gamma-module-aim.version>
+        <gravitee-gamma-module-aim.version>1.0.0-alpha.169</gravitee-gamma-module-aim.version>
-        <gravitee-gamma-module-authz.version>1.0.0-alpha.19</gravitee-gamma-module-authz.version>
+        <gravitee-gamma-module-authz.version>1.0.0-alpha.20</gravitee-gamma-module-authz.version>`;
    expect(isOnlyGammaModuleVersionBump(diff)).toBe(true);
  });

  it('returns false when another dependency version is bumped', () => {
    const diff = `--- a/pom.xml
+++ b/pom.xml
-        <gravitee-common.version>4.5.0</gravitee-common.version>
+        <gravitee-common.version>4.6.0</gravitee-common.version>`;
    expect(isOnlyGammaModuleVersionBump(diff)).toBe(false);
  });

  it('returns false when a gamma bump is mixed with another change', () => {
    const diff = `--- a/pom.xml
+++ b/pom.xml
-        <gravitee-gamma-module-aim.version>1.0.0-alpha.168</gravitee-gamma-module-aim.version>
+        <gravitee-gamma-module-aim.version>1.0.0-alpha.169</gravitee-gamma-module-aim.version>
-        <gravitee-common.version>4.5.0</gravitee-common.version>
+        <gravitee-common.version>4.6.0</gravitee-common.version>`;
    expect(isOnlyGammaModuleVersionBump(diff)).toBe(false);
  });

  it('returns false for an empty diff', () => {
    expect(isOnlyGammaModuleVersionBump('')).toBe(false);
  });
});
