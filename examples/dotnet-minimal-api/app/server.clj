(ns app.server
  (:require [app.handlers :as h])
  (:import [Microsoft.AspNetCore.Builder WebApplication EndpointRouteBuilderExtensions]
           [Microsoft.Extensions.Hosting HostingAbstractionsHostExtensions]))

(defn- async-handler
  [handler-fn]
  (gen-delegate |System.Func`1[System.String]| []
   (str (.Result (handler-fn)))))

(defn- sync-handler
  [handler-fn]
  (gen-delegate |System.Func`1[System.String]| []
   (str (handler-fn))))

(defn start! [args]
  (let [builder (WebApplication/CreateBuilder (into-array String args))
        app (.Build builder)]
    (EndpointRouteBuilderExtensions/MapGet app "/" (sync-handler h/index-html))
    (EndpointRouteBuilderExtensions/MapGet app "/dog" (async-handler h/fetch-dog-fact))
    (EndpointRouteBuilderExtensions/MapGet app "/cat" (async-handler h/fetch-cat-fact))
    (EndpointRouteBuilderExtensions/MapGet app "/both" (async-handler h/fetch-both))
    (EndpointRouteBuilderExtensions/MapGet app "/status" (sync-handler h/status))
    (HostingAbstractionsHostExtensions/Run app)))
