# Todo App

## Objective

A simple Todo List web application built to explore and validate a full development workflow using **Claude Code CLI**, **WSL2 Linux**, **Docker Desktop**, and **GitHub**. The app supports basic task management with add and remove operations.

## Tech Stack

- **Node.js** + **Express** — Backend REST API
- **HTML/CSS/JavaScript** — Frontend (single-page, no framework)
- **Docker** — Containerized deployment via Docker Compose

## Development Environment

| Tool | Role |
|------|------|
| Claude Code (CLI) | AI-assisted development — scaffolded the app, configured Docker, and managed Git |
| WSL2 (Ubuntu on Windows) | Linux development environment |
| Docker Desktop (Windows) | Container runtime integrated with WSL2 |
| GitHub CLI (`gh`) | Repository creation and management |

## How to Run

### Option 1 — Local (Node.js)

Requires Node.js 20+ installed on your machine.

```bash
cd todo-app
npm install
npm start
```

### Option 2 — Docker

Requires Docker Desktop (or Docker Engine) running.

```bash
cd todo-app
docker compose build
docker compose up -d
```

To stop the container:

```bash
docker compose down
```

Both options serve the app at **http://localhost:3000**.

> **Note:** Only run one option at a time — both use port 3000.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/todos` | List all tasks |
| `POST` | `/api/todos` | Add a new task (`{ "title": "..." }`) |
| `DELETE` | `/api/todos/:id` | Remove a task by ID |

## Steps to Reproduce (from scratch)

1. **GitHub setup** — Configured `git` identity and authenticated `gh` CLI
2. **Repository creation** — Created `Treina-Workspace2026` public repo via `gh repo create`
3. **Application code** — Built the Express server with three REST endpoints
4. **Frontend** — Single HTML page (`public/index.html`) with inline CSS and JavaScript
5. **Dockerization** — Created `Dockerfile` (Node 20 Alpine) and `docker-compose.yml`

## Project Structure

```
todo-app/
├── server.js            # Express API server
├── public/
│   └── index.html       # Frontend UI
├── package.json         # Node.js dependencies
├── Dockerfile           # Container image definition
├── docker-compose.yml   # Container orchestration
└── .dockerignore        # Files excluded from Docker build
```
