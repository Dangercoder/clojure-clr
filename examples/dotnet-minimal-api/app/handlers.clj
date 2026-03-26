(ns app.handlers
  (:require [clojure.async.task :as t]))

;; ── Shared HttpClient ──────────────────────────────────────────────────

(def ^:private http-client (System.Net.Http.HttpClient.))

;; ── Async handlers — real network I/O via HttpClient ───────────────────

(defn ^:async fetch-dog-fact
  "Fetches a random dog fact. Returns JSON string."
  []
  (t/await (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")))

(defn ^:async fetch-cat-fact
  "Fetches a random cat fact. Returns JSON string."
  []
  (t/await (.GetStringAsync http-client "https://catfact.ninja/fact")))

(defn ^:async fetch-both
  "Fetches dog + cat facts concurrently via Task.WhenAll. Returns JSON string."
  []
  (let [dog-task (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")
        cat-task (.GetStringAsync http-client "https://catfact.ninja/fact")
        [a b]   (t/await-all dog-task cat-task)]
    (str "{\"dog\":" a ",\"cat\":" b "}")))

;; ── Sync handlers ──────────────────────────────────────────────────────

(defn status []
  (str "{\"status\":\"ok\",\"runtime\":\"ClojureCLR on .NET " (Environment/Version) "\"}"))

(defn index-html []
  "<html><body style=\"font-family:monospace;max-width:600px;margin:40px auto\">
  <h2>ClojureCLR + .NET 11 Runtime Async</h2>
  <p>All handlers in Clojure (<code>app/handlers.clj</code>),
     using <code>defn ^:async</code> + <code>t/await</code>:</p>
  <ul>
    <li><a href=\"/dog\">/dog</a> &mdash; await HttpClient.GetStringAsync</li>
    <li><a href=\"/cat\">/cat</a> &mdash; await HttpClient.GetStringAsync</li>
    <li><a href=\"/both\">/both</a> &mdash; concurrent fetch (t/await-all)</li>
    <li><a href=\"/status\">/status</a> &mdash; sync handler</li>
  </ul>
  </body></html>")
