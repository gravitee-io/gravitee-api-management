package policy

import (
	"context"
	"fmt"
	"regexp"
)

type ContentFilter struct {
	name     string
	patterns []compiledPattern
	message  string
}

type compiledPattern struct {
	name  string
	regex *regexp.Regexp
}

func NewContentFilter(name string, config map[string]any) (*ContentFilter, error) {
	rawPatterns, ok := config["patterns"]
	if !ok {
		return nil, fmt.Errorf("content-filter requires 'patterns' config")
	}

	patternList, ok := rawPatterns.([]any)
	if !ok {
		return nil, fmt.Errorf("patterns must be a list")
	}

	var patterns []compiledPattern
	for _, raw := range patternList {
		m, ok := raw.(map[string]any)
		if !ok {
			continue
		}
		pName, _ := m["name"].(string)
		pRegex, _ := m["regex"].(string)
		compiled, err := regexp.Compile(pRegex)
		if err != nil {
			return nil, fmt.Errorf("invalid regex for pattern %s: %w", pName, err)
		}
		patterns = append(patterns, compiledPattern{name: pName, regex: compiled})
	}

	msg, _ := config["message"].(string)

	return &ContentFilter{
		name:     name,
		patterns: patterns,
		message:  msg,
	}, nil
}

func (cf *ContentFilter) Name() string { return cf.name }

func (cf *ContentFilter) Evaluate(_ context.Context, req *InterceptedRequest) *PolicyResult {
	body := string(req.Body)
	for _, p := range cf.patterns {
		if p.regex.MatchString(body) {
			return &PolicyResult{
				Action: ActionBlock,
				Policy: cf.name,
				Reason: fmt.Sprintf("sensitive content detected: %s", p.name),
				Details: map[string]any{
					"match_name": p.name,
				},
			}
		}
	}
	return &PolicyResult{Action: ActionAllow, Policy: cf.name}
}
