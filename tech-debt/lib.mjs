import { execFileSync, execSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export const TECH_DEBT_DIR = __dirname;
export const REPO_ROOT = path.resolve(TECH_DEBT_DIR, "..");
export const METRICS_DIR = path.join(TECH_DEBT_DIR, "metrics");

export const SONAR_PROJECTS = [
  {
    key: "gravitee-io_gravitee-api-management_rest-api",
    label: "rest-api",
  },
  {
    key: "gravitee-io_gravitee-api-management_gateway",
    label: "gateway",
  },
  {
    key: "gravitee-io_gravitee-api-management_repository",
    label: "repository",
  },
  {
    key: "gravitee-io_gravitee-api-management_definition",
    label: "definition",
  },
  {
    key: "gravitee-io_gravitee-api-management_plugins",
    label: "plugins",
  },
  {
    key: "gravitee-io_gravitee-api-management_reporter",
    label: "reporter",
  },
  {
    key: "gravitee-io_gravitee-api-management_console-webui",
    label: "console-webui",
  },
  {
    key: "gravitee-io_gravitee-api-management_portal-webui",
    label: "portal-webui",
  },
  { key: "gravitee-apim-portal-webui-next", label: "portal-webui-next" },
];

const IGNORE_DIR_NAMES = new Set([
  "node_modules",
  "target",
  ".git",
  "dist",
  "coverage",
  ".yarn",
  ".nx",
  "build",
]);

export function todayUtcDate() {
  return new Date().toISOString().slice(0, 10);
}

export function gitHeadSha() {
  try {
    return execSync("git rev-parse --short HEAD", {
      cwd: REPO_ROOT,
      encoding: "utf8",
    }).trim();
  } catch {
    return "unknown";
  }
}

export function readRepoVersion() {
  const pom = path.join(REPO_ROOT, "pom.xml");
  const text = fs.readFileSync(pom, "utf8");
  const match = text.match(/<revision>([^<]+)<\/revision>/);
  return match ? match[1] : "unknown";
}

export function ensureMetricsDir() {
  fs.mkdirSync(METRICS_DIR, { recursive: true });
}

export function snapshotPaths(date) {
  return {
    dated: path.join(METRICS_DIR, `${date}.json`),
    latest: path.join(METRICS_DIR, "latest.json"),
  };
}

export function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

export function writeJson(filePath, value) {
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

export function loadLatestSnapshot() {
  const latest = path.join(METRICS_DIR, "latest.json");
  if (!fs.existsSync(latest)) {
    throw new Error(`Missing ${latest}. Run collect-metrics.mjs first.`);
  }
  return readJson(latest);
}

export function listSnapshotFiles() {
  if (!fs.existsSync(METRICS_DIR)) return [];
  return fs
    .readdirSync(METRICS_DIR)
    .filter((name) => /^\d{4}-\d{2}-\d{2}\.json$/.test(name))
    .sort();
}

export function loadAllSnapshots() {
  return listSnapshotFiles().map((name) =>
    readJson(path.join(METRICS_DIR, name)),
  );
}

function hasRg() {
  try {
    execFileSync("rg", ["--version"], { stdio: "ignore" });
    return true;
  } catch {
    return false;
  }
}

function walkFiles(rootDir, predicate, acc = []) {
  if (!fs.existsSync(rootDir)) return acc;
  for (const entry of fs.readdirSync(rootDir, { withFileTypes: true })) {
    if (IGNORE_DIR_NAMES.has(entry.name)) continue;
    const full = path.join(rootDir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(full, predicate, acc);
    } else if (entry.isFile() && predicate(full, entry.name)) {
      acc.push(full);
    }
  }
  return acc;
}

/**
 * Count files matching a glob-like pattern under a relative directory.
 * Uses ripgrep when available; otherwise a Node walker.
 *
 * @param {string} relativeDir directory under repo root
 * @param {string} fileGlob e.g. "*LegacyWrapper.java" or "*.ajs.ts"
 */
export function countFiles(relativeDir, fileGlob) {
  const absDir = path.join(REPO_ROOT, relativeDir);
  if (!fs.existsSync(absDir)) return 0;

  if (hasRg()) {
    try {
      const out = execFileSync(
        "rg",
        ["--files", "-g", fileGlob, "--glob", "!node_modules", absDir],
        { encoding: "utf8", cwd: REPO_ROOT },
      );
      return out
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean).length;
    } catch (err) {
      // rg exits 1 when no matches
      if (err && err.status === 1) return 0;
      throw err;
    }
  }

  const matcher = globToRegExp(fileGlob);
  return walkFiles(absDir, (_full, name) => matcher.test(name)).length;
}

/**
 * Count files whose path contains a substring (e.g. "mapi-v1").
 */
export function countPathsContaining(relativeDir, needle) {
  const absDir = path.join(REPO_ROOT, relativeDir);
  if (!fs.existsSync(absDir)) return 0;

  if (hasRg()) {
    try {
      const out = execFileSync("rg", ["--files", absDir], {
        encoding: "utf8",
        cwd: REPO_ROOT,
      });
      return out
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.includes(needle)).length;
    } catch (err) {
      if (err && err.status === 1) return 0;
      throw err;
    }
  }

  return walkFiles(absDir, (full) => full.includes(needle)).length;
}

export function pathExists(relativePath) {
  return fs.existsSync(path.join(REPO_ROOT, relativePath));
}

function globToRegExp(glob) {
  const escaped = glob
    .replace(/[.+^${}()|[\]\\]/g, "\\$&")
    .replace(/\*/g, ".*");
  return new RegExp(`^${escaped}$`);
}

/**
 * Minimal YAML loader for debt-register.yaml (list of maps under `items:`).
 * Avoids adding a dependency for a tiny curated file.
 */
export function loadDebtRegister() {
  const filePath = path.join(TECH_DEBT_DIR, "debt-register.yaml");
  const text = fs.readFileSync(filePath, "utf8");
  const items = [];
  let current = null;
  let multilineKey = null;
  let multilineLines = [];

  const flushMultiline = () => {
    if (current && multilineKey) {
      current[multilineKey] = multilineLines
        .join(" ")
        .replace(/\s+/g, " ")
        .trim();
    }
    multilineKey = null;
    multilineLines = [];
  };

  for (const rawLine of text.split("\n")) {
    const line = rawLine.replace(/\s+$/, "");
    if (!line.trim() || line.trim().startsWith("#")) continue;

    const itemStart = line.match(/^\s*-\s+id:\s*(.+)\s*$/);
    if (itemStart) {
      flushMultiline();
      current = { id: stripQuotes(itemStart[1]) };
      items.push(current);
      continue;
    }

    if (!current) continue;

    const folded = line.match(/^\s{4}(\w+):\s*>\s*$/);
    if (folded) {
      flushMultiline();
      multilineKey = folded[1];
      multilineLines = [];
      continue;
    }

    if (multilineKey && /^\s{6}\S/.test(line)) {
      multilineLines.push(line.trim());
      continue;
    }

    const simple = line.match(/^\s{4}(\w+):\s*(.+)\s*$/);
    if (simple) {
      flushMultiline();
      current[simple[1]] = stripQuotes(simple[2]);
    }
  }
  flushMultiline();
  return { items };
}

function stripQuotes(value) {
  const v = value.trim();
  if (
    (v.startsWith('"') && v.endsWith('"')) ||
    (v.startsWith("'") && v.endsWith("'"))
  ) {
    return v.slice(1, -1);
  }
  return v;
}
