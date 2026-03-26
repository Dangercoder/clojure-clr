(ns app.handlers
  (:require [clojure.async.task :as t]))

;; ── Shared HttpClient ──────────────────────────────────────────────────

(def ^:private http-client (System.Net.Http.HttpClient.))

;; ── Async handlers — real network I/O via HttpClient ───────────────────

(defn ^:async fetch-dog-fact []
  (t/await (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")))

(defn ^:async fetch-cat-fact []
  (t/await (.GetStringAsync http-client "https://catfact.ninja/fact")))

(defn ^:async fetch-both []
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
  <p>Entire app in Clojure (<code>app/handlers.clj</code> + <code>app/server.clj</code>).
     C# is only 3 lines of bootstrap.</p>
  <ul>
    <li><a href=\"/dog\">/dog</a> &mdash; defn ^:async + t/await HttpClient</li>
    <li><a href=\"/cat\">/cat</a> &mdash; defn ^:async + t/await HttpClient</li>
    <li><a href=\"/both\">/both</a> &mdash; concurrent fetch (t/await-all)</li>
    <li><a href=\"/status\">/status</a> &mdash; sync handler</li>
  </ul>
  </body></html>")
