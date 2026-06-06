using System.Net;
using AetherStream.Dashboard.Models;

namespace AetherStream.Dashboard.Services;

public sealed class GatewayApiClient(IHttpClientFactory httpClientFactory, ILogger<GatewayApiClient> logger)
{
    private static readonly string[] KnownTurbineIds = ["T-001", "T-002", "T-003"];

    public async Task BootstrapAsync(DashboardState state, CancellationToken cancellationToken = default)
    {
        try
        {
            var client = httpClientFactory.CreateClient("Gateway");

            var energyTask = client.GetFromJsonAsync<List<EnergyStateDto>>("/api/energy/latest", cancellationToken);
            var alertsTask = client.GetFromJsonAsync<List<AlertDto>>("/api/alerts?limit=50", cancellationToken);
            var recommendationsTask =
                client.GetFromJsonAsync<List<RecommendationDto>>("/api/recommendations?limit=50", cancellationToken);
            var turbineTasks = KnownTurbineIds
                .Select(id => FetchTurbineAsync(client, id, cancellationToken))
                .ToArray();

            await Task.WhenAll(energyTask, alertsTask, recommendationsTask, Task.WhenAll(turbineTasks));

            state.SetEnergyStates(energyTask.Result ?? []);
            state.SetAlerts(alertsTask.Result ?? []);
            state.SetRecommendations(recommendationsTask.Result ?? []);

            var turbines = (await Task.WhenAll(turbineTasks))
                .Where(t => t is not null)
                .Cast<TurbineDto>()
                .ToList();
            state.SetTurbines(turbines);
            state.SetGatewayReachable(true);
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "Failed to bootstrap dashboard from gateway");
            state.SetGatewayReachable(false, ex.Message);
        }
    }

    private static async Task<TurbineDto?> FetchTurbineAsync(
        HttpClient client,
        string turbineId,
        CancellationToken cancellationToken)
    {
        using var response = await client.GetAsync($"/api/turbines/{turbineId}", cancellationToken);
        if (response.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }

        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<TurbineDto>(cancellationToken);
    }
}
