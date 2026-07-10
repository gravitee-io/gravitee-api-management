#!/usr/bin/env node
/**
 * Import SonarCloud summary metrics into the latest structural snapshot.
 * Best-effort: missing token or API errors do not fail the overall refresh.
 *
 * Env:
 *   SONAR_TOKEN  — SonarCloud token with browse permission (optional)
 *   SONAR_HOST   — default https://sonarcloud.io
 */
import {
  SONAR_PROJECTS,
  ensureMetricsDir,
  loadLatestSnapshot,
  snapshotPaths,
  todayUtcDate,
  writeJson,
} from "./lib.mjs";

const HOST = (process.env.SONAR_HOST || "https://sonarcloud.io").replace(
  /\/$/,
  "",
);
const TOKEN = process.env.SONAR_TOKEN || "";

const METRIC_KEYS = [
  "coverage",
  "bugs",
  "vulnerabilities",
  "code_smells",
  "duplicated_lines_density",
];

async function fetchProjectMeasures(projectKey) {
  const params = new URLSearchParams({
    component: projectKey,
    metricKeys: METRIC_KEYS.join(","),
  });
  const url = `${HOST}/api/measures/component?${params}`;
  const headers = { Accept: "application/json" };
  if (TOKEN) {
    headers.Authorization = `Bearer ${TOKEN}`;
  }

  const res = await fetch(url, { headers });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(
      `${projectKey}: HTTP ${res.status} ${res.statusText} — ${body.slice(0, 200)}`,
    );
  }
  const data = await res.json();
  const measures = Object.fromEntries(
    (data.component?.measures || []).map((m) => [m.metric, Number(m.value)]),
  );
  return {
    key: projectKey,
    coverage: measures.coverage ?? null,
    bugs: measures.bugs ?? null,
    vulnerabilities: measures.vulnerabilities ?? null,
    codeSmells: measures.code_smells ?? null,
    duplication: measures.duplicated_lines_density ?? null,
  };
}

function aggregate(projects) {
  const withCoverage = projects.filter((p) => typeof p.coverage === "number");
  const sum = (key) =>
    projects.reduce(
      (acc, p) => acc + (typeof p[key] === "number" ? p[key] : 0),
      0,
    );
  return {
    coverage:
      withCoverage.length === 0
        ? null
        : Number(
            (
              withCoverage.reduce((a, p) => a + p.coverage, 0) /
              withCoverage.length
            ).toFixed(1),
          ),
    codeSmells: sum("codeSmells"),
    bugs: sum("bugs"),
    vulnerabilities: sum("vulnerabilities"),
    duplication:
      withCoverage.length === 0
        ? null
        : Number(
            (
              projects
                .filter((p) => typeof p.duplication === "number")
                .reduce((a, p) => a + p.duplication, 0) /
              Math.max(
                1,
                projects.filter((p) => typeof p.duplication === "number")
                  .length,
              )
            ).toFixed(1),
          ),
  };
}

async function main() {
  ensureMetricsDir();
  const snapshot = loadLatestSnapshot();
  const date = snapshot.date || todayUtcDate();

  if (!TOKEN) {
    snapshot.sonar = {
      source: "sonarcloud",
      available: false,
      reason: "SONAR_TOKEN not set",
      fetchedAt: new Date().toISOString(),
      projects: [],
      aggregate: null,
    };
    const paths = snapshotPaths(date);
    writeJson(paths.dated, snapshot);
    writeJson(paths.latest, snapshot);
    console.warn(
      "SONAR_TOKEN not set — marked sonar unavailable; structural KPIs unchanged.",
    );
    return;
  }

  const projects = [];
  const errors = [];
  for (const project of SONAR_PROJECTS) {
    try {
      const measures = await fetchProjectMeasures(project.key);
      projects.push({ ...measures, label: project.label });
    } catch (err) {
      errors.push(String(err.message || err));
    }
  }

  if (projects.length === 0) {
    snapshot.sonar = {
      source: "sonarcloud",
      available: false,
      reason: errors.join("; ") || "no projects fetched",
      fetchedAt: new Date().toISOString(),
      projects: [],
      aggregate: null,
    };
  } else {
    snapshot.sonar = {
      source: "sonarcloud",
      available: true,
      fetchedAt: new Date().toISOString(),
      host: HOST,
      projects,
      aggregate: aggregate(projects),
      errors: errors.length ? errors : undefined,
    };
  }

  const paths = snapshotPaths(date);
  writeJson(paths.dated, snapshot);
  writeJson(paths.latest, snapshot);
  console.log(
    snapshot.sonar.available
      ? `Sonar summary imported for ${projects.length} projects.`
      : `Sonar unavailable: ${snapshot.sonar.reason}`,
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
