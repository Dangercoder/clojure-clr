// Pre-load framework assemblies so Clojure's type resolver can find them
System.Runtime.CompilerServices.RuntimeHelpers.RunClassConstructor(
    typeof(Microsoft.AspNetCore.Builder.WebApplication).TypeHandle);
System.Runtime.CompilerServices.RuntimeHelpers.RunClassConstructor(
    typeof(Microsoft.AspNetCore.Http.Results).TypeHandle);
System.Runtime.CompilerServices.RuntimeHelpers.RunClassConstructor(
    typeof(Microsoft.Extensions.Hosting.HostingAbstractionsHostExtensions).TypeHandle);
System.Runtime.CompilerServices.RuntimeHelpers.RunClassConstructor(
    typeof(Dapper.SqlMapper).TypeHandle);
System.Runtime.CompilerServices.RuntimeHelpers.RunClassConstructor(
    typeof(Microsoft.Data.Sqlite.SqliteConnection).TypeHandle);

clojure.lang.RT.Init();
clojure.lang.RT.var("clojure.core", "require").invoke(clojure.lang.Symbol.intern("app.server"));
clojure.lang.RT.var("app.server", "start!").invoke(args);
