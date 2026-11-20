/**
 * @file background.js
 * @description Service worker for Gmail Keyword Monitor extension.
 *
 * Responsibilities:
 *  - Authenticate with Gmail via OAuth2 (chrome.identity)
 *  - Poll Gmail API every 5 minutes using chrome.alarms
 *  - Match incoming emails against user-configured keywords
 *  - Fire browser push notifications for matches
 *  - Update the extension badge with the unread match count
 *  - Persist matched emails to chrome.storage.local for the popup panel
 */

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ALARM_NAME = "gmail-poll";
const POLL_INTERVAL_MINUTES = 5;
const GMAIL_SEARCH_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/messages";
const GMAIL_MESSAGE_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/messages";

/** Storage keys */
const STORAGE_KEYS = {
  MATCHES: "matches",          // EmailMatch[]
  LAST_POLL: "lastPoll",       // ISO timestamp
  KEYWORDS: "keywords",        // string[]
  SEEN_IDS: "seenIds",         // string[]  — already-notified message IDs
  UNREAD_COUNT: "unreadCount", // number
};

/** Default keywords if the user hasn't configured any yet */
const DEFAULT_KEYWORDS = ["urgent", "invoice", "PR review", "critical", "asap"];

// ---------------------------------------------------------------------------
// Types (JSDoc)
// ---------------------------------------------------------------------------

/**
 * @typedef {Object} EmailMatch
 * @property {string} id          - Gmail message ID
 * @property {string} subject     - Email subject
 * @property {string} from        - Sender name/address
 * @property {string} snippet     - Gmail snippet (first ~100 chars of body)
 * @property {string[]} keywords  - Which keywords triggered this match
 * @property {string} receivedAt  - ISO timestamp
 * @property {boolean} read       - Whether the user dismissed it in the popup
 */

// ---------------------------------------------------------------------------
// Initialisation
// ---------------------------------------------------------------------------

chrome.runtime.onInstalled.addListener(async () => {
  console.log("[GmailMonitor] Extension installed — initialising defaults.");

  await chrome.storage.local.set({
    [STORAGE_KEYS.KEYWORDS]: DEFAULT_KEYWORDS,
    [STORAGE_KEYS.MATCHES]: [],
    [STORAGE_KEYS.SEEN_IDS]: [],
    [STORAGE_KEYS.UNREAD_COUNT]: 0,
  });

  scheduleAlarm();
});

chrome.runtime.onStartup.addListener(() => {
  console.log("[GmailMonitor] Browser started — re-scheduling alarm.");
  scheduleAlarm();
});

// Re-create alarm if it was somehow cleared (e.g. extension update)
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === ALARM_NAME) {
    pollGmail();
  }
});

// ---------------------------------------------------------------------------
// Alarm scheduling
// ---------------------------------------------------------------------------

/**
 * Creates (or recreates) the periodic alarm used to trigger Gmail polling.
 */
function scheduleAlarm() {
  chrome.alarms.create(ALARM_NAME, {
    delayInMinutes: 0.1,          // first run after ~6 seconds
    periodInMinutes: POLL_INTERVAL_MINUTES,
  });
  console.log(`[GmailMonitor] Alarm scheduled every ${POLL_INTERVAL_MINUTES} min.`);
}

// ---------------------------------------------------------------------------
// OAuth2 token retrieval
// ---------------------------------------------------------------------------

/**
 * Retrieves a valid OAuth2 access token via chrome.identity.
 *
 * @param {boolean} [interactive=false] - Whether to show the sign-in UI if needed.
 * @returns {Promise<string>} The access token.
 * @throws {Error} If authentication fails.
 */
async function getAccessToken(interactive = false) {
  return new Promise((resolve, reject) => {
    chrome.identity.getAuthToken({ interactive }, (token) => {
      if (chrome.runtime.lastError || !token) {
        reject(new Error(chrome.runtime.lastError?.message ?? "Failed to get auth token"));
      } else {
        resolve(token);
      }
    });
  });
}

/**
 * Removes a cached token and fetches a fresh one (used after 401 responses).
 *
 * @param {string} staleToken
 * @returns {Promise<string>}
 */
async function refreshToken(staleToken) {
  await new Promise((resolve) => chrome.identity.removeCachedAuthToken({ token: staleToken }, resolve));
  return getAccessToken(false);
}

// ---------------------------------------------------------------------------
// Gmail API helpers
// ---------------------------------------------------------------------------

