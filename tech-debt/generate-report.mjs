#!/usr/bin/env node
/**
 * Generate TECH-DEBT-REPORT.md from snapshots + debt-register.yaml
 */
import fs from "node:fs";
import path from "node:path";
import {
  TECH_DEBT_DIR,
  loadAllSnapshots,
  loadDebtRegister,
  loadLatestSnapshot,
} from "./lib.mjs";

const REPORT_PATH = path.join(TECH_DEBT_DIR, "TECH-DEBT-REPORT.md");

const KPI_LABELS = {
  legacy_wrappers: "LegacyWrapper files",
  angularjs_files: "AngularJS *.ajs.ts files",
  e2e_mapi_v1: "e2e specs on mapi-v1",
  e2e_mapi_v2: "e2e specs on mapi-v2",
  dual_portal: "Dual portal UIs present",
};

function formatValue(kpi, value) {
  if (kpi === "dual_portal") return value ? "yes" : "no";
  return String(value);
}

function delta(curr, prev, kpi) {
  if (prev === undefined || prev === null) return { text: "—", arrow: "" };
  if (kpi === "dual_portal") {
    if (curr === prev) return { text: "0", arrow: "→" };
    return { text: curr ? "+on" : "off", arrow: curr ? "↑" : "↓" };
  }
  const d = Number(curr) - Number(prev);
  if (d === 0) return { text: "0", arrow: "→" };
  const sign = d > 0 ? "+" : "";
  return { text: `${sign}${d}`, arrow: d < 0 ? "↓" : "↑" };
}

function trendArrow(kpi, curr, prev) {
  if (prev === undefined || prev === null) return "—";
  if (kpi === "dual_portal") return curr === prev ? "flat" : curr ? "regressed" : "improved";
  const d = Number(curr) - Number(prev);
  if (d === 0) return "flat";
  if (kpi === "e2e_mapi_v2") return d > 0 ? "improving" : "worsening";
  return d < 0 ? "improving" : "worsening";
}

function previousSnapshot(snapshots, currentDate) {
  const older = snapshots.filter((s) => s.date < currentDate);
  return older.length ? older[older.length - 1] : null;
}

function renderStructuralTable(current, previous) {
  const rows = Object.keys(KPI_LABELS).map((kpi) => {
    const curr = current.structural?.[kpi];
    const prev = previous?.structural?.[kpi];
    const d = delta(curr, prev, kpi);
    return `| ${KPI_LABELS[kpi]} | \`${kpi}\` | ${formatValue(kpi, curr)} | ${d.text} | ${trendArrow(kpi, curr, prev)} |`;
  });
  return [
    "| KPI | id | Current | Δ vs previous | Trend |",
    "|-----|----|---------|---------------|-------|",
    ...rows,
  ].join("\n");
}

function renderSonarSection(sonar) {
  if (!sonar || sonar.available === false) {
    const reason = sonar?.reason || "not fetched yet";
    return [
      "## SonarCloud summary (imported)",
      "",
      `**Unavailable** — ${reason}`,
      "",
      "Set `SONAR_TOKEN` and run `node tech-debt/fetch-sonar-summary.mjs` (or wait for the weekly workflow).",
      "Deep dives remain on [SonarCloud / gravitee-io](https://sonarcloud.io/organizations/gravitee-io/projects).",
      "",
    ].join("\n");
  }

  const agg = sonar.aggregate || {};
  const projectRows = (sonar.projects || []).map((p) => {
    const label = p.label || p.key;
    return `| ${label} | ${fmtNum(p.coverage)}% | ${fmtNum(p.codeSmells)} | ${fmtNum(p.bugs)} | ${fmtNum(p.vulnerabilities)} | ${fmtNum(p.duplication)}% |`;
  });

  return [
    "## SonarCloud summary (imported)",
    "",
    "_Imported from SonarCloud — not re-analyzed here. Use SonarCloud for PR decoration and issue drill-down._",
    "",
    `| Metric | Aggregate |`,
    `|--------|-----------|`,
    `| Coverage (avg %) | ${fmtNum(agg.coverage)} |`,
    `| Code smells (sum) | ${fmtNum(agg.codeSmells)} |`,
    `| Bugs (sum) | ${fmtNum(agg.bugs)} |`,
    `| Vulnerabilities (sum) | ${fmtNum(agg.vulnerabilities)} |`,
    `| Duplication (avg %) | ${fmtNum(agg.duplication)} |`,
    "",
    `Fetched: ${sonar.fetchedAt || "—"}`,
    "",
    "### By project",
    "",
    "| Project | Coverage | Smells | Bugs | Vulns | Duplication |",
    "|---------|----------|--------|------|-------|-------------|",
    ...projectRows,
    "",
  ].join("\n");
}

