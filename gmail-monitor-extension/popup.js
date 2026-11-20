/**
 * @file popup.js
 * @description Controller for the Gmail Monitor extension popup.
 *
 * Reads persisted state from chrome.storage.local and renders:
 *  - Stats bar (total / unread / keyword count)
 *  - Matched email list with keyword tags, sender, subject, snippet, timestamp
 *  - Settings panel (keyword editor, clear button, auth trigger)
 */

// ---------------------------------------------------------------------------
// DOM references
// ---------------------------------------------------------------------------

const $ = (id) => document.getElementById(id);

const lastPollLabel  = $("lastPollLabel");
const statTotal      = $("statTotal");
const statUnread     = $("statUnread");
const statKeywords   = $("statKeywords");
const emptyState     = $("emptyState");
const matchList      = $("matchList");
const panelMatches   = $("panelMatches");
const panelSettings  = $("panelSettings");
const keywordsInput  = $("keywordsInput");
const btnRefresh     = $("btnRefresh");
const btnSettings    = $("btnSettings");
const btnMarkRead    = $("btnMarkRead");
const btnSaveKeywords = $("btnSaveKeywords");
const btnClearAll    = $("btnClearAll");
const btnAuth        = $("btnAuth");

// ---------------------------------------------------------------------------
// Storage keys (must match background.js)
// ---------------------------------------------------------------------------

const STORAGE_KEYS = {
  MATCHES:      "matches",
  LAST_POLL:    "lastPoll",
  KEYWORDS:     "keywords",
  UNREAD_COUNT: "unreadCount",
};

// ---------------------------------------------------------------------------
// Bootstrap
// ---------------------------------------------------------------------------

document.addEventListener("DOMContentLoaded", async () => {
  await renderAll();
  bindEvents();
});

// ---------------------------------------------------------------------------
// Render
// ---------------------------------------------------------------------------

/**
 * Loads all state from storage and re-renders every panel.
 */
async function renderAll() {
  const stored = await chrome.storage.local.get(Object.values(STORAGE_KEYS));

  const matches   = stored[STORAGE_KEYS.MATCHES]      ?? [];
  const lastPoll  = stored[STORAGE_KEYS.LAST_POLL]    ?? null;
  const keywords  = stored[STORAGE_KEYS.KEYWORDS]     ?? [];
  const unread    = matches.filter((m) => !m.read).length;

  renderStats({ total: matches.length, unread, keywordCount: keywords.length });
  renderLastPoll(lastPoll);
  renderMatches(matches);
  renderKeywordsTextarea(keywords);
}

/**
 * Updates the three stat counters.
 */
function renderStats({ total, unread, keywordCount }) {
  statTotal.textContent    = total;
  statUnread.textContent   = unread;
  statKeywords.textContent = keywordCount;
}

/**
 * Renders the "last polled" timestamp label in the header.
 */
function renderLastPoll(isoTimestamp) {
  if (!isoTimestamp) {
    lastPollLabel.textContent = "Not polled yet";
    return;
  }
  const date = new Date(isoTimestamp);
  lastPollLabel.textContent = `Last poll: ${formatRelative(date)}`;
}

/**
 * Renders the list of email matches (or the empty state).
 *
 * @param {import('./background.js').EmailMatch[]} matches
 */
function renderMatches(matches) {
  matchList.innerHTML = "";

  if (matches.length === 0) {
    emptyState.classList.remove("hidden");
    matchList.classList.add("hidden");
    return;
  }

  emptyState.classList.add("hidden");
  matchList.classList.remove("hidden");

  matches.forEach((match) => {
    const li = document.createElement("li");
    li.className = `match-item${match.read ? "" : " unread"}`;
    li.innerHTML = `
      <div class="match-subject" title="${escapeHtml(match.subject)}">${escapeHtml(match.subject)}</div>
      <div class="match-from" title="${escapeHtml(match.from)}">${escapeHtml(match.from)}</div>
      <div class="match-snippet">${escapeHtml(match.snippet)}</div>
      <div class="match-footer">
        <div class="match-tags">${renderTags(match.keywords)}</div>
        <span class="match-time">${formatRelative(new Date(match.receivedAt))}</span>
      </div>
    `;
    matchList.appendChild(li);
  });
}

