package policy

import (
	"context"
	"fmt"
)

type ModelAllowlist struct {
	name    string
	models  map[string]bool
	message string
}

func NewModelAllowlist(name string, config map[string]any) (*ModelAllowlist, error) {
	models := make(map[string]bool)
	if v, ok := config["models"]; ok {
		if list, ok := v.([]any); ok {
			for _, m := range list {
				if s, ok := m.(string); ok {
					models[s] = true
				}
			}
		}
	}

	msg, _ := config["message"].(string)

	return &ModelAllowlist{
		name:    name,
		models:  models,
		message: msg,
	}, nil
}

func (ma *ModelAllowlist) Name() string { return ma.name }

func (ma *ModelAllowlist) Evaluate(_ context.Context, req *InterceptedRequest) *PolicyResult {
	if req.Model == "" {
		return &PolicyResult{Action: ActionAllow, Policy: ma.name}
	}
	if !ma.models[req.Model] {
		return &PolicyResult{
			Action: ActionBlock,
			Policy: ma.name,
			Reason: fmt.Sprintf("model %s is not in the allowlist", req.Model),
			Details: map[string]any{
				"requested_model": req.Model,
			},
		}
	}
	return &PolicyResult{Action: ActionAllow, Policy: ma.name}
}
