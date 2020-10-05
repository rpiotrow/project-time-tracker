{{/*
Expand the name of the chart.
*/}}
{{- define "ptt.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ptt.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app names.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ptt.gateway.fullname" -}}
{{- if .Values.gateway.fullnameOverride }}
{{- .Values.gateway.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- printf "%s-%s" .Release.Name .Values.gateway.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s-%s" .Release.Name $name .Values.gateway.name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "ptt.readside.fullname" -}}
{{- if .Values.readside.fullnameOverride }}
{{- .Values.readside.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- printf "%s-%s" .Release.Name .Values.readside.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s-%s" .Release.Name $name .Values.readside.name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "ptt.writeside.fullname" -}}
{{- if .Values.writeside.fullnameOverride }}
{{- .Values.writeside.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- printf "%s-%s" .Release.Name .Values.writeside.name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s-%s" .Release.Name $name .Values.writeside.name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create labels for services
*/}}
{{- define "ptt.gateway.labels" -}}
helm.sh/chart: {{ include "ptt.chart" . }}
{{ include "ptt.gateway.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.pttVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "ptt.readside.labels" -}}
helm.sh/chart: {{ include "ptt.chart" . }}
{{ include "ptt.readside.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.pttVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "ptt.writeside.labels" -}}
helm.sh/chart: {{ include "ptt.chart" . }}
{{ include "ptt.writeside.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.pttVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "ptt.gateway.selectorLabels" -}}
app.kubernetes.io/name: {{ .Values.gateway.name | quote }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "ptt.readside.selectorLabels" -}}
app.kubernetes.io/name: {{ .Values.readside.name | quote }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "ptt.writeside.selectorLabels" -}}
app.kubernetes.io/name: {{ .Values.writeside.name | quote }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ptt.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "ptt.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
