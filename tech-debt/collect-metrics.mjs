#!/usr/bin/env node
/**
 * Collect structural debt KPIs for Gravitee APIM.
 * Writes metrics/YYYY-MM-DD.json and metrics/latest.json
 */
import {
  countFiles,
  countPathsContaining,
  ensureMetricsDir,
  gitHeadSha,
  pathExists,
  readRepoVersion,
  snapshotPaths,
  todayUtcDate,
  writeJson,
  loadLatestSnapshot,
  REPO_ROOT,
} from "./lib.mjs";

function collectStructural() {
  const legacyWrappers = countFiles(".", "*LegacyWrapper.java");
  const angularjsFiles = countFiles(
    "gravitee-apim-console-webui",
    "*.ajs.ts",
  );
  const e2eMapiV1 = countPathsContaining("gravitee-apim-e2e", "mapi-v1");
  const e2eMapiV2 = countPathsContaining("gravitee-apim-e2e", "mapi-v2");
  const dualPortal =
    pathExists("gravitee-apim-portal-webui") &&
    pathExists("gravitee-apim-portal-webui-next");

  return {
    legacy_wrappers: legacyWrappers,
    angularjs_files: angularjsFiles,
    e2e_mapi_v1: e2eMapiV1,
    e2e_mapi_v2: e2eMapiV2,
    dual_portal: dualPortal,
  };
}

function main() {
  if (!pathExists("pom.xml")) {
    console.error(
      `Expected APIM repo root with pom.xml at ${REPO_ROOT}. Aborting.`,
    );
    process.exit(1);
  }

  ensureMetricsDir();
  const date = todayUtcDate();
  const structural = collectStructural();

  let previousSonar = undefined;
  try {
    const previous = loadLatestSnapshot();
    if (previous.sonar) previousSonar = previous.sonar;
  } catch {
    // first run
  }

  const snapshot = {
    date,
    commit: gitHeadSha(),
    repoVersion: readRepoVersion(),
    structural,
  };
  if (previousSonar) {
    snapshot.sonar = previousSonar;
  }

  const { dated, latest } = snapshotPaths(date);
  writeJson(dated, snapshot);
  writeJson(latest, snapshot);

  console.log(`Wrote ${dated}`);
  console.log(`Wrote ${latest}`);
  console.log(JSON.stringify(structural, null, 2));
}

main();
