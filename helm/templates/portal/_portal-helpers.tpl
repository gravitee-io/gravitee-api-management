{{/*
Portal helpers split:

1. STRICT HTTPRoute helpers (portal.httpRoute.*)
   Used ONLY by portal-httproute.yaml.
   They read ONLY from .Values.portal.httpRoute.*
   Required fields fail loudly when missing — no fallback to ingress.

2. EFFECTIVE route helpers (portal.route.*) and URL builders (portal.portalUrl,
   portal.route.baseHref).
   Used by sibling components (configmap, deployment env vars).
   Precedence is INGRESS-FIRST:
     - If portal.ingress.enabled=true  → use ingress values
     - Else if portal.httpRoute.enabled=true → use httpRoute values
     - Else → safe default
*/}}

{{/* ============================================================ */}}
{{/* Strict HTTPRoute helpers (no ingress fallback, fail on missing) */}}
{{/* ============================================================ */}}

{{- define "portal.httpRoute.path" -}}
{{- required "portal.httpRoute.path is required when portal.httpRoute.enabled=true" .Values.portal.httpRoute.path -}}
{{- end -}}

{{- define "portal.httpRoute.pathMatchType" -}}
{{- required "portal.httpRoute.pathMatchType is required when portal.httpRoute.enabled=true" .Values.portal.httpRoute.pathMatchType -}}
{{- end -}}

{{- define "portal.httpRoute.hostnames" -}}
{{- $hostnames := default (list) .Values.portal.httpRoute.hostnames -}}
{{- if gt (len $hostnames) 0 -}}
{{ toYaml $hostnames }}
{{- else -}}
{{- fail "portal.httpRoute.hostnames must contain at least one hostname when portal.httpRoute.enabled=true" -}}
{{- end -}}
{{- end -}}

{{/* ============================================================ */}}
{{/* Effective route helpers (ingress-first, used by sibling components) */}}
{{/* ============================================================ */}}

{{- define "portal.route.path" -}}
{{- if .Values.portal.ingress.enabled -}}
{{- default "/" .Values.portal.ingress.path -}}
{{- else if (default false ((.Values.portal).httpRoute).enabled) -}}
{{- default "/" .Values.portal.httpRoute.path -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "portal.route.hostnames" -}}
{{- if .Values.portal.ingress.enabled -}}
{{ toYaml (default (list) .Values.portal.ingress.hosts) }}
{{- else if (default false ((.Values.portal).httpRoute).enabled) -}}
{{ toYaml (default (list) .Values.portal.httpRoute.hostnames) }}
{{- else -}}
[]
{{- end -}}
{{- end -}}

{{- define "portal.route.scheme" -}}
{{- if .Values.portal.ingress.enabled -}}
{{- default "https" .Values.portal.ingress.scheme -}}
{{- else if (default false ((.Values.portal).httpRoute).enabled) -}}
{{- default "https" .Values.portal.httpRoute.scheme -}}
{{- else -}}
https
{{- end -}}
{{- end -}}

{{- define "portal.route.baseHref" -}}
{{- $path := include "portal.route.path" . -}}
{{- $baseHref := regexFind "/[a-zA-Z0-9-\\/_.~]*" $path -}}
{{- if or (eq $baseHref "") (eq $baseHref "/") -}}
/
{{- else -}}
{{ printf "%s/" (trimSuffix "/" $baseHref) }}
{{- end -}}
{{- end -}}

{{- define "portal.portalUrl" -}}
{{- $hosts := fromYamlArray (include "portal.route.hostnames" .) -}}
{{- $scheme := include "portal.route.scheme" . -}}
{{- $path := include "portal.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" $scheme (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]*" $path) }}
{{- end -}}
{{- end -}}
