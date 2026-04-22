{{/*
UI helper split:
- ui.httpRoute.* are strict helpers for rendering the UI HTTPRoute resource only
- ui.route.* and URL/baseHref helpers are shared discovery helpers for active UI consumers
*/}}

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

{{- define "ui.route.path" -}}
{{- if and .Values.ui.ingress.enabled .Values.ui.ingress.path -}}
{{- .Values.ui.ingress.path -}}
{{- else if and .Values.ui.httpRoute.enabled .Values.ui.httpRoute.path -}}
{{- .Values.ui.httpRoute.path -}}
{{- else -}}
/
{{- end -}}
{{- end -}}

{{- define "ui.route.pathMatchType" -}}
{{- if and .Values.ui.ingress.enabled (eq (default "Prefix" .Values.ui.ingress.pathType) "ImplementationSpecific") -}}
RegularExpression
{{- else if and .Values.ui.ingress.enabled (eq (default "Prefix" .Values.ui.ingress.pathType) "Prefix") -}}
PathPrefix
{{- else if and .Values.ui.httpRoute.enabled .Values.ui.httpRoute.pathMatchType -}}
{{- .Values.ui.httpRoute.pathMatchType -}}
{{- else -}}
PathPrefix
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
{{- $path := include "ui.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "https://%s%s" (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]+" $path) }}
{{- end -}}
{{- end -}}

{{- define "ui.managementApiUrl" -}}
{{- $url := include "api.management.route.url" . -}}
{{- if $url -}}
{{ $url }}
{{- else -}}
{{- $management := default dict .Values.api.ingress.management -}}
{{- $hosts := default (list) (get $management "hosts") -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "%s://%s%s" (default "https" (get $management "scheme")) (index $hosts 0) (default "/" (get $management "path")) }}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "ui.portalEntrypointUrl" -}}
{{- include "gateway.route.portalEntrypointUrl" . -}}
{{- end -}}