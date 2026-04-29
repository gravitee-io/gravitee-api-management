export interface Config {
  graviteeGammaBaseUrl: string;
  graviteeAppId: string;
  graviteeApiKey: string;
  /** e.g. http://127.0.0.1:11434 — no trailing path */
  ollamaBaseUrl: string;
  /** e.g. llama3.2:1b */
  ollamaModel: string;
}

const REQUIRED_VARS = [
  ["GRAVITEE_GAMMA_BASE_URL", "graviteeGammaBaseUrl"],
  ["GRAVITEE_APP_ID", "graviteeAppId"],
  ["GRAVITEE_API_KEY", "graviteeApiKey"],
] as const;

const DEFAULT_OLLAMA_BASE_URL = "http://127.0.0.1:11434";
const DEFAULT_OLLAMA_MODEL = "llama3.2:1b";

export function loadConfig(): Config {
  const missing = REQUIRED_VARS.filter(([env]) => !process.env[env]).map(([env]) => env);

  if (missing.length > 0) {
    console.error(
      `[gravitee-catalog] Missing required environment variables:\n${missing.map((v) => `  - ${v}`).join("\n")}`,
    );
    process.exit(1);
  }

  const base: Record<string, string> = Object.fromEntries(
    REQUIRED_VARS.map(([env, key]) => [key, process.env[env]!]),
  ) as unknown as Record<string, string>;

  return {
    graviteeGammaBaseUrl: base["graviteeGammaBaseUrl"]!,
    graviteeAppId: base["graviteeAppId"]!,
    graviteeApiKey: base["graviteeApiKey"]!,
    ollamaBaseUrl: (process.env["OLLAMA_BASE_URL"]?.trim() || DEFAULT_OLLAMA_BASE_URL).replace(
      /\/+$/,
      "",
    ),
    ollamaModel: (process.env["OLLAMA_MODEL"]?.trim() || DEFAULT_OLLAMA_MODEL),
  };
}
