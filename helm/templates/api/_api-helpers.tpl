{{/*
API helpers split:

Two helper families exist for every API sub-component (management, portal,
automation, bridge, federation):

1. STRICT HTTPRoute helpers (api.<component>.httpRoute.*)
   Used ONLY by the corresponding HTTPRoute manifest.
   They read ONLY from .Values.api.httpRoute.<component>.*
   Required fields fail loudly when missing — no fallback to ingress.

2. EFFECTIVE route helpers (api.<component>.route.*)
   Used by sibling components (configmaps, deployment env vars, URL builders)
   that need to know the effective host/path for the API regardless of which
   exposure mechanism is enabled.
   Precedence is INGRESS-FIRST:
     - If <component>.ingress.enabled=true  → use ingress values
     - Else if <component>.httpRoute.enabled=true → use httpRoute values
     - Else → component-specific safe default

ingress.pathType -> HTTPRoute pathMatchType translation (effective helpers only):
  Prefix                 -> PathPrefix
  ImplementationSpecific -> RegularExpression
  Exact                  -> Exact (passthrough)
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "api.management.httpRoute.path" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- required "api.httpRoute.management.path is required when api.httpRoute.management.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.management.httpRoute.pathMatchType" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- required "api.httpRoute.management.pathMatchType is required when api.httpRoute.management.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.management.httpRoute.hosts" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.management.hostnames must contain at least one hostname when api.httpRoute.management.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.portal.httpRoute.path" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- required "api.httpRoute.portal.path is required when api.httpRoute.portal.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.portal.httpRoute.pathMatchType" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- required "api.httpRoute.portal.pathMatchType is required when api.httpRoute.portal.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.portal.httpRoute.hosts" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.portal.hostnames must contain at least one hostname when api.httpRoute.portal.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.gamma.httpRoute.path" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- required "api.httpRoute.gamma.path is required when api.httpRoute.gamma.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.gamma.httpRoute.pathMatchType" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- required "api.httpRoute.gamma.pathMatchType is required when api.httpRoute.gamma.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.gamma.httpRoute.hosts" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.gamma.hostnames must contain at least one hostname when api.httpRoute.gamma.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.automation.httpRoute.path" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "automation") -}}
{{- required "api.httpRoute.automation.path is required when api.httpRoute.automation.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.automation.httpRoute.pathMatchType" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "automation") -}}
{{- required "api.httpRoute.automation.pathMatchType is required when api.httpRoute.automation.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.automation.httpRoute.hosts" -}}
{{- $hr := default dict (get (default dict .Values.api.httpRoute) "automation") -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.httpRoute.automation.hostnames must contain at least one hostname when api.httpRoute.automation.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.bridge.httpRoute.path" -}}
{{- $hr := default dict .Values.api.services.bridge.httpRoute -}}
{{- required "api.services.bridge.httpRoute.path is required when api.services.bridge.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.bridge.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.api.services.bridge.httpRoute -}}
{{- required "api.services.bridge.httpRoute.pathMatchType is required when api.services.bridge.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.bridge.httpRoute.hosts" -}}
{{- $hr := default dict .Values.api.services.bridge.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.services.bridge.httpRoute.hostnames must contain at least one hostname when api.services.bridge.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "api.federation.httpRoute.path" -}}
{{- $hr := default dict .Values.api.federation.httpRoute -}}
{{- required "api.federation.httpRoute.path is required when api.federation.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "api.federation.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.api.federation.httpRoute -}}
{{- required "api.federation.httpRoute.pathMatchType is required when api.federation.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "api.federation.httpRoute.hosts" -}}
{{- $hr := default dict .Values.api.federation.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "api.federation.httpRoute.hostnames must contain at least one hostname when api.federation.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{/* --- Management --- */}}

{{- define "api.management.route.path" -}}
{{- $ingress := default dict .Values.api.ingress.management -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/management" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/management" (get $httpRoute "path") -}}
{{- else -}}
/management
{{- end -}}
{{- end -}}

