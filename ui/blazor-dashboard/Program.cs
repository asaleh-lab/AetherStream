using AetherStream.Dashboard.Components;
using AetherStream.Dashboard.Services;
using Radzen;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();
builder.Services.AddRadzenComponents();
builder.Services.AddHealthChecks();

builder.Services.AddSingleton<DashboardState>();
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
