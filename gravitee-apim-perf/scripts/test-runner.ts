#! ./node_modules/.bin/ts-node
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

import 'dotenv/config';
import { $, fs, chalk, ProcessOutput } from 'zx';
import { resolve } from 'path';
import { fetchGatewaySuccess, Logger } from '@gravitee/utils/gateway';
import { parse } from 'ts-command-line-args';
import { GatewayTestData } from '../lib/test-api';

const testSuffix = '.test.js';
const dist = 'dist';
const assets = 'dist/assets';
let debugEnabled = false;

interface ScriptArgs {
  file: string;
  verbose?: boolean;
  debug?: boolean;
}

const fakeLogger: Logger = {
  error(...data: any[]): void {},
  info(...data: any[]): void {},
};

const K6_COMMAND = [
  `K6_PROMETHEUS_REMOTE_URL=${process.env.K6_PROMETHEUS_REMOTE_URL}`,
  './bin/k6',
  'run',
  `-e GATEWAY_BASE_URL=${process.env.GATEWAY_BASE_URL}`,
  `-e SKIP_TLS_VERIFY=${process.env.SKIP_TLS_VERIFY}`,
  `-e API_ENDPOINT_URL=${process.env.API_ENDPOINT_URL}`,
  `-o ${process.env.K6_OUTPUT_MODE}`,
];

let TEARDOWN_ENV_VAR = '';

function initDistFolder() {
  if (!fs.existsSync(dist)) {
    fs.mkdirSync(dist);
  }
}

void (async function () {
  const { args, filePath } = readCommandArgs();
  if (filePath && filePath.endsWith(testSuffix)) {
    initDistFolder();
    // Get relative path of the test from 'src'
    const test = extractTestRelativePathFromSrc(filePath);
    const setupTestFilePath = `src/${test}.setup.ts`;
    const hasSetupFile = fs.existsSync(setupTestFilePath);
    let setupTestFiles;
    if (hasSetupFile) {
      setupTestFiles = await prepareTestConfiguration(setupTestFilePath, test);
      let { dataAsString, data } = await runInitHook(setupTestFiles.initPath);
      await waitForApiAvailability(data);
      const testDataPath = writeTestData(test, dataAsString);
      const absoluteTestDataPath = resolve(testDataPath);
      enrichK6CommandWithParameters(absoluteTestDataPath, testDataPath);
    }
    await runK6Test(test, args);

    if (hasSetupFile) {
      await runTearDownHook(test, setupTestFiles.tearDownPath);
    }
  } else {
    error('Missing arg: Should be `./test-runner.ts -f src/use-case.test.js`');
  }
})();

function readCommandArgs() {
  const args = parse<ScriptArgs>({
    // @ts-ignore
    file: { type: String, alias: 'f' },
    verbose: { type: Boolean, alias: 'v', optional: true },
    debug: { type: Boolean, alias: 'd', optional: true },
  });
  $.verbose = args.verbose;
  debugEnabled = args.debug;
  const filePath = args.file;
  return { args, filePath };
}

function extractTestRelativePathFromSrc(filePath: string) {
  return filePath.replace('src/', '').replace(testSuffix, '');
}

/**
 * Compiles a typescript file using 'tsc' command
 * @param tsConfigFilePath is the tsconfig file to use for compilation
 */
async function compileTypescript(tsConfigFilePath: string) {
  debug(`🛠 Running typescript compilation: yarn tsc --project ${tsConfigFilePath}`);
  await $`yarn tsc --project ${tsConfigFilePath}`;
  debug(`🛠 ✅ Typescript compilation done`);
}

/**
 * Prepares test configuration by generating a dedicated tsconfig file, compile typescript for the test, and build hooks file (init and tearDown)
 * @param setupTestFilePath is the path to the setup.ts file
 * @param test is the relative path of the test
 */
async function prepareTestConfiguration(setupTestFilePath: string, test: string) {
  const tsConfigFilePath = buildTsConfig(setupTestFilePath, test);
  await compileTypescript(tsConfigFilePath);
  const setupTestFiles = buildHookFiles(test);
  return setupTestFiles;
}

/**
 * Builds a tsconfig file dedicated to the running test
 * @param setupFilePath is the path of the ${test}.setup.ts to include in the tsconfig file
 * @param test is the relative path of the test
 */
function buildTsConfig(setupFilePath: string, test: string): string {
  debug(`🛠 Building tsconfig file for ${test} with setup ${setupFilePath}`);
  const tsConfigBase = fs.readFileSync('tsconfig.setup.json');
  const tsConfigGenerated = JSON.parse(tsConfigBase.toString());
  tsConfigGenerated.compilerOptions.baseUrl = '../';
  tsConfigGenerated.compilerOptions.typeRoots.push('../');
  tsConfigGenerated.compilerOptions.outDir = '.';
  tsConfigGenerated.include = [`../${setupFilePath}`];
  // Impossible to use '/' in file name so replace it by an underscore
  const generateConfigPath = `dist/tsconfig.${replaceSlashByUnderscoreGlobally(test)}.json`;
  fs.writeFileSync(generateConfigPath, JSON.stringify(tsConfigGenerated));
  debug(`🛠 ✅ Tsconfig file built!`);
  return generateConfigPath;
}

/**
 * Builds the hook files (init and tearDown)
 * @param test is the relative path of the test
 */
