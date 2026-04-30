export interface Config {
  /** e.g. http://localhost:8083/portal/environments/DEFAULT */
  portalApiBaseUrl: string;
  portalApiUsername: string;
  portalApiPassword: string;
}

export function loadConfig(): Config {
  const portalApiBaseUrl = process.env["PORTAL_API_BASE_URL"]?.trim().replace(/\/+$/, "");
  const portalApiUsername = process.env["PORTAL_API_USERNAME"]?.trim();
  const portalApiPassword = process.env["PORTAL_API_PASSWORD"]?.trim();

  const missing: string[] = [];
  if (!portalApiBaseUrl) missing.push("PORTAL_API_BASE_URL");
  if (!portalApiUsername) missing.push("PORTAL_API_USERNAME");
  if (!portalApiPassword) missing.push("PORTAL_API_PASSWORD");

  if (missing.length > 0) {
    console.error(
      `[gravitee-catalog] Missing required environment variables:\n${missing.map((v) => `  - ${v}`).join("\n")}`,
    );
    process.exit(1);
  }

  return {
    portalApiBaseUrl: portalApiBaseUrl!,
    portalApiUsername: portalApiUsername!,
    portalApiPassword: portalApiPassword!,
  };
}
