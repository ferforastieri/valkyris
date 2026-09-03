# Architecture

Valkyris is a single-home system. The Go process is the security boundary: apps
never receive camera passwords and MediaMTX is not published on a host port.

1. The camera is added by address and camera-account credentials.
2. ONVIF probing stores the capabilities, service addresses and media profile token.
3. MediaMTX pulls the selected RTSP profile once and maintains LL-HLS plus a
   short rolling fMP4 buffer.
4. ONVIF events and local audio/video detectors submit normalized detections to
   the rule engine.
5. Matching rules create an event, materialize a 5-second pre-roll and 10-second
   post-roll clip, and enqueue per-device push deliveries. Overlapping events
   extend and share the same clip while retaining their own snapshots.
6. The Android app receives a minimal encrypted signal, then retrieves the
   authenticated event over LAN or VPN.

Camera credentials and push secrets are encrypted with AES-256-GCM. API bearer
tokens are stored only as SHA-256 hashes. A fresh installation has no preset
credential: the first Android client atomically creates the home password and
becomes the administrator. Only a bcrypt hash is retained in SQLite. An
authenticated administrator may then create short-lived, single-use invitations;
the Android app combines the invitation code with its already-known server URL
and renders the QR locally. The Go backend exposes no setup or QR page.
