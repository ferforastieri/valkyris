package auth

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/ferforastieri/valkyris/backend/internal/store"
)

func TestPairingIsOneTimeAndTokenAuthenticates(t *testing.T) {
	db, err := store.Open(t.TempDir() + "/auth.db")
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	manager := NewManager(db, time.Minute)
	session, err := manager.CreatePairing(context.Background(), "https://home.local:8443", "AA:BB")
	if err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(session.URI, "valkyris://pair?") || !strings.Contains(session.URI, "fingerprint=AA%3ABB") {
		t.Fatalf("unexpected pairing URI: %s", session.URI)
	}
	paired, err := manager.Pair(context.Background(), PairRequest{Code: strings.ToLower(session.Code), DeviceName: "Pixel", Locale: "en"})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = manager.Pair(context.Background(), PairRequest{Code: session.Code, DeviceName: "Again"}); err == nil {
		t.Fatal("one-time pairing code was accepted twice")
	}
	if id, err := manager.Authenticate(context.Background(), paired.Token); err != nil || id != paired.DeviceID {
		t.Fatalf("token did not authenticate: id=%q err=%v", id, err)
	}

	handler := manager.Middleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if DeviceID(r.Context()) != paired.DeviceID {
			t.Error("device ID was not propagated")
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+paired.Token)
	response := httptest.NewRecorder()
	handler.ServeHTTP(response, req)
	if response.Code != http.StatusNoContent {
		t.Fatalf("authenticated request returned %d", response.Code)
	}
}

func TestExpiredPairingCodeIsRejected(t *testing.T) {
	db, err := store.Open(t.TempDir() + "/expired.db")
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	manager := NewManager(db, -time.Second)
	session, err := manager.CreatePairing(context.Background(), "https://home", "fingerprint")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = manager.Pair(context.Background(), PairRequest{Code: session.Code, DeviceName: "Phone"}); err == nil {
		t.Fatal("expired pairing code was accepted")
	}
}
