# Camtacte

Camtacte is a local-first home camera monitor for ONVIF Profile S cameras. It
keeps credentials, video analysis and recordings at home while delivering
important events to a native Android app.

> Camtacte is independent software and is not affiliated with TP-Link or Tapo.

## What is included

- Go API with SQLite, encrypted camera credentials, pairing, rules and events.
- ONVIF capability probing and PTZ controls.
- MediaMTX-based LL-HLS gateway and event recording buffer.
- In-process, int8 sherpa-onnx audio classification for amd64 and arm64.
- Kotlin/Compose Android client with live view, rules, event timeline and alarms.
- Bilingual Astro documentation site for Vercel.

## Quick start

Provision the camera on Wi-Fi and create its ONVIF/RTSP camera account first.
Then install Camtacte on a machine that can reach the camera:

```bash
curl -fsSL https://camtacte.vercel.app/install.sh | sh
```

For development:

```bash
cp .env.example .env
docker compose up --build
```

Open `https://localhost:8443/health`. The first-run pairing code is printed once
to the backend logs. Keep the camera and ports 554/2020 private; use a VPN for
remote access.

## Repository

- `backend/` — Go service and OpenAPI contract
- `mobile/` — native Android app
- `web/` — static landing page and documentation
- `docs/` — architecture and operating notes

Released under the [MIT License](LICENSE).
