export interface Config {
  graviteeGammaBaseUrl: string;
  graviteeAppId: string;
  graviteeApiKey: string;
  geminiApiKey: string;
}

const REQUIRED_VARS = [
  ["GRAVITEE_GAMMA_BASE_URL", "graviteeGammaBaseUrl"],
  ["GRAVITEE_APP_ID", "graviteeAppId"],
  ["GRAVITEE_API_KEY", "graviteeApiKey"],
  ["GEMINI_API_KEY", "geminiApiKey"],
] as const;

export function loadConfig(): Config {
  const missing = REQUIRED_VARS.filter(([env]) => !process.env[env]).map(([env]) => env);

  if (missing.length > 0) {
    console.error(
      `[gravitee-catalog] Missing required environment variables:\n${missing.map((v) => `  - ${v}`).join("\n")}`,
    );
    process.exit(1);
  }

  return Object.fromEntries(
    REQUIRED_VARS.map(([env, key]) => [key, process.env[env]!]),
  ) as unknown as Config;
}
