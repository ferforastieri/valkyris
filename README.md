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

At the end of the installation, the terminal displays a QR code and a private
local setup URL. Open the Android app, tap **Scan QR code**, and point it at the
terminal or at the setup page. The page is served directly by the Go backend;
it does not depend on the Vercel landing page. Pairing codes are one-time and
expire after ten minutes, while the setup page automatically creates fresh
ones. Keep the camera and ports 554/2020 private; use a VPN for remote access.

## Repository

- `backend/` — Go service and OpenAPI contract
- `mobile/` — native Android app
- `web/` — static landing page and documentation
- `docs/` — architecture and operating notes

Released under the [MIT License](LICENSE).
