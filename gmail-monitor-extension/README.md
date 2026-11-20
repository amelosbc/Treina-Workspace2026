# Gmail Keyword Monitor — Chrome Extension

A lightweight Chrome extension that monitors your Gmail inbox for keyword matches and alerts you via push notifications, badge counter, and a popup summary panel.

---

## Features

- 🔍 **Keyword matching** — define any list of keywords to watch for in subject + body
- 🔔 **Push notifications** — browser notification fires immediately on match
- 🔴 **Badge counter** — icon badge shows unread match count at a glance
- 📋 **Popup panel** — clean summary of all matched emails with tags and timestamps
- ⏱️ **5-minute polling** — uses `chrome.alarms` (survives browser restart)
- 🔐 **OAuth2** — authenticates directly with your Google account via `chrome.identity`

---

## Project Structure

```
gmail-monitor-extension/
├── manifest.json        # Extension manifest (MV3)
├── background.js        # Service worker: polling, Gmail API, notifications, badge
├── popup.html           # Popup UI shell
├── popup.css            # Popup styles (dark industrial aesthetic)
├── popup.js             # Popup controller: renders matches, settings
├── generate-icons.js    # Script to generate PNG icons (run once)
├── icons/
│   ├── icon16.png
│   ├── icon32.png
│   ├── icon48.png
│   └── icon128.png
└── README.md
```

---

## Setup

### 1. Create a Google OAuth2 Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select existing)
3. Enable the **Gmail API**: APIs & Services → Library → Gmail API → Enable
4. Go to **APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID**
5. Application type: **Chrome Extension**
6. Under "Item ID", enter your extension ID (see step 3 below to get it)
7. Copy the generated **Client ID**

### 2. Configure the Extension

Open `manifest.json` and replace the placeholder:

```json
"oauth2": {
  "client_id": "YOUR_GOOGLE_OAUTH_CLIENT_ID.apps.googleusercontent.com",
  ...
}
```

### 3. Generate Icons

```bash
npm install canvas
node generate-icons.js
```

This creates `icons/icon16.png`, `icon32.png`, `icon48.png`, `icon128.png`.

> Alternatively, drop your own 16×16, 32×32, 48×48, and 128×128 PNG files into the `icons/` folder.

### 4. Load the Extension in Chrome

1. Open Chrome and navigate to `chrome://extensions`
2. Enable **Developer mode** (top-right toggle)
3. Click **Load unpacked**
4. Select the `gmail-monitor-extension/` folder
5. Note your **Extension ID** shown under the extension card — you'll need it for the OAuth Client ID (step 1.6)

### 5. Authenticate

1. Click the extension icon in the toolbar
2. Open **Settings** (gear icon)
3. Click **Authenticate with Gmail**
4. Complete the Google OAuth flow

---

## Usage

### Setting Keywords

1. Click the extension icon
2. Open Settings (⚙️)
3. Enter one keyword per line in the textarea
4. Click **Save keywords**

Default keywords: `urgent`, `invoice`, `PR review`, `critical`, `asap`

### Reading Matches

- The **popup panel** lists all matched emails with:
  - Subject, sender, snippet
  - Which keywords triggered the match (orange tags)
  - Relative timestamp
  - Unread emails highlighted with an orange left border
- Click **Mark all read** to clear the badge counter

### Manual Poll

Click the **↻ refresh** button in the popup header to trigger an immediate poll outside the 5-minute cycle.

---

## Architecture

```
chrome.alarms (every 5 min)
        │
        ▼
background.js (service worker)
  ├── getAccessToken()       → chrome.identity OAuth2
  ├── gmailGet()             → Gmail REST API v1
  │     └── /messages?q=...  → keyword search query
  ├── fetchMessageDetail()   → get subject, from, snippet
  ├── fireNotification()     → chrome.notifications
  ├── updateBadge()          → chrome.action.setBadgeText
  └── chrome.storage.local   → persist matches, seenIds, keywords
        │
        ▼
popup.js (on open)
  ├── Reads chrome.storage.local
  ├── Renders match list
  └── Sends messages to background (POLL_NOW, MARK_ALL_READ, CLEAR_MATCHES)
```

---

## Permissions Explained

| Permission     | Why it's needed |
|----------------|-----------------|
| `identity`     | OAuth2 login with Google |
| `notifications`| Push notifications on keyword match |
| `alarms`       | Periodic background polling (survives browser restart) |
| `storage`      | Persist matches, keywords, seen IDs |
| `badges`       | Update icon badge count |
| `googleapis.com` | Gmail REST API calls |

---

## Limitations & Next Steps

- **Polling only** — Gmail push (Pub/Sub) would give real-time alerts but requires a backend server
- **100 match cap** — oldest entries are pruned after 100 matches (configurable in `background.js`)
- **Read-only scope** — extension only reads emails, never modifies them
- **Single account** — multi-account support would require managing multiple tokens

### Possible Enhancements

- [ ] Per-keyword notification sound
- [ ] Click notification to open email in Gmail
- [ ] Export matches to CSV
- [ ] Sender-based filtering (VIP list)
- [ ] Gmail Pub/Sub integration for real-time alerts (requires backend)

---

## Development

```bash
# After editing any file, reload the extension:
# chrome://extensions → click the ↻ reload icon on your extension card

# To inspect the service worker logs:
# chrome://extensions → click "Service Worker" link → DevTools opens
```

---

## License

MIT
