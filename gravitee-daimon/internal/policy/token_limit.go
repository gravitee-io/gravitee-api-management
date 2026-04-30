package policy

import (
	"context"
	"fmt"
	"strings"
)

type TokenLimit struct {
	name      string
	maxTokens int
	message   string
}

func NewTokenLimit(name string, config map[string]any) (*TokenLimit, error) {
	maxTokens := 100000
	if v, ok := config["max_tokens_per_request"]; ok {
		switch val := v.(type) {
		case int:
			maxTokens = val
		case float64:
			maxTokens = int(val)
		}
	}

	msg, _ := config["message"].(string)

	return &TokenLimit{
		name:      name,
		maxTokens: maxTokens,
		message:   msg,
	}, nil
}

func (tl *TokenLimit) Name() string { return tl.name }

func (tl *TokenLimit) Evaluate(_ context.Context, req *InterceptedRequest) *PolicyResult {
	estimated := estimateTokens(req.Body)
	if estimated > tl.maxTokens {
		return &PolicyResult{
			Action: ActionBlock,
			Policy: tl.name,
			Reason: fmt.Sprintf("estimated %d tokens exceeds limit of %d", estimated, tl.maxTokens),
			Details: map[string]any{
				"estimated_tokens": estimated,
				"max_tokens":       tl.maxTokens,
			},
		}
	}
	return &PolicyResult{Action: ActionAllow, Policy: tl.name}
}

func estimateTokens(body []byte) int {
	words := strings.Fields(string(body))
	return int(float64(len(words)) * 1.3)
}
