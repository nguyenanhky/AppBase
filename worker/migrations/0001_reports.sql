-- User reports of AI-generated results (Google Play requires in-app reporting).
CREATE TABLE IF NOT EXISTS reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  uid TEXT NOT NULL,
  task TEXT NOT NULL,
  result_json TEXT NOT NULL,
  reason TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS reports_created_at ON reports (created_at);
