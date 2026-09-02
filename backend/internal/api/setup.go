package api

import (
	"bytes"
	"crypto/sha256"
	"crypto/subtle"
	_ "embed"
	"encoding/base64"
	"html/template"
	"net/http"
	"strings"

	"github.com/ferforastieri/valkyris/backend/internal/auth"
	qrcode "github.com/skip2/go-qrcode"
)

const setupCookieName = "valkyris_setup"

//go:embed setup.html
var setupHTML string

//go:embed valkyris-mark.png
var setupBrandMark []byte

var setupTemplate = template.Must(template.New("setup").Parse(setupHTML))

type setupPageData struct {
	BrandMark template.URL
	QRCode    template.URL
	PairURI   template.URL
	Code      string
	PublicURL string
	Expires   string
}

func (s *Server) setupRoot(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	target := "/setup"
	if key := r.URL.Query().Get("key"); key != "" {
		target += "?key=" + key
	}
	http.Redirect(w, r, target, http.StatusTemporaryRedirect)
}

func (s *Server) setupPage(w http.ResponseWriter, r *http.Request) {
	if !s.authorizeSetup(w, r, true) {
		s.setupDenied(w)
		return
	}
	if r.URL.Query().Has("key") {
		http.Redirect(w, r, "/setup", http.StatusSeeOther)
		return
	}
	session, err := s.newSetupSession(r)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err)
		return
	}
	png, err := qrcode.Encode(session.URI, qrcode.Medium, 360)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err)
		return
	}
	var page bytes.Buffer
	err = setupTemplate.Execute(&page, setupPageData{
		BrandMark: template.URL("data:image/png;base64," + base64.StdEncoding.EncodeToString(setupBrandMark)),
		QRCode:    template.URL("data:image/png;base64," + base64.StdEncoding.EncodeToString(png)),
		PairURI:   template.URL(session.URI),
		Code:      session.Code,
		PublicURL: session.PublicURL,
		Expires:   "10 min",
	})
	if err != nil {
		writeError(w, http.StatusInternalServerError, err)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'")
	w.Header().Set("Refresh", "480; url=/setup")
	_, _ = page.WriteTo(w)
}

func (s *Server) setupTerminal(w http.ResponseWriter, r *http.Request) {
	if !s.authorizeSetup(w, r, false) {
		writeError(w, http.StatusUnauthorized, errSetupAuthorization)
		return
	}
	session, err := s.newSetupSession(r)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err)
		return
	}
	code, err := qrcode.New(session.URI, qrcode.Medium)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err)
		return
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	_, _ = w.Write([]byte(terminalQRCode(code.Bitmap())))
}

func (s *Server) newSetupSession(r *http.Request) (auth.PairingSession, error) {
	publicURL := s.publicURL
	if publicURL == "" {
		publicURL = origin(r)
	}
	return s.auth.CreatePairing(r.Context(), publicURL, s.fingerprint)
}

var errSetupAuthorization = &setupError{"setup authorization required"}

type setupError struct{ message string }

func (e *setupError) Error() string { return e.message }

func (s *Server) authorizeSetup(w http.ResponseWriter, r *http.Request, allowQuery bool) bool {
	if s.setupToken == "" {
		return false
	}
	candidates := []string{r.Header.Get("X-Valkyris-Setup-Key")}
	if cookie, err := r.Cookie(setupCookieName); err == nil {
		candidates = append(candidates, cookie.Value)
	}
	if allowQuery {
		candidates = append(candidates, r.URL.Query().Get("key"))
	}
	for _, candidate := range candidates {
		if secureEqual(candidate, s.setupToken) {
			if allowQuery && r.URL.Query().Get("key") != "" {
				http.SetCookie(w, &http.Cookie{Name: setupCookieName, Value: s.setupToken, Path: "/setup", Secure: true, HttpOnly: true, SameSite: http.SameSiteStrictMode, MaxAge: 365 * 24 * 60 * 60})
			}
			return true
		}
	}
	return false
}

func secureEqual(left, right string) bool {
	leftHash := sha256.Sum256([]byte(left))
	rightHash := sha256.Sum256([]byte(right))
	return subtle.ConstantTimeCompare(leftHash[:], rightHash[:]) == 1
}

func (s *Server) setupDenied(w http.ResponseWriter) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'")
	w.WriteHeader(http.StatusUnauthorized)
	_, _ = w.Write([]byte(`<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Valkyris</title><style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#e8e9e5;color:#242724;font:16px system-ui}.card{max-width:34rem;margin:1.5rem;padding:2rem;border-radius:24px;background:#f9faf7;box-shadow:0 18px 50px #24272418}h1{margin:0 0 .7rem}p{color:#70766f;line-height:1.6}@media(prefers-color-scheme:dark){body{background:#101310;color:#f1f4ef}.card{background:#1a1f1a}p{color:#a6afa5}}</style><main class="card"><h1>Use o endereço completo</h1><p>Abra a URL de configuração exibida pelo instalador do Valkyris neste navegador.</p></main></html>`))
}

func terminalQRCode(bitmap [][]bool) string {
	var out strings.Builder
	for y := 0; y < len(bitmap); y += 2 {
		for x, upperDark := range bitmap[y] {
			lowerDark := false
			if y+1 < len(bitmap) && x < len(bitmap[y+1]) {
				lowerDark = bitmap[y+1][x]
			}
			switch {
			case upperDark && lowerDark:
				out.WriteString("\x1b[40m ")
			case !upperDark && !lowerDark:
				out.WriteString("\x1b[47m ")
			case upperDark:
				out.WriteString("\x1b[30;47m▀")
			default:
				out.WriteString("\x1b[37;40m▀")
			}
		}
		out.WriteString("\x1b[0m\n")
	}
	out.WriteString("\x1b[0m")
	return out.String()
}
