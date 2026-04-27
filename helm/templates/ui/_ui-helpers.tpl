{{/*
UI helpers split:

1. STRICT HTTPRoute helpers (ui.httpRoute.*)
   Used ONLY by ui-httproute.yaml.
   They read ONLY from .Values.ui.httpRoute.*
   Required fields fail loudly when missing — no fallback to ingress.

2. EFFECTIVE route helpers (ui.route.*) and URL builders (ui.consoleUrl,
   ui.consoleBaseHref, ui.managementApiUrl, ui.portalEntrypointUrl)
   Used by sibling components (configmap, deployment env vars).
   Precedence is INGRESS-FIRST:
     - If ui.ingress.enabled=true  → use ingress values
     - Else if ui.httpRoute.enabled=true → use httpRoute values
     - Else → safe default
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "ui.httpRoute.path" -}}
{{- required "ui.httpRoute.path is required when ui.httpRoute.enabled=true" .Values.ui.httpRoute.path -}}
{{- end -}}

{{- define "ui.httpRoute.pathMatchType" -}}
{{- required "ui.httpRoute.pathMatchType is required when ui.httpRoute.enabled=true" .Values.ui.httpRoute.pathMatchType -}}
{{- end -}}

{{- define "ui.httpRoute.hostnames" -}}
{{- $hostnames := default (list) .Values.ui.httpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "ui.httpRoute.hostnames must contain at least one hostname when ui.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{- define "ui.route.path" -}}
{{- if and .Values.ui.ingress.enabled .Values.ui.ingress.path -}}
{{- .Values.ui.ingress.path -}}
{{- else if and .Values.ui.httpRoute.enabled .Values.ui.httpRoute.path -}}
{{- .Values.ui.httpRoute.path -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "ui.route.hostnames" -}}
{{- if and .Values.ui.ingress.enabled .Values.ui.ingress.hosts -}}
{{ toYaml .Values.ui.ingress.hosts }}
{{- else if and .Values.ui.httpRoute.enabled .Values.ui.httpRoute.hostnames -}}
{{ toYaml .Values.ui.httpRoute.hostnames }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "ui.route.scheme" -}}
{{- if .Values.ui.ingress.enabled -}}
{{- default "https" .Values.ui.ingress.scheme -}}
{{- else if .Values.ui.httpRoute.enabled -}}
{{- default "https" .Values.ui.httpRoute.scheme -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "ui.consoleBaseHref" -}}
{{- $path := include "ui.route.path" . -}}
{{- $baseHref := regexFind "/[a-zA-Z0-9-\\/_.~]*" $path -}}
{{- if or (eq $baseHref "") (eq $baseHref "/") -}}
/
{{- else -}}
{{ printf "%s/" (trimSuffix "/" $baseHref) }}
{{- end -}}
{{- end -}}

{{- define "ui.consoleUrl" -}}
{{- $hosts := fromYamlArray (include "ui.route.hostnames" .) -}}
{{- $scheme := include "ui.route.scheme" . -}}
{{- $path := include "ui.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}

{{- define "ui.managementApiUrl" -}}
{{- include "api.management.route.url" . -}}
{{- end -}}

{{- define "ui.portalEntrypointUrl" -}}
{{- include "gateway.route.portalEntrypointUrl" . -}}
{{- end -}}
