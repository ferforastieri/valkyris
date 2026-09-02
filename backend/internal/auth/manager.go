package auth

import (
	"context"
	"crypto/subtle"
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	appcrypto "github.com/ferforastieri/camtacte/backend/internal/crypto"
	"github.com/ferforastieri/camtacte/backend/internal/store"
	"github.com/google/uuid"
)

type contextKey string

const deviceKey contextKey = "deviceID"

type Manager struct {
	store    *store.Store
	lifetime time.Duration
}

type PairingSession struct {
	ID          string    `json:"id"`
	Code        string    `json:"code,omitempty"`
	PublicURL   string    `json:"publicUrl"`
	Fingerprint string    `json:"fingerprint"`
	URI         string    `json:"uri"`
	ExpiresAt   time.Time `json:"expiresAt"`
}

type PairRequest struct {
	Code       string `json:"code"`
	DeviceName string `json:"deviceName"`
	Locale     string `json:"locale"`
}
type PairResponse struct {
	DeviceID string `json:"deviceId"`
	Token    string `json:"token"`
}

func NewManager(s *store.Store, lifetime time.Duration) *Manager {
	return &Manager{store: s, lifetime: lifetime}
}

func (m *Manager) CreatePairing(ctx context.Context, publicURL, fingerprint string) (PairingSession, error) {
	codeRaw, err := appcrypto.RandomToken(6)
	if err != nil {
		return PairingSession{}, err
	}
	code := strings.ToUpper(codeRaw[:8])
	now := time.Now().UTC()
	values := url.Values{"url": {publicURL}, "code": {code}, "fingerprint": {fingerprint}}
	session := PairingSession{ID: uuid.NewString(), Code: code, PublicURL: publicURL, Fingerprint: fingerprint, URI: "camtacte://pair?" + values.Encode(), ExpiresAt: now.Add(m.lifetime)}
	_, err = m.store.DB.ExecContext(ctx, `INSERT INTO pairing_sessions(id,code_hash,expires_at,created_at) VALUES(?,?,?,?)`, session.ID, appcrypto.Hash(code), session.ExpiresAt.Format(time.RFC3339Nano), now.Format(time.RFC3339Nano))
	return session, err
}

func (m *Manager) Pair(ctx context.Context, req PairRequest) (PairResponse, error) {
	if req.Code == "" || req.DeviceName == "" {
		return PairResponse{}, fmt.Errorf("code and deviceName are required")
	}
	if req.Locale == "" {
		req.Locale = "pt-BR"
	}
	rows, err := m.store.DB.QueryContext(ctx, `SELECT id,code_hash,expires_at FROM pairing_sessions WHERE used_at IS NULL`)
	if err != nil {
		return PairResponse{}, err
	}
	var sessionID string
	for rows.Next() {
		var id, expires string
		var hash []byte
		if err = rows.Scan(&id, &hash, &expires); err != nil {
			return PairResponse{}, err
		}
		expiry, _ := time.Parse(time.RFC3339Nano, expires)
		if time.Now().Before(expiry) && subtle.ConstantTimeCompare(hash, appcrypto.Hash(strings.ToUpper(req.Code))) == 1 {
			sessionID = id
			break
		}
	}
	if err = rows.Err(); err != nil {
		rows.Close()
		return PairResponse{}, err
	}
	if err = rows.Close(); err != nil {
		return PairResponse{}, err
	}
	if sessionID == "" {
		return PairResponse{}, fmt.Errorf("invalid or expired pairing code")
	}
	token, err := appcrypto.RandomToken(32)
	if err != nil {
		return PairResponse{}, err
	}
	deviceID := uuid.NewString()
	now := time.Now().UTC().Format(time.RFC3339Nano)
	tx, err := m.store.DB.BeginTx(ctx, nil)
	if err != nil {
		return PairResponse{}, err
	}
	defer tx.Rollback()
	if _, err = tx.ExecContext(ctx, `INSERT INTO devices(id,name,token_hash,locale,created_at,last_seen_at) VALUES(?,?,?,?,?,?)`, deviceID, req.DeviceName, appcrypto.Hash(token), req.Locale, now, now); err != nil {
		return PairResponse{}, err
	}
	if _, err = tx.ExecContext(ctx, `UPDATE pairing_sessions SET used_at=? WHERE id=? AND used_at IS NULL`, now, sessionID); err != nil {
		return PairResponse{}, err
	}
	if err = tx.Commit(); err != nil {
		return PairResponse{}, err
	}
	return PairResponse{DeviceID: deviceID, Token: token}, nil
}

func (m *Manager) Authenticate(ctx context.Context, token string) (string, error) {
	if token == "" {
		return "", fmt.Errorf("missing token")
	}
	var id string
	err := m.store.DB.QueryRowContext(ctx, `SELECT id FROM devices WHERE token_hash=? AND enabled=1`, appcrypto.Hash(token)).Scan(&id)
	if err != nil {
		return "", err
	}
	_, _ = m.store.DB.ExecContext(ctx, `UPDATE devices SET last_seen_at=? WHERE id=?`, time.Now().UTC().Format(time.RFC3339Nano), id)
	return id, nil
}

func (m *Manager) Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		header := r.Header.Get("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			writeUnauthorized(w)
			return
		}
		id, err := m.Authenticate(r.Context(), strings.TrimPrefix(header, "Bearer "))
		if err != nil {
			writeUnauthorized(w)
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), deviceKey, id)))
	})
}

func DeviceID(ctx context.Context) string { v, _ := ctx.Value(deviceKey).(string); return v }
func writeUnauthorized(w http.ResponseWriter) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusUnauthorized)
	_ = json.NewEncoder(w).Encode(map[string]string{"error": "authentication required"})
}
func IsNotFound(err error) bool { return err == sql.ErrNoRows }
