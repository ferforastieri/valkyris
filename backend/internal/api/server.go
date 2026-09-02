package api

import (
	"context"
	"database/sql"
	_ "embed"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/ferforastieri/camtacte/backend/internal/auth"
	"github.com/ferforastieri/camtacte/backend/internal/camera"
	"github.com/ferforastieri/camtacte/backend/internal/detector"
	"github.com/ferforastieri/camtacte/backend/internal/event"
	"github.com/ferforastieri/camtacte/backend/internal/media"
	"github.com/ferforastieri/camtacte/backend/internal/notify"
	"github.com/ferforastieri/camtacte/backend/internal/rules"
)

//go:embed openapi.yaml
var openAPI []byte

type DetectionSubmitter interface {
	Submit(context.Context, rules.Detection) ([]event.Event, error)
}
type Server struct {
	auth        *auth.Manager
	cameras     *camera.Repository
	onvif       *camera.ONVIFClient
	media       *media.Manager
	rules       *rules.Service
	events      *event.Service
	notify      *notify.Service
	hub         *Hub
	submitter   DetectionSubmitter
	logger      *slog.Logger
	publicURL   string
	fingerprint string
	setupToken  string
}

func NewServer(a *auth.Manager, c *camera.Repository, o *camera.ONVIFClient, m *media.Manager, r *rules.Service, e *event.Service, n *notify.Service, h *Hub, logger *slog.Logger) *Server {
	return &Server{auth: a, cameras: c, onvif: o, media: m, rules: r, events: e, notify: n, hub: h, logger: logger}
}
func (s *Server) SetSubmitter(sub DetectionSubmitter) { s.submitter = sub }
func (s *Server) SetPairingIdentity(publicURL, fingerprint, setupToken string) {
	s.publicURL, s.fingerprint, s.setupToken = publicURL, fingerprint, setupToken
}
func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", s.setupRoot)
	mux.HandleFunc("GET /setup", s.setupPage)
	mux.HandleFunc("GET /setup/terminal", s.setupTerminal)
	mux.HandleFunc("GET /health", s.health)
	mux.HandleFunc("GET /openapi.yaml", s.openapi)
	mux.HandleFunc("POST /api/v1/pair", s.pair)
	protected := http.NewServeMux()
	protected.HandleFunc("POST /pairing-sessions", s.pairingSession)
	protected.HandleFunc("GET /cameras", s.listCameras)
	protected.HandleFunc("POST /cameras", s.createCamera)
	protected.HandleFunc("DELETE /cameras/{id}", s.deleteCamera)
	protected.HandleFunc("POST /cameras/{id}/ptz", s.ptz)
	protected.HandleFunc("GET /cameras/{id}/snapshot", s.snapshot)
	protected.HandleFunc("GET /cameras/{id}/live/{asset...}", s.live)
	protected.HandleFunc("GET /detectors", s.detectors)
	protected.HandleFunc("GET /rules", s.listRules)
	protected.HandleFunc("POST /rules", s.createRule)
	protected.HandleFunc("DELETE /rules/{id}", s.deleteRule)
	protected.HandleFunc("GET /events", s.listEvents)
	protected.HandleFunc("GET /events/{id}", s.getEvent)
	protected.HandleFunc("POST /events/{id}/acknowledge", s.ackEvent)
	protected.HandleFunc("GET /events/{id}/snapshot", s.eventSnapshot)
	protected.HandleFunc("GET /events/{id}/clip", s.eventClip)
	protected.HandleFunc("POST /devices/push", s.push)
	protected.HandleFunc("POST /detections", s.submitDetection)
	protected.Handle("/realtime", s.hub)
	mux.Handle("/api/v1/", http.StripPrefix("/api/v1", s.auth.Middleware(protected)))
	return requestLog(s.logger, securityHeaders(mux))
}
func (s *Server) health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{"status": "ok", "service": "camtacte", "time": time.Now().UTC()})
}
func (s *Server) openapi(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/yaml")
	_, _ = w.Write(openAPI)
}
func (s *Server) pair(w http.ResponseWriter, r *http.Request) {
	var in auth.PairRequest
	if !decode(w, r, &in) {
		return
	}
	out, err := s.auth.Pair(r.Context(), in)
	if err != nil {
		writeError(w, 401, err)
		return
	}
	writeJSON(w, 201, out)
}
func (s *Server) pairingSession(w http.ResponseWriter, r *http.Request) {
	publicURL := s.publicURL
	if publicURL == "" {
		publicURL = origin(r)
	}
	session, err := s.auth.CreatePairing(r.Context(), publicURL, s.fingerprint)
	if err != nil {
		writeError(w, 500, err)
		return
	}
	writeJSON(w, 201, session)
}
func (s *Server) listCameras(w http.ResponseWriter, r *http.Request) {
	out, err := s.cameras.List(r.Context())
	respond(w, out, err)
}
func (s *Server) createCamera(w http.ResponseWriter, r *http.Request) {
	var in camera.CreateInput
	if !decode(w, r, &in) {
		return
	}
	caps, profile, services, err := s.onvif.Probe(r.Context(), in.Host, defaultPort(in.Port), in.Username, in.Password)
	if err != nil {
		writeError(w, 422, err)
		return
	}
	cam, err := s.cameras.Create(r.Context(), in, caps, profile, services)
	if err != nil {
		writeError(w, 400, err)
		return
	}
	if err = s.media.ConfigureCamera(r.Context(), cam.ID, in.RTSPURI); err != nil {
		_ = s.cameras.Delete(r.Context(), cam.ID)
		writeError(w, 502, err)
		return
	}
	writeJSON(w, 201, cam)
}
func (s *Server) deleteCamera(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	err := s.cameras.Delete(r.Context(), id)
	if err == nil {
		_ = s.media.RemoveCamera(r.Context(), id)
		w.WriteHeader(204)
		return
	}
	respond(w, nil, err)
}
func (s *Server) ptz(w http.ResponseWriter, r *http.Request) {
	cam, cred, err := s.cameras.Get(r.Context(), r.PathValue("id"))
	if err != nil {
		respond(w, nil, err)
		return
	}
	if !cam.Capabilities.PTZ {
		writeError(w, 409, fmt.Errorf("camera does not advertise PTZ"))
		return
	}
	var command camera.PTZCommand
	if !decode(w, r, &command) {
		return
	}
	if err = s.onvif.PTZ(r.Context(), cam, cred, command); err != nil {
		writeError(w, 502, err)
		return
	}
	w.WriteHeader(204)
}
func (s *Server) snapshot(w http.ResponseWriter, r *http.Request) {
	cam, cred, err := s.cameras.Get(r.Context(), r.PathValue("id"))
	if err != nil {
		respond(w, nil, err)
		return
	}
	uri, err := s.onvif.SnapshotURI(r.Context(), cam, cred)
	if err != nil {
		writeError(w, 502, err)
		return
	}
	req, _ := http.NewRequestWithContext(r.Context(), http.MethodGet, uri, nil)
	req.SetBasicAuth(cred.Username, cred.Password)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		writeError(w, 502, err)
		return
	}
	defer resp.Body.Close()
	w.Header().Set("Content-Type", resp.Header.Get("Content-Type"))
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(resp.StatusCode)
	_, _ = io.Copy(w, io.LimitReader(resp.Body, 12<<20))
}
func (s *Server) live(w http.ResponseWriter, r *http.Request) {
	target, _ := url.Parse(s.media.HLSBase())
	proxy := httputil.NewSingleHostReverseProxy(target)
	id := r.PathValue("id")
	asset := r.PathValue("asset")
	if asset == "" {
		asset = "index.m3u8"
	}
	proxy.Director = func(req *http.Request) {
		req.URL.Scheme = target.Scheme
		req.URL.Host = target.Host
		req.URL.Path = "/camera-" + id + "/" + asset
		req.Host = target.Host
		req.Header.Del("Authorization")
	}
	proxy.ServeHTTP(w, r)
}
func (s *Server) detectors(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, detector.Catalog)
}
func (s *Server) listRules(w http.ResponseWriter, r *http.Request) {
	out, err := s.rules.List(r.Context(), r.URL.Query().Get("cameraId"))
	respond(w, out, err)
}
func (s *Server) createRule(w http.ResponseWriter, r *http.Request) {
	var in rules.Rule
	if !decode(w, r, &in) {
		return
	}
	out, err := s.rules.Create(r.Context(), in)
	if err != nil {
		writeError(w, 400, err)
		return
	}
	writeJSON(w, 201, out)
}
func (s *Server) deleteRule(w http.ResponseWriter, r *http.Request) {
	err := s.rules.Delete(r.Context(), r.PathValue("id"))
	if err != nil {
		respond(w, nil, err)
		return
	}
	w.WriteHeader(204)
}
func (s *Server) listEvents(w http.ResponseWriter, r *http.Request) {
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
	out, err := s.events.List(r.Context(), r.URL.Query().Get("cameraId"), limit)
	respond(w, out, err)
}
func (s *Server) getEvent(w http.ResponseWriter, r *http.Request) {
	out, err := s.events.Get(r.Context(), r.PathValue("id"))
	respond(w, out, err)
}
func (s *Server) ackEvent(w http.ResponseWriter, r *http.Request) {
	err := s.events.Acknowledge(r.Context(), r.PathValue("id"), auth.DeviceID(r.Context()))
	if err != nil {
		respond(w, nil, err)
		return
	}
	s.hub.Broadcast(map[string]any{"type": "event.acknowledged", "eventId": r.PathValue("id")})
	w.WriteHeader(204)
}
func (s *Server) eventClip(w http.ResponseWriter, r *http.Request) {
	e, err := s.events.Get(r.Context(), r.PathValue("id"))
	if err != nil {
		respond(w, nil, err)
		return
	}
	if e.ClipPath == "" {
		writeError(w, 404, fmt.Errorf("clip is not ready"))
		return
	}
	w.Header().Set("Content-Type", "video/mp4")
	w.Header().Set("Cache-Control", "private, max-age=3600")
	http.ServeFile(w, r, filepath.Clean(e.ClipPath))
}
func (s *Server) eventSnapshot(w http.ResponseWriter, r *http.Request) {
	e, err := s.events.Get(r.Context(), r.PathValue("id"))
	if err != nil {
		respond(w, nil, err)
		return
	}
	if e.SnapshotPath == "" {
		writeError(w, 404, fmt.Errorf("event snapshot is not ready"))
		return
	}
	w.Header().Set("Cache-Control", "private, max-age=86400")
	http.ServeFile(w, r, filepath.Clean(e.SnapshotPath))
}
func (s *Server) push(w http.ResponseWriter, r *http.Request) {
	var in notify.Registration
	if !decode(w, r, &in) {
		return
	}
	if err := s.notify.Register(r.Context(), auth.DeviceID(r.Context()), in); err != nil {
		writeError(w, 400, err)
		return
	}
	w.WriteHeader(204)
}
func (s *Server) submitDetection(w http.ResponseWriter, r *http.Request) {
	if s.submitter == nil {
		writeError(w, 503, fmt.Errorf("detection pipeline is not ready"))
		return
	}
	var in struct {
		CameraID   string         `json:"cameraId"`
		Type       string         `json:"type"`
		Confidence float64        `json:"confidence"`
		Metadata   map[string]any `json:"metadata"`
	}
	if !decode(w, r, &in) {
		return
	}
	out, err := s.submitter.Submit(r.Context(), rules.Detection{CameraID: in.CameraID, Type: in.Type, Confidence: in.Confidence, OccurredAt: time.Now().UTC(), Metadata: in.Metadata})
	respond(w, out, err)
}

func decode(w http.ResponseWriter, r *http.Request, out any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(out); err != nil {
		writeError(w, 400, fmt.Errorf("invalid request: %w", err))
		return false
	}
	return true
}
func respond(w http.ResponseWriter, value any, err error) {
	if err == nil {
		writeJSON(w, 200, value)
		return
	}
	if errors.Is(err, sql.ErrNoRows) {
		writeError(w, 404, fmt.Errorf("resource not found"))
		return
	}
	writeError(w, 500, err)
}
func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
func writeError(w http.ResponseWriter, status int, err error) {
	writeJSON(w, status, map[string]string{"error": err.Error()})
}
func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		next.ServeHTTP(w, r)
	})
}
func requestLog(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		logger.Info("request", "method", r.Method, "path", r.URL.Path, "duration", time.Since(start))
	})
}
func defaultPort(v int) int {
	if v == 0 {
		return 2020
	}
	return v
}
func origin(r *http.Request) string {
	scheme := "https"
	if r.TLS == nil {
		scheme = "http"
	}
	return scheme + "://" + r.Host
}

var _ = strings.Builder{}
var _ = os.ErrNotExist
