using AetherStream.Dashboard.Components;
using AetherStream.Dashboard.Services;
using Azure.Extensions.AspNetCore.DataProtection.Blobs;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.HttpOverrides;
using Radzen;

var builder = WebApplication.CreateBuilder(args);

var dataProtectionKeysPath = Path.Combine(builder.Environment.ContentRootPath, "dataprotection-keys");
Directory.CreateDirectory(dataProtectionKeysPath);
var dataProtection = builder.Services.AddDataProtection()
    .PersistKeysToFileSystem(new DirectoryInfo(dataProtectionKeysPath));

var blobConnectionString = builder.Configuration["DataProtection:BlobConnectionString"];
var blobContainerName = builder.Configuration["DataProtection:BlobContainerName"];
var blobBlobName = builder.Configuration["DataProtection:BlobName"] ?? "keys.xml";
if (!string.IsNullOrWhiteSpace(blobConnectionString) && !string.IsNullOrWhiteSpace(blobContainerName))
{
    dataProtection.PersistKeysToAzureBlobStorage(blobConnectionString, blobContainerName, blobBlobName);
}

if (string.Equals(builder.Configuration["ASPNETCORE_FORWARDEDHEADERS_ENABLED"], "true", StringComparison.OrdinalIgnoreCase))
{
    builder.Services.Configure<ForwardedHeadersOptions>(options =>
    {
        options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto | ForwardedHeaders.XForwardedHost;
        options.KnownIPNetworks.Clear();
        options.KnownProxies.Clear();
    });
}

builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();
builder.Services.AddRadzenComponents();
builder.Services.AddHealthChecks();

builder.Services.AddSingleton<DashboardState>();
builder.Services.AddSingleton<DashboardDisplay>();
builder.Services.AddSingleton<GatewayApiClient>();
builder.Services.AddHostedService<DashboardBootstrapService>();
builder.Services.AddHostedService<RealtimeConnectionService>();

builder.Services.AddHttpClient("Gateway", client =>
{
    var baseUrl = builder.Configuration["Gateway:BaseUrl"] ?? "http://localhost:8085";
    client.BaseAddress = new Uri(baseUrl);
});

var app = builder.Build();

if (string.Equals(builder.Configuration["ASPNETCORE_FORWARDEDHEADERS_ENABLED"], "true", StringComparison.OrdinalIgnoreCase))
{
    app.UseForwardedHeaders();
}

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
}
app.UseStatusCodePagesWithReExecute("/not-found", createScopeForStatusCodePages: true);
app.UseAntiforgery();

app.MapHealthChecks("/health");
app.MapStaticAssets();
app.MapRazorComponents<App>()
    .AddInteractiveServerRenderMode();

app.Run();
