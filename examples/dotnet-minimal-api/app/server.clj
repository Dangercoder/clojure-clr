(ns app.server
  (:require [clojure.async.task :as t])
  (:import [Microsoft.AspNetCore.Builder WebApplication EndpointRouteBuilderExtensions]
           [Microsoft.Extensions.Hosting HostingAbstractionsHostExtensions]))

;; ── Routing macros — hide delegate type boilerplate ────────────────────

(defmacro GET [app path & body]
  `(EndpointRouteBuilderExtensions/MapGet ~app ~path
     (gen-delegate |System.Func`1[System.String]| [] (str (do ~@body)))))

(defmacro async-GET [app path & body]
  `(EndpointRouteBuilderExtensions/MapGet ~app ~path
     (gen-delegate |System.Func`1[System.Threading.Tasks.Task`1[System.Object]]| []
       (t/async ~@body))))

;; ── App ────────────────────────────────────────────────────────────────

(def ^:private http-client (System.Net.Http.HttpClient.))

(defn start! [args]
  (let [builder (WebApplication/CreateBuilder (into-array String args))
        app (.Build builder)]

    (GET app "/"
      "<html><body style=\"font-family:monospace;max-width:600px;margin:40px auto\">
       <h2>ClojureCLR + .NET 11 Runtime Async</h2>
       <ul>
         <li><a href=\"/dog\">/dog</a></li>
         <li><a href=\"/cat\">/cat</a></li>
         <li><a href=\"/both\">/both</a></li>
         <li><a href=\"/status\">/status</a></li>
       </ul></body></html>")

    (async-GET app "/dog"
      (t/await (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")))

    (async-GET app "/cat"
      (t/await (.GetStringAsync http-client "https://catfact.ninja/fact")))

    (async-GET app "/both"
      (let [dog-task (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")
            cat-task (.GetStringAsync http-client "https://catfact.ninja/fact")
            [a b] (t/await-all dog-task cat-task)]
        (str "{\"dog\":" a ",\"cat\":" b "}")))

    (GET app "/status"
      (str "{\"status\":\"ok\",\"runtime\":\"ClojureCLR on .NET " (Environment/Version) "\"}" ))

    (HostingAbstractionsHostExtensions/Run app)))
