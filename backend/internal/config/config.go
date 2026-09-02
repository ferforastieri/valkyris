package config

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
)

type Config struct {
	Listen          string
	DataDir         string
	DatabasePath    string
	PublicURL       string
	TLSCert         string
	TLSKey          string
	MasterKeyFile   string
	MediaURL        string
	MediaAPI        string
	RecordingsDir   string
	ModelsDir       string
	RetentionAge    time.Duration
	RetentionBytes  int64
	PairingLifetime time.Duration
	SetupToken      string
}

func Load() Config {
	data := env("CAMTACTE_DATA_DIR", "./data")
	return Config{
		Listen:          env("CAMTACTE_LISTEN", ":8443"),
		DataDir:         data,
		DatabasePath:    env("CAMTACTE_DATABASE", filepath.Join(data, "camtacte.db")),
		PublicURL:       env("CAMTACTE_PUBLIC_URL", "https://localhost:8443"),
		TLSCert:         env("CAMTACTE_TLS_CERT", filepath.Join(data, "tls", "server.crt")),
		TLSKey:          env("CAMTACTE_TLS_KEY", filepath.Join(data, "tls", "server.key")),
		MasterKeyFile:   env("CAMTACTE_MASTER_KEY_FILE", filepath.Join(data, "secrets", "master.key")),
		MediaURL:        env("CAMTACTE_MEDIA_URL", "http://localhost:8888"),
		MediaAPI:        env("CAMTACTE_MEDIA_API", "http://localhost:9997"),
		RecordingsDir:   env("CAMTACTE_MEDIA_RECORDINGS", filepath.Join(data, "recordings")),
		ModelsDir:       env("CAMTACTE_MODELS_DIR", "./models"),
		RetentionAge:    7 * 24 * time.Hour,
		RetentionBytes:  5 * 1024 * 1024 * 1024,
		PairingLifetime: 10 * time.Minute,
		SetupToken:      os.Getenv("CAMTACTE_SETUP_TOKEN"),
	}
}

func (c Config) Prepare() error {
	for _, dir := range []string{c.DataDir, filepath.Dir(c.DatabasePath), filepath.Dir(c.TLSCert), filepath.Dir(c.MasterKeyFile), c.RecordingsDir} {
		if err := os.MkdirAll(dir, 0o700); err != nil {
			return fmt.Errorf("create %s: %w", dir, err)
		}
	}
	return nil
}

func env(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
