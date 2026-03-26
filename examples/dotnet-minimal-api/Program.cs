using clojure.lang;
using System.Threading.Tasks;

RT.Init();
RT.var("clojure.core", "require").invoke(Symbol.intern("app.handlers"));

var app = WebApplication.CreateBuilder(args).Build();

// All handler logic lives in app/handlers.clj — this is just plumbing.
IFn V(string name) => (IFn)RT.var("app.handlers", name).deref();

app.MapGet("/",      () => Results.Content((string)V("index-html").invoke(), "text/html"));
app.MapGet("/dog",   async () => (string)await (Task<object>)V("fetch-dog-fact").invoke());
app.MapGet("/cat",   async () => (string)await (Task<object>)V("fetch-cat-fact").invoke());
app.MapGet("/both",  async () => (string)await (Task<object>)V("fetch-both").invoke());
app.MapGet("/status",() => (string)V("status").invoke());

app.Run();