{{- define "api.management.route.hosts" -}}
{{- $ingress := default dict .Values.api.ingress.management -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
{{ toYaml (list "apim.example.com") }}
{{- end -}}
{{- end -}}

{{- define "api.management.route.scheme" -}}
{{- $ingress := default dict .Values.api.ingress.management -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "management") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "https" (get $ingress "scheme") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "https" (get $httpRoute "scheme") -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "api.management.route.url" -}}
{{- $hosts := fromYamlArray (include "api.management.route.hosts" .) -}}
{{- $scheme := include "api.management.route.scheme" . -}}
{{- $path := include "api.management.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}

{{/* --- Portal --- */}}

{{- define "api.portal.route.path" -}}
{{- $ingress := default dict .Values.api.ingress.portal -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/portal" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/portal" (get $httpRoute "path") -}}
{{- else -}}
/portal
{{- end -}}
{{- end -}}

{{- define "api.portal.route.hosts" -}}
{{- $ingress := default dict .Values.api.ingress.portal -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
{{ toYaml (list "apim.example.com") }}
{{- end -}}
{{- end -}}

{{- define "api.portal.route.scheme" -}}
{{- $ingress := default dict .Values.api.ingress.portal -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "portal") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "https" (get $ingress "scheme") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "https" (get $httpRoute "scheme") -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "api.portal.route.url" -}}
{{- $hosts := fromYamlArray (include "api.portal.route.hosts" .) -}}
{{- $scheme := include "api.portal.route.scheme" . -}}
{{- $path := include "api.portal.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}

{{/* --- Gamma --- */}}

{{- define "api.gamma.route.path" -}}
{{- $ingress := default dict .Values.api.ingress.gamma -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/gamma" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/gamma" (get $httpRoute "path") -}}
{{- else -}}
/gamma
{{- end -}}
{{- end -}}

{{- define "api.gamma.route.hosts" -}}
{{- $ingress := default dict .Values.api.ingress.gamma -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
{{ toYaml (list "apim.example.com") }}
{{- end -}}
{{- end -}}

{{- define "api.gamma.route.scheme" -}}
{{- $ingress := default dict .Values.api.ingress.gamma -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "gamma") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "https" (get $ingress "scheme") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "https" (get $httpRoute "scheme") -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "api.gamma.route.url" -}}
{{- $hosts := fromYamlArray (include "api.gamma.route.hosts" .) -}}
{{- $scheme := include "api.gamma.route.scheme" . -}}
{{- $path := include "api.gamma.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}

{{/* --- Automation --- */}}

{{- define "api.automation.route.path" -}}
{{- $ingress := default dict .Values.api.ingress.automation -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "automation") -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/automation" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/automation" (get $httpRoute "path") -}}
{{- else -}}
/automation
{{- end -}}
{{- end -}}

{{- define "api.automation.route.hosts" -}}
{{- $ingress := default dict .Values.api.ingress.automation -}}
{{- $httpRoute := default dict (get (default dict .Values.api.httpRoute) "automation") -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Bridge (api.services.bridge) --- */}}

{{- define "api.bridge.route.path" -}}
{{- $ingress := default dict .Values.api.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.api.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/_bridge" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/_bridge" (get $httpRoute "path") -}}
{{- else -}}
/_bridge
{{- end -}}
{{- end -}}

{{- define "api.bridge.route.hosts" -}}
{{- $ingress := default dict .Values.api.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.api.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Federation (api.federation) --- */}}

{{- define "api.federation.route.path" -}}
{{- $ingress := default dict .Values.api.federation.ingress -}}
{{- $httpRoute := default dict .Values.api.federation.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/integration-controller" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/integration-controller" (get $httpRoute "path") -}}
{{- else -}}
/integration-controller
{{- end -}}
{{- end -}}

{{- define "api.federation.route.hosts" -}}
{{- $ingress := default dict .Values.api.federation.ingress -}}
{{- $httpRoute := default dict .Values.api.federation.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}