function buildHookFiles(test: string) {
  debug(`🛠 Building hook files for ${test}`);
  const initPath = `dist/src/${test}.init.js`;
  const tearDownPath = `dist/src/${test}.tearDown.js`;
  const pathsToTest = test.split('/');
  let testName = pathsToTest.length > 0 ? pathsToTest[pathsToTest.length - 1] : test;
  fs.writeFileSync(
    initPath,
    `require('./${testName}.setup.js').init().then((d) => console.log(JSON.stringify(d, null, 2))).catch(console.error);`,
  );
  fs.writeFileSync(tearDownPath, `require('./${testName}.setup.js').tearDown().then(console.log).catch(console.error);`);

  debug(`🛠 ✅ Hook files built!`);
  return { initPath, tearDownPath };
}

/**
 * Runs the init hook ({test}.setup.ts#init()) and return {@link GatewayTestData} object.
 * @param initPath is the path of the {test}.init.js
 */
async function runInitHook(initPath: string) {
  info(`🪝 Init hook ${initPath}`);
  const dataAsString = output(
    await $`TS_NODE_BASEURL=./dist NODE_TLS_REJECT_UNAUTHORIZED="0" node --loader ts-node/esm -r tsconfig-paths/register ${initPath}`,
  );
  let data: GatewayTestData;
  try {
    // Check data format before k6 script
    data = JSON.parse(dataAsString);
  } catch (err) {
    error(err);
    throw err;
  }
  return { dataAsString, data };
}

/**
 * Runs the tear down hook ({test}.setup.ts#tearDown())
 * @param test is the relative path of the test
 * @param tearDownPath is the path of {test}.tearDown.js
 */
async function runTearDownHook(test: string, tearDownPath) {
  info(`Teardown hook ${test}`);
  debug(`Teardown path ${tearDownPath}`);
  output(
    await exec(`TS_NODE_BASEURL=./dist ${TEARDOWN_ENV_VAR} node --loader ts-node/esm -r tsconfig-paths/register ${tearDownPath}`, true),
  );
}

/**
 * Adds TEST_DATA_PATH to K6 command to use the data file written in assets folder
 * @param absoluteTestDataPath is the absolute path to relative test data written in assets folder
 * @param testDataPath the relative version of the path
 */
function enrichK6CommandWithParameters(absoluteTestDataPath: string, testDataPath: string) {
  debug(`Absolute test data path used by K6: ${absoluteTestDataPath}`);
  K6_COMMAND.push(`-e TEST_DATA_PATH=${absoluteTestDataPath}`);
  TEARDOWN_ENV_VAR += `TEST_DATA_PATH=${testDataPath.replace('dist/', '')}`;
}

/**
 * Runs K6 test
 * @param test is the test to run
 * @param args are the args used to run this scripts
 */
async function runK6Test<T>(test: string, args: ScriptArgs) {
  info('🏁 Run k6 test');
  const testFile = `src/${test}.test.js`;
  K6_COMMAND.push(testFile);
  await exec(K6_COMMAND.join(' '), true, { resetValue: args.verbose });
}

/**
 * Waits for API to be deployed by retrying calls until it answers with a 200 status
 * @param data from init phase
 */
async function waitForApiAvailability(data: GatewayTestData) {
  if (data.waitGateway) {
    await fetchGatewaySuccess({ contextPath: data.waitGateway.contextPath }, fakeLogger);
  }
}

/**
 * Executes a command
 * @param command to execute
 * @param unsafe
 * @param verbose activation
 */
async function exec(command: string, unsafe = false, verbose?: { resetValue: boolean }) {
  if (verbose != null) {
    $.verbose = true;
  }
  let quote;
  if (unsafe) {
    quote = $.quote;
    $.quote = (v) => v;
  }
  const output = await $`${command}`;
  if (unsafe) {
    $.quote = quote;
  }
  if (verbose != null) {
    $.verbose = verbose.resetValue;
  }
  return output;
}

/**
 * Write test data {@link GatewayTestData} into asset folder
 * @param test is the relative path of the test
 * @param data is the data built by the init phase
 */
function writeTestData(test: string, data: any) {
  if (!fs.existsSync(assets)) {
    fs.mkdirSync(assets);
  }
  const testDataPath = `${assets}/${test.replace(/\//g, '_')}.json`;
  fs.writeFileSync(testDataPath, data);
  info(`📁 File "${testDataPath}" written successfully !`);
  return testDataPath;
}

function debug(message) {
  if (debugEnabled) {
    console.log(chalk.grey(message));
  }
}

function info(message) {
  console.log(chalk.cyanBright(message));
}

function success(message) {
  console.log(chalk.green(message));
}

function error(message) {
  console.log(chalk.bold.red(message));
}

function warn(message) {
  console.log(chalk.hex('#FFA500')(message));
}

function output(processOutput: ProcessOutput): any {
  if (processOutput.exitCode === 1) {
    error(processOutput.stderr);
    warn(processOutput.stdout);
  } else {
    error(processOutput.stderr);
    success(processOutput.stdout);
  }

  return processOutput.stdout;
}

/**
 * Replaces all '/' in a string by '_'
 * @param test
 */
function replaceSlashByUnderscoreGlobally(test: string) {
  return test.replace(/\//g, '_');
}
