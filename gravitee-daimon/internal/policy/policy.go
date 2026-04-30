package policy

import "context"

type Action string

const (
	ActionAllow Action = "allowed"
	ActionBlock Action = "blocked"
	ActionWarn  Action = "warned"
)

type InterceptedRequest struct {
	Model string
	Body  []byte
}

type PolicyResult struct {
	Action  Action
	Policy  string
	Reason  string
	Details map[string]any
}

type Policy interface {
	Name() string
	Evaluate(ctx context.Context, req *InterceptedRequest) *PolicyResult
}
