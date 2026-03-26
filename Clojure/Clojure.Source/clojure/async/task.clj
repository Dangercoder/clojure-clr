(ns clojure.async.task
  "Task-based async/await interop for .NET 11+ runtime async.
   Requires .NET 11 Preview 1 or later."
  (:refer-clojure :exclude [await]))

(defmacro await
  "Suspends the current ^:async function until the given Task completes.
   Returns the task's result value. Only valid inside a ^:async defn
   or an (async ...) block.

   Usage:
     (t/await (.ReadAllTextAsync System.IO.File path))
     (t/await some-task)"
  [task-expr]
  `(await* ~task-expr))

(defmacro async
  "Executes body in an async context, returning a Task<object>.
   The body may use (t/await ...) to suspend on Tasks.

   Usage:
     (t/async
       (let [data (t/await (fetch url))]
         (process data)))"
  [& body]
  `((^:async fn* [] ~@body)))

(defmacro await-all
  "Awaits multiple tasks in parallel. Returns a vector of results.

   Usage:
     (let [[a b c] (t/await-all (task1) (task2) (task3))]
       ...)"
  [& task-exprs]
  (let [tasks-sym (gensym "tasks")]
    `(let [~tasks-sym (into-array System.Threading.Tasks.Task [~@task-exprs])]
       (await* (System.Threading.Tasks.Task/WhenAll ~tasks-sym))
       (mapv (fn [t#] (.Result t#)) ~tasks-sym))))

(defn task?
  "Returns true if x is a Task."
  [x]
  (instance? System.Threading.Tasks.Task x))
