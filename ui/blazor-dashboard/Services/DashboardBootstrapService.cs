namespace AetherStream.Dashboard.Services;

/// <summary>Loads initial REST snapshot on startup and periodically refreshes turbine state.</summary>
public sealed class DashboardBootstrapService(
    GatewayApiClient gatewayApiClient,
    DashboardState dashboardState,
    ILogger<DashboardBootstrapService> logger) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await RefreshAsync(stoppingToken);

        using var timer = new PeriodicTimer(TimeSpan.FromSeconds(30));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            await RefreshAsync(stoppingToken);
        }
    }

    private async Task RefreshAsync(CancellationToken cancellationToken)
    {
        try
        {
            await gatewayApiClient.BootstrapAsync(dashboardState, cancellationToken);
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "Dashboard refresh failed");
        }
    }
}
