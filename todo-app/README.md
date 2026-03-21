# Todo App

## Objective

A simple Todo List web application built to explore and validate a full development workflow using **Claude Code CLI**, **WSL2 Linux**, **Docker Desktop**, **GitHub**, and **Playwright MCP**. The app supports basic task management with add and remove operations.

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
| Playwright MCP | Browser-based UI testing via Claude Code |

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
6. **Browser testing with Playwright MCP** — Tested the running app end-to-end using Claude Code's Playwright MCP integration

## Testing with Playwright MCP

The app was tested interactively using the **Playwright MCP server** connected to Claude Code. This allows Claude to open a real browser, navigate the app, and interact with UI elements — all from the CLI.

### Prerequisites

- Docker Desktop running (the app must be up on `http://localhost:3000`)
- Playwright MCP server configured in Claude Code (via `settings.json` or MCP config)

### Steps

1. **Start the app in Docker**

   ```bash
   cd todo-app
   docker compose up -d --build
   ```

2. **Open the app in the Playwright browser**

   Ask Claude Code to navigate to `http://localhost:3000`. Claude uses the `browser_navigate` MCP tool to open the page and returns a snapshot of the UI.

3. **Add tasks**

   Ask Claude to type a task name (e.g., "Buy groceries") in the input field and click the "Add" button. Repeat for multiple tasks. Claude uses `browser_fill_form` and `browser_click` to interact with the page.

4. **Verify tasks appear**

   After each add, Claude takes a page snapshot (`browser_snapshot`) and confirms the new task is listed.

5. **Remove a task**

   Ask Claude to click the "Remove" button next to a specific task. Claude confirms the task is deleted from the list.

6. **Stop the app**

   ```bash
   docker compose down
   ```

### Test Results

| Action | Input | Result |
|--------|-------|--------|
| Add task | "Buy groceries" | Task appeared in the list |
| Add task | "Learn Docker" | Task appeared in the list |
| Add task | "Test Playwright MCP" | Task appeared in the list |
| Remove task | "Learn Docker" | Task removed, 2 remaining |

All CRUD operations (Create, Read, Delete) worked as expected via the browser UI.

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
