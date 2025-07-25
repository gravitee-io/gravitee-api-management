{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "gravitee.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "gravitee.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified gateway name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "gravitee.gateway.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if .Values.fullnameOverride -}}
{{- printf "%s-%s" .Values.fullnameOverride .Values.gateway.name | trunc 63 | trimSuffix "-" -}}
{{- else if contains $name .Release.Name -}}
{{- printf "%s-%s" .Release.Name .Values.gateway.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s-%s" .Release.Name $name .Values.gateway.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified gateway name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "gravitee.api.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if .Values.fullnameOverride -}}
{{- printf "%s-%s" .Values.fullnameOverride .Values.api.name | trunc 63 | trimSuffix "-" -}}
{{- else if contains $name .Release.Name -}}
{{- printf "%s-%s" .Release.Name .Values.api.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s-%s" .Release.Name $name .Values.api.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified gateway name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "gravitee.ui.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if .Values.fullnameOverride -}}
{{- printf "%s-%s" .Values.fullnameOverride .Values.ui.name | trunc 63 | trimSuffix "-" -}}
{{- else if contains $name .Release.Name -}}
{{- printf "%s-%s" .Release.Name .Values.ui.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s-%s" .Release.Name $name .Values.ui.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified gateway name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "gravitee.portal.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if .Values.fullnameOverride -}}
{{- printf "%s-%s" .Values.fullnameOverride .Values.portal.name | trunc 63 | trimSuffix "-" -}}
{{- else if contains $name .Release.Name -}}
{{- printf "%s-%s" .Release.Name .Values.portal.name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s-%s" .Release.Name $name .Values.portal.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create initContainers for downloading plugins ext plugin-ext
*/}}
{{- define "deployment.pluginInitContainers" -}}
{{- if .plugins }}
- name: get-plugins
  {{- toYaml .initContainers | nindent 2 }}
  command: ['sh', '-c', "mkdir -p /tmp/plugins && cd /tmp/plugins {{- range $url := .plugins -}}
    {{ printf " && ( rm "}} {{regexFind "[^/]+$" $url}} {{ printf " 2>/dev/null || true ) && wget %s" $url }}
  {{- end -}}"]
  volumeMounts:
    - name: graviteeio-apim-plugins
      mountPath: /tmp/plugins
{{- end }}
{{- range $key, $url := .extPlugins }}
- name: get-{{ $key }}-ext
  {{- toYaml .initContainers | nindent 2 }}
  command: ['sh', '-c', "mkdir -p /tmp/plugins-ext && cd /tmp/plugins-ext && ( rm {{regexFind "[^/]+$" $url}} || true ) && wget {{ $url }}"]
  volumeMounts:
    - name: graviteeio-apim-{{ $key }}-ext
      mountPath: /tmp/plugins-ext
{{- end }}
{{- end -}}

{{/*
Create volumeMounts for plugins
*/}}
{{- define "deployment.pluginVolumeMounts" -}}
{{- if or .plugins .extPlugins }}
- name: graviteeio-apim-plugins
  mountPath: /opt/{{ .appName }}/plugins-ext
{{- end }}
{{- $appName := .appName -}}
{{- range $key, $_ := .extPlugins }}
- name: graviteeio-apim-{{ $key }}-ext
  mountPath: /opt/{{ $appName }}/plugins-ext/ext/{{ $key }}
{{- end }}
{{- end -}}

{{/*
Create volumes for plugins
*/}}
{{- define "deployment.pluginVolumes" -}}
{{- if or .plugins .extPlugins }}
- name: graviteeio-apim-plugins
  emptyDir: {}
{{- end }}
{{- range $key, $_ := .extPlugins }}
- name: graviteeio-apim-{{ $key }}-ext
  emptyDir: {}
{{- end }}
{{- end -}}

{{/*
Use the fullname if the serviceAccount value is not set
*/}}
{{- define "apim.serviceAccount" -}}
{{- if or (not .Values.apim.managedServiceAccount) (and .Values.apim.managedServiceAccount .Values.apim.serviceAccount) }}
{{- .Values.apim.serviceAccount -}}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the target Kubernetes version
*/}}
{{- define "common.capabilities.kubeVersion" -}}
{{- if .Values.global }}
    {{- if .Values.global.kubeVersion }}
    {{- .Values.global.kubeVersion -}}
    {{- else }}
    {{- default .Capabilities.KubeVersion.Version .Values.kubeVersion -}}
    {{- end -}}
{{- else }}
{{- default .Capabilities.KubeVersion.Version .Values.kubeVersion -}}
{{- end -}}
{{- end -}}