/**
 * Generic authenticated GET wrapper for the Gmail REST API.
 *
 * @param {string} url
 * @param {string} token
 * @returns {Promise<Object>}
 */
async function gmailGet(url, token) {
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (response.status === 401) {
    // Token expired — refresh and retry once
    const newToken = await refreshToken(token);
    const retryResponse = await fetch(url, {
      headers: { Authorization: `Bearer ${newToken}` },
    });
    if (!retryResponse.ok) throw new Error(`Gmail API error: ${retryResponse.status}`);
    return retryResponse.json();
  }

  if (!response.ok) throw new Error(`Gmail API error: ${response.status}`);
  return response.json();
}

/**
 * Builds a Gmail search query string from the given keywords.
 * Looks for matches in subject OR body, received after the last poll.
 *
 * @param {string[]} keywords
 * @param {string|null} afterTimestamp - ISO string; if provided adds an `after:` filter
 * @returns {string} Gmail query string
 */
function buildQuery(keywords, afterTimestamp) {
  const keywordQuery = keywords
    .map((kw) => `(subject:"${kw}" OR "${kw}")`)
    .join(" OR ");

  if (afterTimestamp) {
    // Gmail `after:` uses Unix epoch seconds
    const epochSeconds = Math.floor(new Date(afterTimestamp).getTime() / 1000);
    return `(${keywordQuery}) after:${epochSeconds}`;
  }

  // First run: look back 24 hours
  const oneDayAgo = Math.floor((Date.now() - 86_400_000) / 1000);
  return `(${keywordQuery}) after:${oneDayAgo}`;
}

/**
 * Fetches full message metadata (headers + snippet) for a given message ID.
 *
 * @param {string} messageId
 * @param {string} token
 * @returns {Promise<EmailMatch|null>}
 */
async function fetchMessageDetail(messageId, token, matchedKeywords) {
  try {
    const data = await gmailGet(
      `${GMAIL_MESSAGE_ENDPOINT}/${messageId}?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date`,
      token
    );

    const headers = data.payload?.headers ?? [];
    const getHeader = (name) =>
      headers.find((h) => h.name.toLowerCase() === name.toLowerCase())?.value ?? "";

    const receivedAt = getHeader("Date")
      ? new Date(getHeader("Date")).toISOString()
      : new Date().toISOString();

    return {
      id: messageId,
      subject: getHeader("Subject") || "(no subject)",
      from: getHeader("From") || "Unknown sender",
      snippet: data.snippet ?? "",
      keywords: matchedKeywords,
      receivedAt,
      read: false,
    };
  } catch (err) {
    console.warn(`[GmailMonitor] Could not fetch detail for ${messageId}:`, err);
    return null;
  }
}

// ---------------------------------------------------------------------------
// Core polling logic
// ---------------------------------------------------------------------------

/**
 * Main poll cycle — called by the alarm handler every 5 minutes.
 * Searches Gmail, filters already-seen IDs, fetches details,
 * fires notifications, updates badge, and persists to storage.
 */
