namespace AetherStream.Dashboard.Services;

/// <summary>Loads one REST snapshot on startup; live updates come from the WebSocket feed.</summary>
public sealed class DashboardBootstrapService(
    GatewayApiClient gatewayApiClient,
    DashboardState dashboardState,
    ILogger<DashboardBootstrapService> logger) : BackgroundService
{
    private static readonly TimeSpan RetryDelay = TimeSpan.FromSeconds(3);

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            if (await gatewayApiClient.BootstrapAsync(dashboardState, stoppingToken))
            {
                logger.LogInformation("Dashboard bootstrap completed");
                return;
            }

            logger.LogWarning("Dashboard bootstrap failed; retrying in {DelaySeconds}s", RetryDelay.TotalSeconds);
            await Task.Delay(RetryDelay, stoppingToken);
        }
    }
}
