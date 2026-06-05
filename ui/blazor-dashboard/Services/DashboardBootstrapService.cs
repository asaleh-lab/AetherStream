namespace AetherStream.Dashboard.Services;

/// <summary>Loads one REST snapshot on startup; live updates come from the WebSocket feed.</summary>
public sealed class DashboardBootstrapService(
    GatewayApiClient gatewayApiClient,
    DashboardState dashboardState,
    ILogger<DashboardBootstrapService> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            await gatewayApiClient.BootstrapAsync(dashboardState, stoppingToken);
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "Dashboard bootstrap failed");
        }
    }
}