async function pollGmail() {
  console.log("[GmailMonitor] Poll cycle started:", new Date().toISOString());

  let token;
  try {
    token = await getAccessToken(false);
  } catch (err) {
    console.warn("[GmailMonitor] Not authenticated — skipping poll.", err.message);
    return;
  }

  const stored = await chrome.storage.local.get([
    STORAGE_KEYS.KEYWORDS,
    STORAGE_KEYS.MATCHES,
    STORAGE_KEYS.SEEN_IDS,
    STORAGE_KEYS.LAST_POLL,
    STORAGE_KEYS.UNREAD_COUNT,
  ]);

  const keywords = stored[STORAGE_KEYS.KEYWORDS] ?? DEFAULT_KEYWORDS;
  const existingMatches = stored[STORAGE_KEYS.MATCHES] ?? [];
  const seenIds = new Set(stored[STORAGE_KEYS.SEEN_IDS] ?? []);
  const lastPoll = stored[STORAGE_KEYS.LAST_POLL] ?? null;

  if (keywords.length === 0) {
    console.log("[GmailMonitor] No keywords configured — skipping poll.");
    return;
  }

  const query = buildQuery(keywords, lastPoll);
  console.log("[GmailMonitor] Gmail query:", query);

  let messageList;
  try {
    const url = `${GMAIL_SEARCH_ENDPOINT}?q=${encodeURIComponent(query)}&maxResults=20`;
    messageList = await gmailGet(url, token);
  } catch (err) {
    console.error("[GmailMonitor] Failed to fetch message list:", err);
    return;
  }

  const messages = messageList.messages ?? [];
  const newMessages = messages.filter((m) => !seenIds.has(m.id));

  console.log(`[GmailMonitor] ${messages.length} results, ${newMessages.length} new.`);

  if (newMessages.length === 0) {
    await chrome.storage.local.set({ [STORAGE_KEYS.LAST_POLL]: new Date().toISOString() });
    return;
  }

  // Determine which keywords each message matched by re-checking subject/snippet
  // (Gmail already filtered, but we want to annotate which keywords fired)
  const newMatches = [];

  for (const msg of newMessages) {
    const matchedKeywords = keywords; // Gmail already confirmed at least one matched
    const detail = await fetchMessageDetail(msg.id, token, matchedKeywords);
    if (!detail) continue;

    // Refine: check which keywords actually appear in subject or snippet
    const lowerText = `${detail.subject} ${detail.snippet}`.toLowerCase();
    detail.keywords = keywords.filter((kw) => lowerText.includes(kw.toLowerCase()));

    newMatches.push(detail);
    seenIds.add(msg.id);

    // Fire a browser notification per new match
    fireNotification(detail);
  }

  const allMatches = [...newMatches, ...existingMatches].slice(0, 100); // cap at 100 entries
  const unreadCount = allMatches.filter((m) => !m.read).length;

  // Update badge
  updateBadge(unreadCount);

  // Persist
  await chrome.storage.local.set({
    [STORAGE_KEYS.MATCHES]: allMatches,
    [STORAGE_KEYS.SEEN_IDS]: [...seenIds],
    [STORAGE_KEYS.LAST_POLL]: new Date().toISOString(),
    [STORAGE_KEYS.UNREAD_COUNT]: unreadCount,
  });

  console.log(`[GmailMonitor] Poll complete. ${newMatches.length} new matches stored.`);
}

// ---------------------------------------------------------------------------
// Notification helpers
// ---------------------------------------------------------------------------

/**
 * Fires a Chrome push notification for a single email match.
 *
 * @param {EmailMatch} match
 */
function fireNotification(match) {
  const notifId = `gmail-match-${match.id}`;
  const keywordLabel = match.keywords.slice(0, 3).join(", ");

  chrome.notifications.create(notifId, {
    type: "basic",
    iconUrl: "icons/icon128.png",
    title: `📧 Keyword match: ${keywordLabel}`,
    message: `From: ${match.from}\n${match.subject}`,
    contextMessage: match.snippet.slice(0, 80),
    priority: 2,
  });
}

// ---------------------------------------------------------------------------
// Badge helpers
// ---------------------------------------------------------------------------

/**
 * Updates the extension icon badge with the current unread match count.
 *
 * @param {number} count
 */
function updateBadge(count) {
  const text = count > 0 ? (count > 99 ? "99+" : String(count)) : "";
  chrome.action.setBadgeText({ text });
  chrome.action.setBadgeBackgroundColor({ color: count > 0 ? "#E53935" : "#9E9E9E" });
}

// ---------------------------------------------------------------------------
// Message listener (for popup communication)
// ---------------------------------------------------------------------------

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  switch (message.type) {
    case "POLL_NOW":
      pollGmail().then(() => sendResponse({ ok: true }));
      return true; // async

    case "MARK_ALL_READ":
      markAllRead().then(() => sendResponse({ ok: true }));
      return true;

    case "CLEAR_MATCHES":
      clearMatches().then(() => sendResponse({ ok: true }));
      return true;

    default:
      break;
  }
});

/**
 * Marks all stored matches as read and resets the badge.
 */
async function markAllRead() {
  const stored = await chrome.storage.local.get(STORAGE_KEYS.MATCHES);
  const matches = (stored[STORAGE_KEYS.MATCHES] ?? []).map((m) => ({ ...m, read: true }));
  await chrome.storage.local.set({
    [STORAGE_KEYS.MATCHES]: matches,
    [STORAGE_KEYS.UNREAD_COUNT]: 0,
  });
  updateBadge(0);
}

/**
 * Clears all stored matches.
 */
async function clearMatches() {
  await chrome.storage.local.set({
    [STORAGE_KEYS.MATCHES]: [],
    [STORAGE_KEYS.UNREAD_COUNT]: 0,
  });
  updateBadge(0);
}