/**
 * Renders keyword pill tags for a single match.
 *
 * @param {string[]} keywords
 * @returns {string} HTML string
 */
function renderTags(keywords) {
  return keywords
    .slice(0, 4)
    .map((kw) => `<span class="tag">${escapeHtml(kw)}</span>`)
    .join("");
}

/**
 * Populates the keywords textarea in the settings panel.
 *
 * @param {string[]} keywords
 */
function renderKeywordsTextarea(keywords) {
  keywordsInput.value = keywords.join("\n");
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

function bindEvents() {
  // Toggle settings panel
  btnSettings.addEventListener("click", () => {
    const isOpen = !panelSettings.classList.contains("hidden");
    panelSettings.classList.toggle("hidden", isOpen);
    panelMatches.classList.toggle("hidden", !isOpen);
    $("footer").classList.toggle("hidden", !isOpen);
    btnSettings.classList.toggle("active", !isOpen);
  });

  // Manual poll
  btnRefresh.addEventListener("click", async () => {
    btnRefresh.querySelector("svg").parentElement.classList.add("spinning");
    btnRefresh.disabled = true;

    chrome.runtime.sendMessage({ type: "POLL_NOW" }, async () => {
      await renderAll();
      btnRefresh.querySelector("svg").parentElement.classList.remove("spinning");
      btnRefresh.disabled = false;
      showToast("Polled!");
    });
  });

  // Mark all read
  btnMarkRead.addEventListener("click", async () => {
    chrome.runtime.sendMessage({ type: "MARK_ALL_READ" }, async () => {
      await renderAll();
      showToast("All marked as read");
    });
  });

  // Save keywords
  btnSaveKeywords.addEventListener("click", async () => {
    const raw = keywordsInput.value;
    const keywords = raw
      .split("\n")
      .map((kw) => kw.trim())
      .filter(Boolean);

    await chrome.storage.local.set({ [STORAGE_KEYS.KEYWORDS]: keywords });
    await renderAll();
    showToast("Keywords saved ✓");
  });

  // Clear all matches
  btnClearAll.addEventListener("click", async () => {
    chrome.runtime.sendMessage({ type: "CLEAR_MATCHES" }, async () => {
      await renderAll();
      showToast("Cleared");
    });
  });

  // Authenticate with Gmail
  btnAuth.addEventListener("click", () => {
    chrome.identity.getAuthToken({ interactive: true }, (token) => {
      if (chrome.runtime.lastError || !token) {
        showToast("Auth failed");
      } else {
        showToast("Authenticated ✓");
      }
    });
  });
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

/**
 * Returns a human-readable relative time string.
 * E.g. "2 min ago", "1 hr ago", "3 days ago"
 *
 * @param {Date} date
 * @returns {string}
 */
function formatRelative(date) {
  const diffMs   = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60_000);

  if (diffMins < 1)   return "just now";
  if (diffMins < 60)  return `${diffMins} min ago`;

  const diffHrs = Math.floor(diffMins / 60);
  if (diffHrs < 24)   return `${diffHrs} hr ago`;

  const diffDays = Math.floor(diffHrs / 24);
  return `${diffDays} day${diffDays !== 1 ? "s" : ""} ago`;
}

/**
 * Escapes HTML special characters to prevent XSS.
 *
 * @param {string} str
 * @returns {string}
 */
function escapeHtml(str = "") {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

/**
 * Shows a transient toast message at the bottom of the popup.
 *
 * @param {string} message
 */
function showToast(message) {
  let toast = document.querySelector(".toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.className = "toast";
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.classList.add("show");
  setTimeout(() => toast.classList.remove("show"), 1800);
}
