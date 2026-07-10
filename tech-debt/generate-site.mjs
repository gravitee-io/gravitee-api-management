#!/usr/bin/env node
/**
 * Generate a self-contained visual dashboard at tech-debt/site/index.html
 * from snapshots + debt-register.yaml. Open locally or host via GitHub Pages.
 */
import fs from "node:fs";
import path from "node:path";
import {
  TECH_DEBT_DIR,
  loadAllSnapshots,
  loadDebtRegister,
  loadLatestSnapshot,
} from "./lib.mjs";

const SITE_DIR = path.join(TECH_DEBT_DIR, "site");
const OUT = path.join(SITE_DIR, "index.html");

const KPI_META = {
  legacy_wrappers: {
    label: "LegacyWrappers",
    unit: "files",
    lowerIsBetter: true,
  },
  angularjs_files: {
    label: "AngularJS pages",
    unit: "files",
    lowerIsBetter: true,
  },
  e2e_mapi_v1: {
    label: "mapi-v1 e2e",
    unit: "specs",
    lowerIsBetter: true,
  },
  e2e_mapi_v2: {
    label: "mapi-v2 e2e",
    unit: "specs",
    lowerIsBetter: false,
  },
  dual_portal: {
    label: "Dual portal",
    unit: "",
    lowerIsBetter: true,
  },
};

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function buildHtml(payload) {
  const dataJson = JSON.stringify(payload).replace(/</g, "\\u003c");

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>APIM Structural Debt</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,700&family=Source+Sans+3:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500&display=swap" rel="stylesheet" />
  <style>
    :root {
      --bg: #e9edf2;
      --bg-accent: #dfe6ee;
      --ink: #15202b;
      --muted: #5b6b7c;
      --faint: #8a97a6;
      --surface: #fbfcfd;
      --line: #c9d3de;
      --brand: #d0122d;
      --ok: #1f7a4c;
      --warn: #9a6700;
      --info: #0b6e99;
      --radius: 14px;
      --shadow: 0 1px 0 rgba(21, 32, 43, 0.04);
      --font-display: "Fraunces", Georgia, serif;
      --font-body: "Source Sans 3", system-ui, sans-serif;
      --font-mono: "IBM Plex Mono", ui-monospace, monospace;
    }

    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; }
    body {
      font-family: var(--font-body);
      color: var(--ink);
      background:
        radial-gradient(1200px 500px at 10% -10%, #f7fafc 0%, transparent 55%),
        radial-gradient(900px 400px at 100% 0%, #e4ebf3 0%, transparent 50%),
        var(--bg);
      min-height: 100vh;
      line-height: 1.5;
    }

    .wrap {
      width: min(1120px, calc(100% - 32px));
      margin: 0 auto;
      padding: 40px 0 72px;
    }

    header.hero {
      display: grid;
      gap: 18px;
      margin-bottom: 28px;
    }

    .eyebrow {
      font-size: 12px;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: var(--brand);
      font-weight: 600;
    }

    h1 {
      font-family: var(--font-display);
      font-weight: 700;
      font-size: clamp(2rem, 4vw, 3rem);
      line-height: 1.1;
      margin: 0;
      letter-spacing: -0.02em;
    }

    .lede {
      max-width: 62ch;
      color: var(--muted);
      font-size: 1.05rem;
      margin: 0;
    }

    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: end;
      justify-content: space-between;
      padding: 14px 16px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      box-shadow: var(--shadow);
    }

    .controls {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: end;
    }

    label.field {
      display: grid;
      gap: 6px;
      font-size: 12px;
      color: var(--faint);
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    select {
      font: 500 14px var(--font-body);
      color: var(--ink);
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 10px;
      padding: 8px 12px;
      min-width: 150px;
    }

    .seg {
      display: inline-flex;
      padding: 3px;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: #f3f6f9;
      gap: 2px;
    }

    .seg button {
      border: 0;
      background: transparent;
      color: var(--muted);
      font: 600 13px var(--font-body);
      padding: 7px 12px;
      border-radius: 999px;
      cursor: pointer;
    }

    .seg button[aria-pressed="true"] {
      background: var(--ink);
      color: #fff;
    }

    .meta {
      font-family: var(--font-mono);
      font-size: 12px;
      color: var(--faint);
      text-align: right;
    }

    .stats {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin: 22px 0;
    }

    @media (max-width: 900px) {
      .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }

    .stat {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      padding: 16px 16px 14px;
      box-shadow: var(--shadow);
      min-height: 110px;
    }

    .stat .label {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--faint);
      font-weight: 600;
    }

    .stat .value {
      font-family: var(--font-display);
      font-size: 2rem;
      font-weight: 700;
      margin-top: 8px;
      letter-spacing: -0.03em;
    }

    .stat .delta {
      margin-top: 6px;
      font-size: 13px;
      font-weight: 600;
    }

    .delta.improving { color: var(--ok); }
    .delta.worsening { color: var(--brand); }
    .delta.flat { color: var(--faint); }

    section.block {
      margin-top: 28px;
    }

    section.block h2 {
      font-family: var(--font-display);
      font-size: 1.45rem;
      margin: 0 0 12px;
      letter-spacing: -0.02em;
    }

    .panel {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      box-shadow: var(--shadow);
      overflow: hidden;
    }

    .chart {
      padding: 18px 18px 8px;
    }

    .chart svg { width: 100%; height: 220px; display: block; }
    .legend {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      padding: 0 18px 16px;
      font-size: 13px;
      color: var(--muted);
    }
    .legend span { display: inline-flex; align-items: center; gap: 6px; }
    .swatch {
      width: 10px; height: 10px; border-radius: 999px; display: inline-block;
    }

    table.data {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }

    table.data th,
    table.data td {
      padding: 12px 14px;
      border-bottom: 1px solid var(--line);
      text-align: left;
      vertical-align: top;
    }

    table.data th {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--faint);
      background: #f4f7fa;
    }

    table.data tr:last-child td { border-bottom: 0; }
    table.data td.num { font-family: var(--font-mono); font-size: 13px; }
    code, .mono { font-family: var(--font-mono); font-size: 0.92em; }

    .cards {
      display: grid;
      gap: 12px;
    }

    .card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      padding: 18px 18px 16px;
      box-shadow: var(--shadow);
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: start;
      margin-bottom: 8px;
    }

    .card h3 {
      margin: 0;
      font-size: 1.05rem;
      font-weight: 700;
    }

    .pill {
      display: inline-flex;
      align-items: center;
      border-radius: 999px;
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.02em;
      text-transform: uppercase;
      border: 1px solid var(--line);
      background: #f3f6f9;
      color: var(--muted);
      white-space: nowrap;
    }

    .pill.improving { color: var(--ok); background: #e8f6ee; border-color: #b7e0c7; }
    .pill.stalled { color: var(--warn); background: #fff6e0; border-color: #f0d48a; }
    .pill.contain { color: var(--info); background: #e8f5fb; border-color: #a9d5e8; }

    .card p {
      margin: 8px 0;
      color: var(--muted);
      font-size: 0.98rem;
    }

    .card .hint {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px dashed var(--line);
      color: var(--ink);
      font-size: 0.95rem;
    }

    .hint strong { color: var(--brand); font-weight: 700; }

    .bars { display: grid; gap: 14px; padding: 18px; }
    .bar-row { display: grid; gap: 6px; }
    .bar-labels {
      display: flex; justify-content: space-between;
      font-size: 13px; color: var(--muted);
    }
    .track {
      height: 10px; border-radius: 999px; background: #e4ebf2; overflow: hidden;
    }
    .fill {
      height: 100%; border-radius: 999px; background: var(--ink);
    }
    .fill.ok { background: var(--ok); }
    .fill.info { background: var(--info); }
    .fill.warn { background: var(--warn); }

    .callout {
      margin-top: 18px;
      padding: 14px 16px;
      border-left: 3px solid var(--brand);
      background: #fff;
      border-radius: 0 var(--radius) var(--radius) 0;
      color: var(--muted);
      font-size: 0.95rem;
    }

    footer {
      margin-top: 36px;
      color: var(--faint);
      font-size: 13px;
    }

    footer a { color: var(--ink); }

    [hidden] { display: none !important; }
  </style>
</head>
<body>
  <div class="wrap">
    <header class="hero">
      <div class="eyebrow">Gravitee APIM · enablement</div>
      <h1>Structural debt</h1>
      <p class="lede">
        Coexistence and migration KPIs Sonar does not track, shown next to
        imported SonarCloud summaries — so you can ideate and pick work in one place.
      </p>
    </header>

    <div class="toolbar">
      <div class="controls">
        <label class="field">Snapshot
          <select id="snapshotSelect"></select>
        </label>
        <label class="field">Compare to
          <select id="compareSelect"></select>
        </label>
        <label class="field">View
          <div class="seg" role="group" aria-label="View filter">
            <button type="button" data-view="structural" aria-pressed="false">Without Sonar</button>
            <button type="button" data-view="sonar" aria-pressed="false">Sonar only</button>
            <button type="button" data-view="both" aria-pressed="true">With Sonar</button>
          </div>
        </label>
      </div>
      <div class="meta" id="metaLine"></div>
    </div>

    <div id="structuralRoot">
      <div class="stats" id="statGrid"></div>

      <section class="block">
        <h2>Trend</h2>
        <div class="panel">
          <div class="chart" id="trendChart"></div>
          <div class="legend" id="trendLegend"></div>
        </div>
      </section>

      <section class="block">
        <h2>Migration progress</h2>
        <div class="panel">
          <div class="bars" id="progressBars"></div>
        </div>
      </section>

      <section class="block">
        <h2>Debt register</h2>
        <div class="cards" id="registerCards"></div>
      </section>
    </div>

    <div id="sonarRoot" hidden>
      <section class="block">
        <h2>SonarCloud summary</h2>
        <div class="callout" id="sonarCallout"></div>
        <div class="stats" id="sonarStats" style="margin-top:16px"></div>
        <div class="panel" style="margin-top:12px">
          <table class="data" id="sonarTable">
            <thead>
              <tr>
                <th>Project</th>
                <th>Coverage</th>
                <th>Smells</th>
                <th>Bugs</th>
                <th>Vulns</th>
                <th>Duplication</th>
              </tr>
            </thead>
            <tbody></tbody>
          </table>
        </div>
      </section>
    </div>

    <footer>
      Markdown twin:
      <a href="../TECH-DEBT-REPORT.md">TECH-DEBT-REPORT.md</a>
      · How to contribute:
      <a href="../README.md">tech-debt/README.md</a>
      · Deep dives:
      <a href="https://sonarcloud.io/organizations/gravitee-io/projects">SonarCloud</a>
    </footer>
  </div>

  <script id="debt-data" type="application/json">${dataJson}</script>
  <script>
    const DATA = JSON.parse(document.getElementById("debt-data").textContent);
    const KPI_META = ${JSON.stringify(KPI_META)};
    const COLORS = {
      legacy_wrappers: "#15202b",
      angularjs_files: "#9a6700",
      e2e_mapi_v1: "#d0122d",
      e2e_mapi_v2: "#0b6e99",
    };

    let view = "both";
    let snapshotDate = DATA.latestDate;
    let compareDate = DATA.previousDate || "";

    const snapshotSelect = document.getElementById("snapshotSelect");
    const compareSelect = document.getElementById("compareSelect");

    function byDate(date) {
      return DATA.snapshots.find((s) => s.date === date);
    }

    function fmt(kpi, value) {
      if (kpi === "dual_portal") return value ? "yes" : "no";
      return value == null ? "—" : String(value);
    }

    function trend(kpi, curr, prev) {
      if (prev == null || prev === undefined) return { label: "no baseline", cls: "flat" };
      if (kpi === "dual_portal") {
        if (curr === prev) return { label: "flat", cls: "flat" };
        return curr
          ? { label: "still dual", cls: "worsening" }
          : { label: "resolved", cls: "improving" };
      }
      const d = Number(curr) - Number(prev);
      if (d === 0) return { label: "flat", cls: "flat" };
      const lowerBetter = KPI_META[kpi].lowerIsBetter;
      const improving = lowerBetter ? d < 0 : d > 0;
      const sign = d > 0 ? "+" : "";
      return {
        label: sign + d + " vs compare",
        cls: improving ? "improving" : "worsening",
      };
    }

    function fillSelects() {
      const dates = DATA.snapshots.map((s) => s.date).slice().reverse();
      snapshotSelect.innerHTML = dates
        .map((d) => '<option value="' + d + '">' + d + "</option>")
        .join("");
      compareSelect.innerHTML =
        '<option value="">None</option>' +
        dates
          .map((d) => '<option value="' + d + '">' + d + "</option>")
          .join("");
      snapshotSelect.value = snapshotDate;
      compareSelect.value = compareDate;
    }

    function renderMeta(current) {
      document.getElementById("metaLine").textContent =
        current.repoVersion + " · " + current.commit + " · " + current.date;
    }

    function renderStats(current, previous) {
      const keys = [
        "legacy_wrappers",
        "angularjs_files",
        "e2e_mapi_v1",
        "e2e_mapi_v2",
      ];
      document.getElementById("statGrid").innerHTML = keys
        .map((kpi) => {
          const curr = current.structural[kpi];
          const prev = previous ? previous.structural[kpi] : undefined;
          const t = trend(kpi, curr, prev);
          return (
            '<div class="stat">' +
            '<div class="label">' +
            KPI_META[kpi].label +
            "</div>" +
            '<div class="value">' +
            fmt(kpi, curr) +
            "</div>" +
            '<div class="delta ' +
            t.cls +
            '">' +
            t.label +
            "</div>" +
            "</div>"
          );
        })
        .join("");
    }

    function renderTrend() {
      const seriesKeys = [
        "legacy_wrappers",
        "angularjs_files",
        "e2e_mapi_v1",
        "e2e_mapi_v2",
      ];
      const points = DATA.snapshots.slice(-12);
      const w = 1000;
      const h = 220;
      const pad = { t: 16, r: 16, b: 36, l: 40 };
      const innerW = w - pad.l - pad.r;
      const innerH = h - pad.t - pad.b;
      const values = points.flatMap((p) =>
        seriesKeys.map((k) => Number(p.structural[k] || 0)),
      );
      const max = Math.max(10, ...values) * 1.1;
      const min = 0;
      const x = (i) =>
        pad.l + (points.length <= 1 ? innerW / 2 : (i * innerW) / (points.length - 1));
      const y = (v) => pad.t + innerH - ((v - min) / (max - min)) * innerH;

      const paths = seriesKeys
        .map((key) => {
          const d = points
            .map((p, i) => {
              const v = Number(p.structural[key] || 0);
              return (i === 0 ? "M" : "L") + x(i).toFixed(1) + " " + y(v).toFixed(1);
            })
            .join(" ");
          return (
            '<path d="' +
            d +
            '" fill="none" stroke="' +
            COLORS[key] +
            '" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />'
          );
        })
        .join("");

      const labels = points
        .map((p, i) => {
          if (points.length > 6 && i !== 0 && i !== points.length - 1 && i % 2 === 1) {
            return "";
          }
          return (
            '<text x="' +
            x(i).toFixed(1) +
            '" y="' +
            (h - 12) +
            '" text-anchor="middle" fill="#8a97a6" font-size="11" font-family="IBM Plex Mono, monospace">' +
            p.date.slice(5) +
            "</text>"
          );
        })
        .join("");

      document.getElementById("trendChart").innerHTML =
        '<svg viewBox="0 0 ' + w + " " + h + '" role="img" aria-label="Structural KPI trend">' +
        '<line x1="' + pad.l + '" x2="' + (w - pad.r) + '" y1="' + (pad.t + innerH) + '" y2="' + (pad.t + innerH) + '" stroke="#c9d3de" />' +
        paths +
        labels +
        "</svg>";

      document.getElementById("trendLegend").innerHTML = seriesKeys
        .map(
          (k) =>
            '<span><i class="swatch" style="background:' +
            COLORS[k] +
            '"></i>' +
            KPI_META[k].label +
            "</span>",
        )
        .join("");
    }

    function renderProgress(current) {
      const st = current.structural;
      const e2eTotal = Number(st.e2e_mapi_v1 || 0) + Number(st.e2e_mapi_v2 || 0);
      const rows = [
        {
          label: "LegacyWrappers remaining",
          right: st.legacy_wrappers + " files",
          pct: Math.min(100, Number(st.legacy_wrappers) || 0),
          // invert visual: full bar = more debt remaining
          cls: "warn",
          fill: Math.min(100, (Number(st.legacy_wrappers) / 80) * 100),
        },
        {
          label: "AngularJS pages remaining",
          right: st.angularjs_files + " files",
          fill: Math.min(100, (Number(st.angularjs_files) / 50) * 100),
          cls: "warn",
        },
        {
          label: "e2e on mapi-v2",
          right: st.e2e_mapi_v2 + " / " + e2eTotal,
          fill: e2eTotal ? (Number(st.e2e_mapi_v2) / e2eTotal) * 100 : 0,
          cls: "info",
        },
      ];

      document.getElementById("progressBars").innerHTML = rows
        .map(
          (r) =>
            '<div class="bar-row">' +
            '<div class="bar-labels"><span>' +
            r.label +
            "</span><span>" +
            r.right +
            "</span></div>" +
            '<div class="track"><div class="fill ' +
            r.cls +
            '" style="width:' +
            r.fill.toFixed(1) +
            '%"></div></div>' +
            "</div>",
        )
        .join("");
    }

    function renderRegister(current) {
      document.getElementById("registerCards").innerHTML = DATA.register.items
        .map((item) => {
          const value = current.structural[item.kpi];
          return (
            '<article class="card">' +
            '<div class="card-top">' +
            "<div>" +
            "<h3>" +
            escapeHtml(item.title) +
            "</h3>" +
            '<div class="mono" style="color:#8a97a6;margin-top:4px;font-size:12px">' +
            escapeHtml(item.id) +
            " · " +
            escapeHtml(item.kpi) +
            " = <strong>" +
            escapeHtml(fmt(item.kpi, value)) +
            "</strong></div>" +
            "</div>" +
            '<span class="pill ' +
            escapeHtml(item.status) +
            '">' +
            escapeHtml(item.status) +
            "</span>" +
            "</div>" +
            "<p>" +
            escapeHtml(item.why) +
            "</p>" +
            '<div class="hint"><strong>Pick this:</strong> ' +
            escapeHtml(item.pick_hint) +
            "</div>" +
            "</article>"
          );
        })
        .join("");
    }

    function escapeHtml(value) {
      return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
    }

    function renderSonar(current) {
      const sonar = current.sonar || {};
      const callout = document.getElementById("sonarCallout");
      const stats = document.getElementById("sonarStats");
      const tbody = document.querySelector("#sonarTable tbody");

      if (!sonar.available) {
        callout.textContent =
          "Sonar summary unavailable — " +
          (sonar.reason || "not fetched") +
          ". Structural KPIs above still refresh. Set SONAR_TOKEN on the weekly workflow to import coverage, smells, bugs, and duplication.";
        stats.innerHTML = "";
        tbody.innerHTML = "";
        return;
      }

      callout.textContent =
        "Imported from SonarCloud (not re-analyzed here). Use SonarCloud for PR decoration and issue drill-down.";
      const agg = sonar.aggregate || {};
      stats.innerHTML = [
        ["Coverage avg", (agg.coverage ?? "—") + "%"],
        ["Code smells", agg.codeSmells ?? "—"],
        ["Bugs", agg.bugs ?? "—"],
        ["Vulnerabilities", agg.vulnerabilities ?? "—"],
      ]
        .map(
          ([label, value]) =>
            '<div class="stat"><div class="label">' +
            label +
            '</div><div class="value">' +
            value +
            "</div></div>",
        )
        .join("");

      tbody.innerHTML = (sonar.projects || [])
        .map((p) => {
          return (
            "<tr>" +
            "<td>" +
            escapeHtml(p.label || p.key) +
            '</td><td class="num">' +
            (p.coverage ?? "—") +
            '%</td><td class="num">' +
            (p.codeSmells ?? "—") +
            '</td><td class="num">' +
            (p.bugs ?? "—") +
            '</td><td class="num">' +
            (p.vulnerabilities ?? "—") +
            '</td><td class="num">' +
            (p.duplication ?? "—") +
            "%</td></tr>"
          );
        })
        .join("");
    }

    function applyView() {
      const showStructural = view === "structural" || view === "both";
      const showSonar = view === "sonar" || view === "both";
      document.getElementById("structuralRoot").hidden = !showStructural;
      document.getElementById("sonarRoot").hidden = !showSonar;
      document.querySelectorAll(".seg button").forEach((btn) => {
        btn.setAttribute("aria-pressed", String(btn.dataset.view === view));
      });
    }

    function render() {
      const current = byDate(snapshotDate) || DATA.snapshots[DATA.snapshots.length - 1];
      const previous = compareDate ? byDate(compareDate) : null;
      renderMeta(current);
      renderStats(current, previous);
      renderTrend();
      renderProgress(current);
      renderRegister(current);
      renderSonar(current);
      applyView();
    }

    snapshotSelect.addEventListener("change", () => {
      snapshotDate = snapshotSelect.value;
      render();
    });
    compareSelect.addEventListener("change", () => {
      compareDate = compareSelect.value;
      render();
    });
    document.querySelectorAll(".seg button").forEach((btn) => {
      btn.addEventListener("click", () => {
        view = btn.dataset.view;
        render();
      });
    });

    fillSelects();
    render();
  </script>
</body>
</html>`;
}

function main() {
  const snapshots = loadAllSnapshots();
  if (snapshots.length === 0) {
    throw new Error("No snapshots in tech-debt/metrics. Run collect-metrics.mjs first.");
  }
  const latest = loadLatestSnapshot();
  const register = loadDebtRegister();
  const previous =
    snapshots.filter((s) => s.date < latest.date).at(-1) || null;

  const payload = {
    latestDate: latest.date,
    previousDate: previous?.date || "",
    register,
    snapshots,
    generatedAt: new Date().toISOString(),
  };

  fs.mkdirSync(SITE_DIR, { recursive: true });
  fs.writeFileSync(OUT, buildHtml(payload), "utf8");
  console.log(`Wrote ${OUT}`);
}

main();
