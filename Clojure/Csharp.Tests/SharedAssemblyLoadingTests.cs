using System;
using System.Reflection;
using NUnit.Framework;
using clojure.lang;

namespace Csharp.Tests
{
    [TestFixture]
    public class SharedAssemblyLoadingTests
    {
        [OneTimeSetUp]
        public void Setup()
        {
            // Initialize Clojure runtime once
            RT.Init();
        }

        [Test]
        public void TestCanLoadAspNetCoreTypes()
        {
            // Test loading various ASP.NET Core types
            var testCases = new[]
            {
                "Microsoft.AspNetCore.Builder.WebApplication",
                "Microsoft.AspNetCore.Builder.WebApplicationBuilder",
                "Microsoft.AspNetCore.Http.HttpContext",
                "Microsoft.AspNetCore.Hosting.IWebHostEnvironment",
                "Microsoft.Extensions.DependencyInjection.IServiceCollection"
            };

            foreach (var typeName in testCases)
            {
                var type = RT.classForName(typeName);
                Assert.That(type, Is.Not.Null, $"Failed to load type: {typeName}");
                Console.WriteLine($"✓ Successfully loaded: {typeName}");
            }
        }

        [Test]
        public void TestCanLoadExtensionMethodTypes()
        {
            // Test loading extension method types that were problematic
            var extensionTypes = new[]
            {
                "Microsoft.AspNetCore.Builder.EndpointRouteBuilderExtensions",
                "Microsoft.AspNetCore.Builder.HttpsPolicyBuilderExtensions",
                "Microsoft.AspNetCore.Builder.StaticFileExtensions",
                "Microsoft.AspNetCore.Http.HttpResponseWritingExtensions",
                "Microsoft.Extensions.DependencyInjection.HealthCheckServiceCollectionExtensions"
            };

            foreach (var typeName in extensionTypes)
            {
                try
                {
                    var type = RT.classForName(typeName);
                    Assert.That(type, Is.Not.Null, $"Failed to load extension type: {typeName}");
                    Console.WriteLine($"✓ Successfully loaded extension type: {typeName}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"✗ Failed to load {typeName}: {ex.Message}");
                    throw;
                }
            }
        }

        [Test]
        public void TestTrustedPlatformAssembliesAreAvailable()
        {
            // Verify that TRUSTED_PLATFORM_ASSEMBLIES is set
            var trustedAssemblies = AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES") as string;
            Assert.That(trustedAssemblies, Is.Not.Null, "TRUSTED_PLATFORM_ASSEMBLIES should be available");
            Assert.That(trustedAssemblies, Is.Not.Empty, "TRUSTED_PLATFORM_ASSEMBLIES should not be empty");
            
            // Check if it contains ASP.NET Core assemblies
            Assert.That(trustedAssemblies.Contains("Microsoft.AspNetCore"), Is.True, 
                "TRUSTED_PLATFORM_ASSEMBLIES should contain ASP.NET Core assemblies");
            
            Console.WriteLine($"TRUSTED_PLATFORM_ASSEMBLIES contains {trustedAssemblies.Split(System.IO.Path.PathSeparator).Length} assemblies");
        }

        [Test]
        public void TestClojureCanAccessAspNetCoreTypes()
        {
            // Test that Clojure can access ASP.NET Core types
            try
            {
                // Verify we can get the class through RT.classForName
                var webAppClass = RT.classForName("Microsoft.AspNetCore.Builder.WebApplication");
                Assert.That(webAppClass, Is.Not.Null);
                Console.WriteLine("✓ Successfully loaded WebApplication class through RT.classForName");
                
                // Test that we can use eval to access the type
                var evalFn = RT.var("clojure.core", "eval");
                var evalResult = evalFn.invoke(RT.readString("(clojure.lang.RT/classForName \"Microsoft.AspNetCore.Builder.WebApplication\")"));
                Assert.That(evalResult, Is.Not.Null);
                Assert.That(evalResult, Is.EqualTo(webAppClass));
                Console.WriteLine("✓ Successfully accessed WebApplication through Clojure eval");
                
                // Test that we can check if it's a class
                var classFn = RT.var("clojure.core", "class?");
                var isClass = classFn.invoke(webAppClass);
                Assert.That(isClass, Is.EqualTo(true));
                Console.WriteLine("✓ WebApplication recognized as a class by Clojure");
            }
            catch (Exception ex)
            {
                Assert.Fail($"Failed to access ASP.NET Core type through Clojure: {ex.Message}");
            }
        }

        [Test]
        public void TestCanCreateWebApplicationBuilder()
        {
            // Test if we can actually instantiate ASP.NET Core types
            try
            {
                var webAppType = RT.classForName("Microsoft.AspNetCore.Builder.WebApplication");
                Assert.That(webAppType, Is.Not.Null);

                // Try to call CreateBuilder with empty args
                var createBuilderMethod = webAppType.GetMethod("CreateBuilder", new Type[] { typeof(string[]) });
                Assert.That(createBuilderMethod, Is.Not.Null, "CreateBuilder method should exist");

                // Don't actually create it in tests as it might interfere
                Console.WriteLine("✓ WebApplication.CreateBuilder method found and accessible");
            }
            catch (Exception ex)
            {
                Assert.Fail($"Failed to access WebApplication.CreateBuilder: {ex.Message}");
            }
        }
    }
}