# Treina-Workspace2026

Workspaces de diversos Treinamentos em 2026

---

## Todo App

### Objective

A simple Todo List web application built to explore and validate a full development workflow using **Claude Code CLI**, **WSL2 Linux**, **Docker Desktop**, and **GitHub**. The app supports basic task management with add and remove operations.

### Tech Stack

- **Node.js** + **Express** — Backend REST API
- **HTML/CSS/JavaScript** — Frontend (single-page, no framework)
- **Docker** — Containerized deployment via Docker Compose

### Development Environment

| Tool | Role |
|------|------|
| Claude Code (CLI) | AI-assisted development — scaffolded the app, configured Docker, and managed Git |
| WSL2 (Ubuntu on Windows) | Linux development environment |
| Docker Desktop (Windows) | Container runtime integrated with WSL2 |
| GitHub CLI (`gh`) | Repository creation and management |

### Steps to Reproduce

1. **GitHub setup** — Configured `git` identity and authenticated `gh` CLI
2. **Repository creation** — Created `Treina-Workspace2026` public repo via `gh repo create`
3. **Application code** — Built the Express server (`server.js`) with three API endpoints:
   - `GET /api/todos` — List all tasks
   - `POST /api/todos` — Add a new task
   - `DELETE /api/todos/:id` — Remove a task
4. **Frontend** — Single HTML page (`public/index.html`) with inline CSS and JavaScript
5. **Dockerization** — Created `Dockerfile` (Node 20 Alpine) and `docker-compose.yml`
6. **Build and run** from WSL terminal:
   ```bash
   cd todo-app
   docker compose build
   docker compose up -d
   ```
7. **Access the app** at `http://localhost:3000`

### Project Structure

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
