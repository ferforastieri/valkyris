package app

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestRetentionAppliesAgeAndSizeLimits(t *testing.T) {
	root := t.TempDir()
	events := filepath.Join(root, "events")
	if err := os.MkdirAll(events, 0o700); err != nil {
		t.Fatal(err)
	}
	old := filepath.Join(events, "old.mp4")
	first := filepath.Join(events, "first.mp4")
	last := filepath.Join(events, "last.mp4")
	for _, path := range []string{old, first, last} {
		if err := os.WriteFile(path, make([]byte, 10), 0o600); err != nil {
			t.Fatal(err)
		}
	}
	now := time.Now()
	_ = os.Chtimes(old, now.Add(-8*24*time.Hour), now.Add(-8*24*time.Hour))
	_ = os.Chtimes(first, now.Add(-2*time.Minute), now.Add(-2*time.Minute))
	_ = os.Chtimes(last, now.Add(-time.Minute), now.Add(-time.Minute))
	service := &Service{DataDir: root}
	service.retain(7*24*time.Hour, 15)
	if _, err := os.Stat(old); !os.IsNotExist(err) {
		t.Fatal("age-expired media was retained")
	}
	if _, err := os.Stat(first); !os.IsNotExist(err) {
		t.Fatal("oldest media was not removed when size limit was exceeded")
	}
	if _, err := os.Stat(last); err != nil {
		t.Fatal("newest media should be retained")
	}
}
