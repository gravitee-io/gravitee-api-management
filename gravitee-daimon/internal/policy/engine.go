package policy

import (
	"context"
	"fmt"
	"log"
	"os"
	"sync"

	"github.com/fsnotify/fsnotify"
	"gopkg.in/yaml.v3"
)

type Engine struct {
	mu       sync.RWMutex
	policies []Policy
	path     string
	watcher  *fsnotify.Watcher
	done     chan struct{}
}

type policyFile struct {
	Policies []policyDef `yaml:"policies"`
}

type policyDef struct {
	Name    string         `yaml:"name"`
	Enabled bool           `yaml:"enabled"`
	Type    string         `yaml:"type"`
	Action  string         `yaml:"action"`
	Config  map[string]any `yaml:"config"`
}

func NewEngine(path string) (*Engine, error) {
	e := &Engine{
		path: path,
		done: make(chan struct{}),
	}

	if err := e.load(); err != nil {
		return nil, err
	}

	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		return nil, fmt.Errorf("failed to create watcher: %w", err)
	}
	e.watcher = watcher

	if err := watcher.Add(path); err != nil {
		return nil, fmt.Errorf("failed to watch %s: %w", path, err)
	}

	go e.watchLoop()

	return e, nil
}

func (e *Engine) Evaluate(ctx context.Context, req *InterceptedRequest) *PolicyResult {
	e.mu.RLock()
	defer e.mu.RUnlock()

	for _, p := range e.policies {
		result := p.Evaluate(ctx, req)
		if result != nil && result.Action != ActionAllow {
			return result
		}
	}
	return &PolicyResult{Action: ActionAllow}
}

func (e *Engine) Policies() []Policy {
	e.mu.RLock()
	defer e.mu.RUnlock()
	out := make([]Policy, len(e.policies))
	copy(out, e.policies)
	return out
}

func (e *Engine) Close() {
	close(e.done)
	if e.watcher != nil {
		e.watcher.Close()
	}
}

func (e *Engine) load() error {
	data, err := os.ReadFile(e.path)
	if err != nil {
		return fmt.Errorf("failed to read %s: %w", e.path, err)
	}

	var pf policyFile
	if err := yaml.Unmarshal(data, &pf); err != nil {
		return fmt.Errorf("failed to parse %s: %w", e.path, err)
	}

	var policies []Policy
	for _, def := range pf.Policies {
		if !def.Enabled {
			continue
		}
		p, err := buildPolicy(def)
		if err != nil {
			log.Printf("skipping policy %s: %v", def.Name, err)
			continue
		}
		policies = append(policies, p)
	}

	e.mu.Lock()
	e.policies = policies
	e.mu.Unlock()

	log.Printf("loaded %d policies from %s", len(policies), e.path)
	return nil
}

func (e *Engine) watchLoop() {
	for {
		select {
		case <-e.done:
			return
		case event, ok := <-e.watcher.Events:
			if !ok {
				return
			}
			if event.Has(fsnotify.Write) || event.Has(fsnotify.Create) {
				log.Printf("policies file changed, reloading...")
				if err := e.load(); err != nil {
					log.Printf("failed to reload policies: %v", err)
				}
			}
		case err, ok := <-e.watcher.Errors:
			if !ok {
				return
			}
			log.Printf("watcher error: %v", err)
		}
	}
}

func buildPolicy(def policyDef) (Policy, error) {
	switch def.Type {
	case "content-filter":
		return NewContentFilter(def.Name, def.Config)
	case "token-limit":
		return NewTokenLimit(def.Name, def.Config)
	case "model-allowlist":
		return NewModelAllowlist(def.Name, def.Config)
	default:
		return nil, fmt.Errorf("unknown policy type: %s", def.Type)
	}
}
