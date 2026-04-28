{{/*
Gamma UI helpers split:

1. STRICT HTTPRoute helpers (gammaUi.httpRoute.*)
   Used ONLY by gammaUi-httproute.yaml.
   They read ONLY from .Values.gammaUi.httpRoute.*
   Required fields fail loudly when missing.

2. EFFECTIVE route helpers (gammaUi.route.*) and URL builders
   (gammaUi.baseHref, gammaUi.consoleUrl, gammaUi.apiUrl)
   Used by sibling components (configmap, deployment, api configmap).
   Precedence is INGRESS-FIRST:
     - If gammaUi.ingress.enabled=true  → use ingress values
     - Else if gammaUi.httpRoute.enabled=true → use httpRoute values
     - Else → safe default
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "gammaUi.httpRoute.path" -}}
{{- required "gammaUi.httpRoute.path is required when gammaUi.httpRoute.enabled=true" .Values.gammaUi.httpRoute.path -}}
{{- end -}}

{{- define "gammaUi.httpRoute.pathMatchType" -}}
{{- required "gammaUi.httpRoute.pathMatchType is required when gammaUi.httpRoute.enabled=true" .Values.gammaUi.httpRoute.pathMatchType -}}
{{- end -}}

{{- define "gammaUi.httpRoute.hostnames" -}}
{{- $hostnames := default (list) .Values.gammaUi.httpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "gammaUi.httpRoute.hostnames must contain at least one hostname when gammaUi.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{- define "gammaUi.route.path" -}}
{{- if and .Values.gammaUi.ingress.enabled .Values.gammaUi.ingress.path -}}
{{- .Values.gammaUi.ingress.path -}}
{{- else if and .Values.gammaUi.httpRoute.enabled .Values.gammaUi.httpRoute.path -}}
{{- .Values.gammaUi.httpRoute.path -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "gammaUi.route.hostnames" -}}
{{- if and .Values.gammaUi.ingress.enabled .Values.gammaUi.ingress.hosts -}}
{{ toYaml .Values.gammaUi.ingress.hosts }}
{{- else if and .Values.gammaUi.httpRoute.enabled .Values.gammaUi.httpRoute.hostnames -}}
{{ toYaml .Values.gammaUi.httpRoute.hostnames }}
{{- else -}}
{{ toYaml (list "apim.example.com") }}
{{- end -}}
{{- end -}}

{{- define "gammaUi.route.scheme" -}}
{{- if .Values.gammaUi.ingress.enabled -}}
https
{{- else if .Values.gammaUi.httpRoute.enabled -}}
{{- default "https" .Values.gammaUi.httpRoute.scheme -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "gammaUi.baseHref" -}}
{{- $path := include "gammaUi.route.path" . -}}
{{- $baseHref := regexFind "/[a-zA-Z0-9-\\/_.~]*" $path -}}
{{- if or (eq $baseHref "") (eq $baseHref "/") -}}
/
{{- else -}}
{{ printf "%s/" (trimSuffix "/" $baseHref) }}
{{- end -}}
{{- end -}}

{{- define "gammaUi.consoleUrl" -}}
{{- $hosts := fromYamlArray (include "gammaUi.route.hostnames" .) -}}
{{- $scheme := include "gammaUi.route.scheme" . -}}
{{- $path := include "gammaUi.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}

{{- define "gammaUi.apiUrl" -}}
{{- $url := include "api.gamma.route.url" . -}}
{{- if ne $url "" -}}
{{ printf "%s/" (trimSuffix "/" $url) }}
{{- end -}}
{{- end -}}