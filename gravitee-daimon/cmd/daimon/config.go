package main

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Gateway      GatewayConfig      `yaml:"gateway"`
	Proxy        ProxyConfig        `yaml:"proxy"`
	Registration RegistrationConfig `yaml:"registration"`
	Metrics      MetricsConfig      `yaml:"metrics"`
	Detector     DetectorConfig     `yaml:"detector"`
}

type GatewayConfig struct {
	URL       string `yaml:"url"`
	AIAPIPath string `yaml:"ai_api_path"`
}

type ProxyConfig struct {
	ListenPort int `yaml:"listen_port"`
}

type RegistrationConfig struct {
	HeartbeatIntervalSec int `yaml:"heartbeat_interval_sec"`
}

type MetricsConfig struct {
	OutputFile string `yaml:"output_file"`
}

type DetectorConfig struct {
	Enabled         bool     `yaml:"enabled"`
	ScanIntervalSec int      `yaml:"scan_interval_sec"`
	Providers       []string `yaml:"providers"`
}

func LoadConfig(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}
