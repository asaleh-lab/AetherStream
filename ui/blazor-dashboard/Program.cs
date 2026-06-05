using AetherStream.Dashboard.Components;
using AetherStream.Dashboard.Services;
using Microsoft.AspNetCore.DataProtection;
using Radzen;

var builder = WebApplication.CreateBuilder(args);

var dataProtectionKeysPath = Path.Combine(builder.Environment.ContentRootPath, "dataprotection-keys");
Directory.CreateDirectory(dataProtectionKeysPath);
builder.Services.AddDataProtection()
    .PersistKeysToFileSystem(new DirectoryInfo(dataProtectionKeysPath));

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
