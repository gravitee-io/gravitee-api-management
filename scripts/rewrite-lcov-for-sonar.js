/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

const fs = require('fs');
const path = require('path');

// 1. Get and validate the input
const projectPath = process.argv[2];
if (!projectPath) {
  console.warn('[Coverage] Usage: node scripts/rewrite-lcov-for-sonar.js <project-path>');
  process.exit(0);
}

// Block directory traversal attempts
if (projectPath.includes('..') || path.isAbsolute(projectPath)) {
  console.error(`[Coverage] Error: Invalid project path provided: ${projectPath}`);
  process.exit(1);
}

// 2. Resolve and verify the canonical path
const repoRoot = process.cwd();
const lcovPath = path.resolve(repoRoot, projectPath, 'coverage', 'lcov.info');

if (!fs.existsSync(lcovPath)) {
  console.warn(`[Coverage] lcov.info not found at ${lcovPath}. Skipping path rewrite.`);
  process.exit(0);
}

// 3. Read the file
let lcovFile = fs.readFileSync(lcovPath, 'utf8');

// Prepare path prefix to match against SF: entries in lcov format (which uses forward slashes)
const projectRootPath = projectPath.replace(/\\/g, '/') + (projectPath.endsWith('/') ? '' : '/');

// 4. Check if we actually need to rewrite
if (!lcovFile.includes(`SF:${projectRootPath}`)) {
  console.log(`[Coverage] Paths in lcov.info are already relative or don't match ${projectRootPath}. No changes made.`);
  process.exit(0);
}

// 5. Rewrite and save (Happens strictly ONCE)
lcovFile = lcovFile.split(`SF:${projectRootPath}`).join('SF:');
fs.writeFileSync(lcovPath, lcovFile, 'utf8');
console.log(`[Coverage] Successfully updated lcov.info paths to be relative to ${projectPath}`);
