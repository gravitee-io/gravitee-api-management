#!/usr/bin/env zx

const { magenta, blue, red, green } = chalk;
const { log } = console;
const { stat } = fs;
const { abs } = Math;

const eol = '\n';
const graviteeDownloadURL = 'https://download.gravitee.io/graviteeio-apim/distributions';
const mainBranch = 'master';

log(magenta(`########################################`));
log(magenta(`# 📦 Checking APIM bundle file size 📦 #`));
log(magenta(`########################################`));

const branch = await getBranch();
const supportVersion = await getSupportVersion(branch);

const currentVersion = await getCurrentVersion();
const previousVersion = await getPreviousVersion();

const tmpDir = path.join(__dirname, '..', '.tmp');
const bundleDir = path.join(tmpDir, currentVersion, 'dist', 'distributions');
const bundleFile = path.join(bundleDir, `graviteeio-full-${currentVersion}.zip`);
const slackMessageFile = path.join(tmpDir, 'bundle-size.md');

log(blue(`Comparing bundle size with version ${previousVersion}`));

await assertBundleExists();

const previousBundleSize = await getBundleSize(previousVersion);
const diff = await getDiff(bundleFile, previousBundleSize);

log(blue(`Saving slack message to ${slackMessageFile}`));

const message = buildDiffMessage(diff);

log(message);

await $`echo ${message} > ${slackMessageFile}`;

function buildDiffMessage(diff) {
  return `📦 APIM ${currentVersion} bundle file has ${diff.verb} by ${diff.percents} (${diff.size}) compared to version ${previousVersion}.`;
}

async function assertBundleExists() {
  try {
    await stat(bundleFile);
  } catch (error) {
    throw new Error(red('Unable to locate current bundle file. Did you run package-bundle.mjs ?'));
  }
}

async function getBranch() {
  log(blue('Getting current branch'));

  const branch = await $`git rev-parse --abbrev-ref HEAD`;
  return trim(branch);
}

async function getCurrentVersion() {
  log(blue(`Getting release tag`));

  const currentVersion = await $`git tag -l --sort -version:refname | grep -v alpha | grep ${supportVersion}| head -1`;
  return trim(currentVersion);
}

async function getPreviousVersion() {
  log(blue(`Getting previous tag for release`));

  const lastTags = await $`git tag -l --sort -version:refname | grep -v alpha | grep ${supportVersion}| head -2`;
  const tagList = trim(lastTags).split(eol);

  if (tagList.length === 2) {
    return tagList.pop();
  }

  log(blue(`No previous tag for ${supportVersion}. Assuming this is the first release.`));

  const previousVersion = await $`git tag -l --sort -version:refname | grep -v alpha | head -2 | tail -1`;
  return trim(previousVersion);
}

async function getSupportVersion(branch) {
  const match = branch.match(/(^\d+\.\d+)/);
  if (match === null) {
    throw new Error(red('Unable to get current support version. Are you on a support branch ?'));
  }
  const [minorVersion] = match;
  return minorVersion;
}

async function getBundleSize(version) {
  log(blue(`Getting bundle size for version ${version}`));

  const bundleFile = `graviteeio-full-${version}.zip`;
  const bundleUrl = `${graviteeDownloadURL}/${bundleFile}`;

  const { headers } = await fetch(bundleUrl, { method: 'HEAD' });

  return headers.get('content-length');
}

async function getFileSize(path) {
  const { size } = await stat(path);
  return size;
}

async function getDiff(bundleFile, previousBundleSize) {
  const bundleSize = await getFileSize(bundleFile);

  const diff = bundleSize - previousBundleSize;
  const size = toHumanReadableSize(abs(diff));
  const percents = abs((diff / previousBundleSize) * 100).toFixed(2) + ' %';
  const verb = diff < 0 ? 'decreased' : 'increased';

  return {
    percents,
    size,
    verb,
  };
}

function toHumanReadableSize(bytes) {
  const scale = bytes === 0 ? 0 : Math.floor(Math.log(bytes) / Math.log(1024));
  const divisor = Math.pow(1024, scale);
  const units = ['bytes', 'KB', 'MB', 'GB', 'TB'];
  const size = (bytes / divisor).toFixed(2);
  return `${size} ${units[scale]}`;
}

function trim(value) {
  return String(value).trim();
}
