# ClojureCLR + .NET 11 Minimal API

All handler logic in Clojure. C# is only the 27-line `Program.cs` bootstrap.

## Run

```bash
dotnet run --project examples/dotnet-minimal-api
# visit http://localhost:5000
```

Requires .NET 11 Preview SDK.

## Project structure

```
Program.cs          <- C# bootstrap: init Clojure, wire routes (27 lines)
app/handlers.clj    <- all handler logic: async HTTP, concurrency, etc.
MinimalApi.csproj
```

## The Clojure handlers (`app/handlers.clj`)

```clojure
(ns app.handlers)

(def ^:private http-client (System.Net.Http.HttpClient.))

(def fetch-dog-fact
  (^:async fn* []
    (await* (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1"))))

(def fetch-both
  (^:async fn* []
    (let [dog-task (.GetStringAsync http-client "https://dogapi.dog/api/v2/facts?limit=1")
          cat-task (.GetStringAsync http-client "https://catfact.ninja/fact")
          tasks    (into-array System.Threading.Tasks.Task [dog-task cat-task])
          _        (await* (System.Threading.Tasks.Task/WhenAll tasks))]
      (str "{\"dog\":" (.Result dog-task) ",\"cat\":" (.Result cat-task) "}"))))
```

## The C# bootstrap (`Program.cs`)

```csharp
RT.Init();
RT.var("clojure.core", "require").invoke(Symbol.intern("app.handlers"));

var fetchDogFact = RT.var("app.handlers", "fetch-dog-fact");
// ...

app.MapGet("/dog", () => Json(fetchDogFact));
app.MapGet("/both", () => Json(fetchBoth));
```

## What it demonstrates

- `^:async fn*` with `await*` calling real `HttpClient.GetStringAsync`
- Concurrent HTTP via `Task.WhenAll` from Clojure
- C# `await`ing `Task<object>` returned by Clojure fns
- .NET 11 runtime async — no state machines, no boilerplate
