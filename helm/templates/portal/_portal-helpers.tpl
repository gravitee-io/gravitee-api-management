{{/*
Portal helper split:
- portal.httpRoute.* are strict helpers for rendering the Portal HTTPRoute resource only
- portal.route.* and portal.portalUrl are shared discovery helpers for active Portal consumers
*/}}

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
{{- $path := include "portal.route.path" . -}}
{{- if gt (len $hosts) 0 -}}
{{ printf "https://%s%s" (index $hosts 0) (regexFind "/[a-zA-Z0-9-\\/_.~]+" $path) }}
{{- end -}}
{{- end -}}