function fmtNum(v) {
  if (v === null || v === undefined || Number.isNaN(v)) return "—";
  return String(v);
}

function renderRegister(register, current) {
  const rows = register.items.map((item) => {
    const kpi = item.kpi;
    const value = current.structural?.[kpi];
    return [
      `### \`${item.id}\` — ${item.title}`,
      "",
      `- **KPI:** \`${kpi}\` = **${formatValue(kpi, value)}**`,
      `- **Status:** ${item.status}`,
      `- **Why it hurts:** ${item.why}`,
      `- **Pick hint:** ${item.pick_hint}`,
      "",
    ].join("\n");
  });
  return ["## Debt register", "", ...rows].join("\n");
}

function renderTrend(snapshots) {
  const recent = snapshots.slice(-12);
  if (recent.length === 0) return "## Trend\n\n_No snapshots yet._\n";

  const header =
    "| Date | LegacyWrappers | AngularJS | mapi-v1 | mapi-v2 | Dual portal |";
  const sep = "|------|----------------|-----------|---------|---------|-------------|";
  const rows = recent.map((s) => {
    const st = s.structural || {};
    return `| ${s.date} | ${st.legacy_wrappers ?? "—"} | ${st.angularjs_files ?? "—"} | ${st.e2e_mapi_v1 ?? "—"} | ${st.e2e_mapi_v2 ?? "—"} | ${st.dual_portal ? "yes" : "no"} |`;
  });
  return ["## Trend (last snapshots)", "", header, sep, ...rows, ""].join(
    "\n",
  );
}

function main() {
  const current = loadLatestSnapshot();
  const snapshots = loadAllSnapshots();
  const previous = previousSnapshot(snapshots, current.date);
  const register = loadDebtRegister();

  const md = [
    "# APIM Structural Debt Report",
    "",
    `Generated from snapshot **${current.date}** (commit \`${current.commit}\`, version \`${current.repoVersion}\`).`,
    previous ? `Compared to previous snapshot **${previous.date}**.` : "First snapshot — no previous comparison.",
    "",
    "## What this is",
    "",
    "A living register of **structural / coexistence debt** in Gravitee APIM,",
    "shown **next to imported SonarCloud summaries** so you can ideate and pick",
    "work in one place. This does **not** replace SonarCloud analysis or remediate debt.",
    "",
    "See [README.md](./README.md) for how to refresh and how to contribute.",
    "",
    "## Structural debt — at a glance",
    "",
    renderStructuralTable(current, previous),
    "",
    renderSonarSection(current.sonar),
    renderRegister(register, current),
    renderTrend(snapshots),
    "## How to contribute",
    "",
    "1. Pick a register `id` whose pick hint matches your team.",
    "2. Open an issue or PR referencing that id (example: `hexagonal-legacy-wrappers`).",
    "3. After merge, the next weekly refresh (or a local run) will move the KPI.",
    "",
  ].join("\n");

  fs.writeFileSync(REPORT_PATH, md, "utf8");
  console.log(`Wrote ${REPORT_PATH}`);
}

main();
