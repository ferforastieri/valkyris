package api

import (
	"html"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"net/url"
	"path/filepath"
	"regexp"
	"strings"
	"testing"
	"time"

	"github.com/ferforastieri/valkyris/backend/internal/auth"
	"github.com/ferforastieri/valkyris/backend/internal/store"
)

func TestSetupPageCreatesScannableOneTimePairing(t *testing.T) {
	server, manager := setupTestServer(t)
	handler := server.Handler()

	bootstrap := httptest.NewRecorder()
	handler.ServeHTTP(bootstrap, httptest.NewRequest(http.MethodGet, "/setup?key=local-secret", nil))
	if bootstrap.Code != http.StatusSeeOther {
		t.Fatalf("bootstrap returned %d", bootstrap.Code)
	}
	cookies := bootstrap.Result().Cookies()
	if len(cookies) != 1 || cookies[0].Value != "local-secret" || !cookies[0].Secure || !cookies[0].HttpOnly {
		t.Fatalf("unexpected setup cookie: %#v", cookies)
	}

	pageRequest := httptest.NewRequest(http.MethodGet, "/setup", nil)
	pageRequest.AddCookie(cookies[0])
	page := httptest.NewRecorder()
	handler.ServeHTTP(page, pageRequest)
	if page.Code != http.StatusOK || !strings.Contains(page.Body.String(), "data:image/png;base64,") {
		t.Fatalf("setup page was not rendered: status=%d body=%s", page.Code, page.Body.String())
	}
	if page.Header().Get("Cache-Control") != "no-store" || page.Header().Get("Refresh") == "" {
		t.Fatal("setup page must remain fresh and uncached")
	}

	match := regexp.MustCompile(`href="(valkyris://pair\?[^"]+)"`).FindStringSubmatch(page.Body.String())
	if len(match) != 2 {
		t.Fatal("setup page did not contain an app pairing link")
	}
	pairURI, err := url.Parse(html.UnescapeString(match[1]))
	if err != nil {
		t.Fatal(err)
	}
	request := auth.PairRequest{Code: pairURI.Query().Get("code"), DeviceName: "Pixel", Locale: "pt-BR"}
	if _, err = manager.Pair(t.Context(), request); err != nil {
		t.Fatalf("QR pairing failed: %v", err)
	}
	if _, err = manager.Pair(t.Context(), request); err == nil {
		t.Fatal("QR pairing code was accepted twice")
	}
}

func TestSetupPageRequiresLocalSetupSecret(t *testing.T) {
	server, _ := setupTestServer(t)
	response := httptest.NewRecorder()
	server.Handler().ServeHTTP(response, httptest.NewRequest(http.MethodGet, "/setup", nil))
	if response.Code != http.StatusUnauthorized || strings.Contains(response.Body.String(), "data:image/png") {
		t.Fatalf("unauthorized setup returned status %d", response.Code)
	}
}

func TestTerminalQRCodeUsesSetupHeader(t *testing.T) {
	server, _ := setupTestServer(t)
	request := httptest.NewRequest(http.MethodGet, "/setup/terminal", nil)
	request.Header.Set("X-Valkyris-Setup-Key", "local-secret")
	response := httptest.NewRecorder()
	server.Handler().ServeHTTP(response, request)
	if response.Code != http.StatusOK || !strings.Contains(response.Body.String(), "\x1b[40m") || !strings.Contains(response.Body.String(), "\x1b[47m") {
		t.Fatalf("terminal QR was not rendered: status=%d", response.Code)
	}
}

func setupTestServer(t *testing.T) (*Server, *auth.Manager) {
	t.Helper()
	db, err := store.Open(filepath.Join(t.TempDir(), "valkyris.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = db.Close() })
	manager := auth.NewManager(db, 10*time.Minute)
	server := &Server{auth: manager, logger: slog.New(slog.NewTextHandler(io.Discard, nil))}
	server.SetPairingIdentity("https://192.168.1.20:8443", "AA:BB:CC", "local-secret")
	return server, manager
}
