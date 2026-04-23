{{/*
Gateway helpers split:

Two helper families exist for every gateway scope (top-level, services.bridge,
services.core/technical):

1. STRICT HTTPRoute helpers (gateway.<scope>.httpRoute.*)
   Used ONLY by the corresponding HTTPRoute manifest.
   They read ONLY from the httpRoute block.
   Required fields fail loudly when missing — no fallback to ingress.

2. EFFECTIVE route helpers (gateway.<scope>.route.*)
   Used by sibling components that need to know the effective host/path.
   Precedence is INGRESS-FIRST:
     - If <scope>.ingress.enabled=true  → use ingress values
     - Else if <scope>.httpRoute.enabled=true → use httpRoute values
     - Else → safe default

Per-server gateway routes (gateway.servers[i]) are not factored into helpers
because the range index cannot be passed cleanly to a Helm template; the same
strict rule is applied inline in gateway-httproute.yaml using `required`.
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "gateway.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- required "gateway.httpRoute.path is required when gateway.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- required "gateway.httpRoute.pathMatchType is required when gateway.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.httpRoute.hostnames must contain at least one hostname when gateway.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- required "gateway.services.bridge.httpRoute.path is required when gateway.services.bridge.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- required "gateway.services.bridge.httpRoute.pathMatchType is required when gateway.services.bridge.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.bridge.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.services.bridge.httpRoute.hostnames must contain at least one hostname when gateway.services.bridge.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.path" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- required "gateway.services.core.httpRoute.path is required when gateway.services.core.httpRoute.enabled=true" (get $hr "path") -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.pathMatchType" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- required "gateway.services.core.httpRoute.pathMatchType is required when gateway.services.core.httpRoute.enabled=true" (get $hr "pathMatchType") -}}
{{- end -}}

{{- define "gateway.technical.httpRoute.hosts" -}}
{{- $hr := default dict .Values.gateway.services.core.httpRoute -}}
{{- $hostnames := default (list) (get $hr "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gateway.services.core.httpRoute.hostnames must contain at least one hostname when gateway.services.core.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{/* --- Top-level gateway --- */}}

{{- define "gateway.route.path" -}}
{{- $ingress := default dict .Values.gateway.ingress -}}
{{- $httpRoute := default dict .Values.gateway.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/" (get $httpRoute "path") -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "gateway.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.ingress -}}
{{- $httpRoute := default dict .Values.gateway.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Bridge (gateway.services.bridge) --- */}}

{{- define "gateway.bridge.route.path" -}}
{{- $ingress := default dict .Values.gateway.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/_bridge" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/_bridge" (get $httpRoute "path") -}}
{{- else -}}
/_bridge
{{- end -}}
{{- end -}}

{{- define "gateway.bridge.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.services.bridge.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.bridge.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{/* --- Technical (gateway.services.core) --- */}}

{{- define "gateway.technical.route.path" -}}
{{- $ingress := default dict .Values.gateway.services.core.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.core.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{- default "/" (get $ingress "path") -}}
{{- else if default false (get $httpRoute "enabled") -}}
{{- default "/" (get $httpRoute "path") -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "gateway.technical.route.hosts" -}}
{{- $ingress := default dict .Values.gateway.services.core.ingress -}}
{{- $httpRoute := default dict .Values.gateway.services.core.httpRoute -}}
{{- if default false (get $ingress "enabled") -}}
{{ toYaml (default (list) (get $ingress "hosts")) }}
{{- else if default false (get $httpRoute "enabled") -}}
{{ toYaml (default (list) (get $httpRoute "hostnames")) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "gateway.route.schemeForRoute" -}}
{{- $route := default dict .route -}}
{{- default "https" (get $route "scheme") -}}
{{- end -}}

{{/*
Builds the portal entrypoint URL from gateway routing configuration.
Logic:
- Phase 1: iterate gateway.servers[] — for each server, check ingress first (if enabled with hosts),
  then httpRoute (if enabled with hostnames). First match wins.
- Phase 2: check top-level gateway.ingress (if enabled with hosts).
- Phase 3: check top-level gateway.httpRoute (if enabled with hostnames).
Returns the URL as https://host/path or empty string if no route is found.
*/}}
{{- define "gateway.route.portalEntrypointUrl" -}}
{{- $result := dict "url" "" -}}
{{- $servers := default (list) .Values.gateway.servers -}}
{{- range $server := $servers -}}
{{- if eq (default "" $server.type) "http" -}}
{{- $serverIngress := default dict $server.ingress -}}
{{- if and (eq (get $result "url") "") (default false $serverIngress.enabled) -}}
{{- $hosts := default (list) $serverIngress.hosts -}}
{{- if gt (len $hosts) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $serverIngress) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hosts 0) (default "/" $serverIngress.path)) -}}
{{- end -}}
{{- end -}}
{{- $serverHttpRoute := default dict $server.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false $serverHttpRoute.enabled) -}}
{{- $hostnames := default (list) $serverHttpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $serverHttpRoute) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hostnames 0) (default "/" $serverHttpRoute.path)) -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- if eq (len $servers) 0 -}}
{{- $gatewayIngress := default dict .Values.gateway.ingress -}}
{{- if and (eq (get $result "url") "") (default false (get $gatewayIngress "enabled")) -}}
{{- $hosts := default (list) (get $gatewayIngress "hosts") -}}
{{- if gt (len $hosts) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $gatewayIngress) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hosts 0) (default "/" (get $gatewayIngress "path"))) -}}
{{- end -}}
{{- end -}}
{{- $gatewayHttpRoute := default dict .Values.gateway.httpRoute -}}
{{- if and (eq (get $result "url") "") (default false (get $gatewayHttpRoute "enabled")) -}}
{{- $hostnames := default (list) (get $gatewayHttpRoute "hostnames") -}}
{{- if gt (len $hostnames) 0 -}}
{{- $scheme := include "gateway.route.schemeForRoute" (dict "route" $gatewayHttpRoute) -}}
{{- $_ := set $result "url" (printf "%s://%s%s" $scheme (index $hostnames 0) (default "/" (get $gatewayHttpRoute "path"))) -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- get $result "url" -}}
{{- end -}}
