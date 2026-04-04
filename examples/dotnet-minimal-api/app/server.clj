(ns app.server
  (:require [clojure.clr.async.task :as t])
  (:import [Microsoft.AspNetCore.Builder WebApplication EndpointRouteBuilderExtensions]
           [Microsoft.Extensions.Hosting HostingAbstractionsHostExtensions]
           [Microsoft.Data.Sqlite SqliteConnection]
           [Dapper SqlMapper]))

;; ── Routing macros ─────────────────────────────────────────────────────

(defmacro GET [app path & body]
  `(EndpointRouteBuilderExtensions/MapGet ~app ~path
     (gen-delegate |System.Func`1[System.String]| [] (str (do ~@body)))))

(defmacro async-GET [app path & body]
  `(EndpointRouteBuilderExtensions/MapGet ~app ~path
     (gen-delegate |System.Func`1[System.Threading.Tasks.Task`1[System.Object]]| []
       (t/async ~@body))))

;; ── Database helpers ───────────────────────────────────────────────────

(def ^:private conn-string "Data Source=app.db")

(defn- open-conn []
  (doto (SqliteConnection. conn-string) (.Open)))

(defn- execute-async
  "Dapper ExecuteAsync — runs SQL, returns Task<int>."
  [conn sql param]
  (SqlMapper/ExecuteAsync conn sql param nil nil nil))

(defn- query-async
  "Dapper QueryAsync (non-generic) — runs SQL, returns Task<IEnumerable<dynamic>>."
  [conn sql param]
  (SqlMapper/QueryAsync conn sql param nil nil nil))

(defn- rows->json
  "Converts Dapper dynamic rows to a JSON array string."
  [rows]
  (let [row->map (fn [row]
                   (let [dict (cast |System.Collections.Generic.IDictionary`2[System.String,System.Object]| row)]
                     (str "{"
                       (clojure.string/join ","
                         (map (fn [kv]
                                (str "\"" (.Key kv) "\":" (pr-str (.Value kv))))
                              dict))
                       "}")))]
    (str "[" (clojure.string/join "," (map row->map rows)) "]")))

;; ── Shared HttpClient ──────────────────────────────────────────────────

(def ^:private http-client (System.Net.Http.HttpClient.))

;; ── App ────────────────────────────────────────────────────────────────

(defn start! [args]
  (let [builder (WebApplication/CreateBuilder (into-array String args))
        app (.Build builder)]

    (GET app "/"
      "<html><body style=\"font-family:monospace;max-width:600px;margin:40px auto\">
       <h2>ClojureCLR + .NET 11 Async Interop</h2>
       <h3>HTTP</h3>
       <ul>
         <li><a href=\"/dog\">/dog</a> — async HttpClient</li>
         <li><a href=\"/cat\">/cat</a> — async HttpClient</li>
         <li><a href=\"/both\">/both</a> — concurrent Task.WhenAll</li>
       </ul>
       <h3>Database (Dapper + SQLite)</h3>
       <ul>
         <li><a href=\"/db/init\">/db/init</a> — create table + seed</li>
         <li><a href=\"/db/list\">/db/list</a> — async SELECT all</li>
         <li><a href=\"/db/add?title=Hello\">/db/add?title=...</a> — async INSERT</li>
       </ul>
       </body></html>")

    ;; ── HTTP routes ──────────────────────────────────────────────────

    (async-GET app "/dog"
      (t/await (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")))

    (async-GET app "/cat"
      (t/await (.GetStringAsync http-client "https://catfact.ninja/fact")))

    (async-GET app "/both"
      (let [dog-task (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")
            cat-task (.GetStringAsync http-client "https://catfact.ninja/fact")
            results (t/await (t/await-all [dog-task cat-task]))
            a (aget results 0)
            b (aget results 1)]
        (str "{\"dog\":" a ",\"cat\":" b "}")))

    ;; ── Database routes ──────────────────────────────────────────────

    (async-GET app "/db/init"
      (let [conn (open-conn)]
        (t/await (execute-async conn
                   "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, done INTEGER DEFAULT 0)"
                   nil))
        (t/await (execute-async conn "DELETE FROM todos" nil))
        (t/await (execute-async conn "INSERT INTO todos (title) VALUES ('Buy groceries')" nil))
        (t/await (execute-async conn "INSERT INTO todos (title) VALUES ('Write ClojureCLR app')" nil))
        (t/await (execute-async conn "INSERT INTO todos (title, done) VALUES ('Patch GenDelegate', 1)" nil))
        (.Close conn)
        "{\"status\":\"ok\",\"message\":\"table created and seeded\"}"))

    (async-GET app "/db/list"
      (let [conn (open-conn)
            rows (t/await (query-async conn "SELECT id, title, done FROM todos ORDER BY id" nil))
            json (rows->json rows)]
        (.Close conn)
        json))

    (EndpointRouteBuilderExtensions/MapGet app "/db/add"
      (gen-delegate |System.Func`2[Microsoft.AspNetCore.Http.HttpContext,System.Threading.Tasks.Task`1[System.Object]]| [ctx]
        (t/async
          (let [title (str (.. ctx -Request -Query (get_Item "title")))
                conn (open-conn)
                dp (doto (Dapper.DynamicParameters.) (.Add "title" title nil nil nil))
                _ (t/await (execute-async conn "INSERT INTO todos (title) VALUES (@title)" dp))]
            (.Close conn)
            (str "{\"status\":\"ok\",\"inserted\":\"" title "\"}")))))

    (GET app "/status"
      (str "{\"status\":\"ok\",\"runtime\":\"ClojureCLR on .NET " (Environment/Version) "\"}"))

    (HostingAbstractionsHostExtensions/Run app)))