{{/*
Return the appropriate apiVersion for poddisruptionbudget.
*/}}
{{- define "common.capabilities.policy.apiVersion" -}}
{{- if semverCompare "<1.21-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "policy/v1beta1" -}}
{{- else -}}
{{- print "policy/v1" -}}
{{- end -}}
{{- end -}}

{{/*
Return the appropriate apiVersion for ingress.
*/}}
{{- define "common.capabilities.ingress.apiVersion" -}}
{{- if semverCompare "<1.14-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "extensions/v1beta1" -}}
{{- else if semverCompare "<1.19-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "networking.k8s.io/v1beta1" -}}
{{- else -}}
{{- print "networking.k8s.io/v1" -}}
{{- end }}
{{- end -}}

{{/*
Returns true if the ingressClassname field is supported
Usage:
{{ include "common.ingress.supportsIngressClassname" . }}
*/}}
{{- define "common.ingress.supportsIngressClassname" -}}
{{- if semverCompare "<1.18-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "false" -}}
{{- else -}}
{{- print "true" -}}
{{- end -}}
{{- end -}}

{{/*
Renders the annotations for an ingress
Usage:
{{ include "common.ingress.annotations.render" ( dict "annotations" .Values.path.to.the.Value "ingressClassName" .Values.path.to.the.Value "context" $) }}
*/}}
{{- define "common.ingress.annotations.render" -}}
{{- $ingressType := get $.annotations "kubernetes.io/ingress.class"  }}
{{- range $key, $value := $.annotations }}
{{- if or ( ne $key "kubernetes.io/ingress.class" ) ( not ( and ( $.ingressClassName ) ( include "common.ingress.supportsIngressClassname" $.context ))) }}
{{- if or (not (hasPrefix "nginx.ingress.kubernetes.io" $key)) (and (hasPrefix "nginx.ingress.kubernetes.io" $key) (contains "nginx" $ingressType)) }}
{{ $key }}: {{ $value | quote }}
{{- end -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Returns true if the appProtocol field is supported
Usage:
{{ include "common.service.supportsAppProtocol" . }}
*/}}
{{- define "common.service.supportsAppProtocol" -}}
{{- if semverCompare "<1.18-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "false" -}}
{{- else -}}
{{- print "true" -}}
{{- end -}}
{{- end -}}

Return the appropriate apiVersion for pod autoscaling.
*/}}
{{- define "common.capabilities.autoscaling.apiVersion" -}}
{{- if semverCompare "<1.12-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "autoscaling/v2beta1" -}}
{{- else if semverCompare "<1.23-0" (include "common.capabilities.kubeVersion" .) -}}
{{- print "autoscaling/v2beta2" -}}
{{- else -}}
{{- print "autoscaling/v2" -}}
{{- end }}
{{- end -}}

{{/*
Returns true if an extraVolumes named config is defined
Usage:
{{ include "gateway.externalConfig" . }}
*/}}
{{- define "gateway.externalConfig" -}}
{{- if hasKey .Values.gateway "extraVolumes" }}
{{- if contains "- name: config" .Values.gateway.extraVolumes  }}
{{- print "true" -}}
{{- end }}
{{- end }}
{{- end }}

{{/*
Returns logback if an extraVolumes named config is defined and gateway.logging.debug is set to true, else return config
Usage:
{{ include "gateway.logbackVolumeName" . }}
*/}}
{{- define "gateway.logbackVolumeName" -}}
{{- if and (include "gateway.externalConfig" .) (.Values.gateway.logging.debug) }}
{{- print "logback" -}}
{{- else -}}
{{- print "config" -}}
{{- end }}
{{- end }}

{{/*
Returns true if an extraVolumes named config is defined
Usage:
{{ include "api.externalConfig" . }}
*/}}
{{- define "api.externalConfig" -}}
{{- if hasKey .Values.api "extraVolumes" }}
{{- if contains "- name: config" .Values.api.extraVolumes  }}
{{- print "true" -}}
{{- end }}
{{- end }}
{{- end }}

{{/*
Returns logback if an extraVolumes named config is defined and api.logging.debug is set to true, else return config
Usage:
{{ include "api.logbackVolumeName" . }}
*/}}
{{- define "api.logbackVolumeName" -}}
{{- if and (include "api.externalConfig" .) (.Values.api.logging.debug) }}
{{- print "logback" -}}
{{- else -}}
{{- print "config" -}}
{{- end }}
{{- end }}

{{/*
Returns true if an extraVolumes named config is defined
Usage:
{{ include "portal.externalConfig" . }}
*/}}
{{- define "portal.externalConfig" -}}
{{- if hasKey .Values.portal "extraVolumes" }}
{{- if contains "- name: config" .Values.portal.extraVolumes  }}
{{- print "true" -}}
{{- end }}
{{- end }}
{{- end }}

{{/*
Returns true if an extraVolumes named config is defined
Usage:
{{ include "ui.externalConfig" . }}
*/}}
{{- define "ui.externalConfig" -}}
{{- if hasKey .Values.ui "extraVolumes" }}
{{- if contains "- name: config" .Values.ui.extraVolumes  }}
{{- print "true" -}}
{{- end }}
{{- end }}
{{- end }}

{{- define "ui.base_href.defined" -}}
{{- if contains "CONSOLE_BASE_HREF" (.Values.ui.env | toString) -}}
{{- print "true" -}}
{{ else }}
{{- print "false" -}}
{{- end }}
{{- end }}

{{- define "portal.base_href.defined" -}}
{{- if contains "PORTAL_BASE_HREF" (.Values.portal.env | toString) -}}
{{- print "true" -}}
{{ else }}
{{- print "false" -}}
{{- end }}
{{- end }}

{{/*
Returns installation type from the values or use default value
Usage:
{{ include "installation.type" . }}
*/}}
{{- define "installation.type" -}}
{{- if hasKey .Values "installation" }}
{{- print .Values.installation.type | default "standalone" -}}
{{ else}}
{{- print "standalone" -}}
{{- end }}
{{- end }}