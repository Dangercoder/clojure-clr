using clojure.lang;
using System.Threading.Tasks;

RT.Init();
RT.var("clojure.core", "require").invoke(Symbol.intern("app.handlers"));

// Grab Clojure handler vars
var fetchDogFact = RT.var("app.handlers", "fetch-dog-fact");
var fetchCatFact = RT.var("app.handlers", "fetch-cat-fact");
var fetchBoth    = RT.var("app.handlers", "fetch-both");
var status       = RT.var("app.handlers", "status");
var indexHtml    = RT.var("app.handlers", "index-html");

// Thin async wrapper: invoke Clojure ^:async fn, await the Task<object>
async Task<IResult> Json(IFn handler)
{
    var result = (Task<object>)handler.invoke();
    return Results.Content((string)await result, "application/json");
}

var app = WebApplication.CreateBuilder(args).Build();

app.MapGet("/",      () => Results.Content((string)indexHtml.invoke(), "text/html"));
app.MapGet("/dog",   () => Json(fetchDogFact));
app.MapGet("/cat",   () => Json(fetchCatFact));
app.MapGet("/both",  () => Json(fetchBoth));
app.MapGet("/status",() => Results.Content((string)status.invoke(), "application/json"));

app.Run();
