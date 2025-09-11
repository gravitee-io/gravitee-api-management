#!/usr/bin/env node
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
/**
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

/**
 * Script to build gravitee-markdown library for console-webui
 * This script navigates to portal-webui-next, installs dependencies, and builds the library
 * Cross-platform Node.js implementation that works on both Windows and Unix-like systems
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

console.log('🚀   Building gravitee-markdown library for console-webui...');

// Get the directory where this script is located
const scriptDir = __dirname;

// Navigate to portal-webui-next directory
const portalDir = path.resolve(scriptDir, '../../gravitee-apim-portal-webui-next');

if (!fs.existsSync(portalDir)) {
  console.error(`❌  Error: portal-webui-next directory not found at ${portalDir}`);
  process.exit(1);
}

console.log('📁  Navigating to portal-webui-next directory...');
process.chdir(portalDir);

console.log('📦  Installing dependencies...');
try {
  execSync('yarn install', { stdio: 'inherit' });
} catch (error) {
  console.error('❌  Error during yarn install:', error.message);
  process.exit(1);
}

console.log('🔨  Building gravitee-markdown library with console configuration...');
try {
  execSync('yarn build:gravitee-markdown:console', { stdio: 'inherit' });
} catch (error) {
  console.error('❌  Error during build:', error.message);
  process.exit(1);
}

console.log('✅  gravitee-markdown library built successfully!');
