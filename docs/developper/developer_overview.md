# Developer Overview

This document gives a high level summary of the repository structure and the main concepts behind CSL. It complements the other documents that cover architecture, execution flows and modules in more detail.

## Codebase layout

The source code lives under `src` with `src/main/java` containing the application and `src/main/resources` providing supporting files. Gradle build files (`build.gradle`, `buildClient.gradle`, `buildServer.gradle`) configure dependencies and tasks. Java 17 is used as the toolchain.

Important directories include:

- `docs/` – Markdown documentation for developers. The table of contents is in `docs/README.md` and the documentation is built with MkDocs.
- `gradle/` – Wrapper configuration for Gradle.
- `Dockerfile` and `Dockerfile.srv` – Images for running the client and server.

## Key programs

The project produces two main executables:

- **CSL_CLIENT** – Connects to local modules inside the customer network. It registers services and communicates with external components via WebSocket and MQTT.
- **CSL_SERVER** – Acts as an HTTP-to-WebSocket proxy reachable from outside. It also hosts an optional MQTT broker.

`introduction.md` provides further details on how these pieces interact.

## Configuration and context

Configuration values come from `application.json` and environment variables. They are loaded by the `Config` class. `CSLContext` is a singleton that builds the services, registers them and starts the network servers depending on whether the process is running in client or server mode.

## Entry points

- `CSLIDSMainClient` bootstraps the client, opens the WebSocket connection and optionally exposes an HTTP API server.
- `CSLIDSMainServer` runs the server mode and starts Jetty and the MQTT broker if configured.

## Services and endpoints

Each feature is implemented as a service derived from `Service`. Services add JSON commands through the `addCmd` method so they can be called over HTTP or WebSockets. Examples are `AutoCryptService` for certificate management and `DiscoveryServices` for device scanning.

## Execution flow and threading

`execution_flows.md` gives a step-by-step sequence of client start-up and explains periodic synchronization tasks. A dedicated document lists the many background threads used for WebSocket keepalive, UDP processing, module synchronization and more.

---

This overview should help orient new contributors before diving into the specific developer guides referenced in the table of contents.
