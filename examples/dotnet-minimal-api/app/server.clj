(ns app.server
  (:require [clojure.async.task :as t])
  (:import [Microsoft.AspNetCore.Builder WebApplication EndpointRouteBuilderExtensions]
           [Microsoft.Extensions.Hosting HostingAbstractionsHostExtensions]))

(def ^:private http-client (System.Net.Http.HttpClient.))

(defn start! [args]
  (let [builder (WebApplication/CreateBuilder (into-array String args))
        app (.Build builder)]

    (EndpointRouteBuilderExtensions/MapGet app "/"
      (gen-delegate |System.Func`1[System.String]| []
        "<html><body style=\"font-family:monospace;max-width:600px;margin:40px auto\">
         <h2>ClojureCLR + .NET 11 Runtime Async</h2>
         <ul>
           <li><a href=\"/dog\">/dog</a></li>
           <li><a href=\"/cat\">/cat</a></li>
           <li><a href=\"/both\">/both</a></li>
           <li><a href=\"/status\">/status</a></li>
         </ul></body></html>"))

    (EndpointRouteBuilderExtensions/MapGet app "/dog"
      (gen-delegate |System.Func`1[System.Threading.Tasks.Task`1[System.Object]]| []
        (t/async (t/await (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")))))

    (EndpointRouteBuilderExtensions/MapGet app "/cat"
      (gen-delegate |System.Func`1[System.Threading.Tasks.Task`1[System.Object]]| []
        (t/async (t/await (.GetStringAsync http-client "https://catfact.ninja/fact")))))

    (EndpointRouteBuilderExtensions/MapGet app "/both"
      (gen-delegate |System.Func`1[System.Threading.Tasks.Task`1[System.Object]]| []
        (t/async
          (let [dog-task (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")
                cat-task (.GetStringAsync http-client "https://catfact.ninja/fact")
                [a b] (t/await-all dog-task cat-task)]
            (str "{\"dog\":" a ",\"cat\":" b "}")))))

    (EndpointRouteBuilderExtensions/MapGet app "/status"
      (gen-delegate |System.Func`1[System.String]| []
        (str "{\"status\":\"ok\",\"runtime\":\"ClojureCLR on .NET " (Environment/Version) "\"}")))

    (HostingAbstractionsHostExtensions/Run app)))
