# Valkyris

Valkyris is a local-first home camera monitor for ONVIF Profile S cameras. It
keeps credentials, video analysis and recordings at home while delivering
important events to a native Android app.

> Valkyris is independent software and is not affiliated with TP-Link or Tapo.

## What is included

- Go API with SQLite, encrypted camera credentials, pairing, rules and events.
- ONVIF capability probing and PTZ controls.
- MediaMTX-based LL-HLS gateway and event recording buffer.
- In-process, int8 sherpa-onnx audio classification for amd64 and arm64.
- Kotlin/Compose Android client with live view, rules, event timeline and alarms.
- Bilingual Astro documentation site for Vercel.

## Quick start

Provision the camera on Wi-Fi and create its ONVIF/RTSP camera account first.
Then install Valkyris on a machine that can reach the camera:

```bash
curl -fsSL https://valkyris.vercel.app/install.sh | sh
```

For development:

```bash
cp .env.example .env
docker compose up --build
```

At the end of the installation, open the Android app and enter the HTTPS address
through which the phone reaches Valkyris. The first phone creates the home
password and becomes its administrator; the password is stored only as a bcrypt
hash. From **Settings**, that administrator can generate a single-use QR invite
for another phone. Keep the camera and ports 554/2020 private; use a VPN for
remote access. A reverse proxy such as Caddy can provide the Android-trusted TLS
certificate for a private domain.

## Repository

- `backend/` — Go service and OpenAPI contract
- `mobile/` — native Android app
- `web/` — static landing page and documentation
- `docs/` — architecture and operating notes

Released under the [MIT License](LICENSE).
