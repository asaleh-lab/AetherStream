using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using AetherStream.Dashboard.Models;

namespace AetherStream.Dashboard.Services;

/// <summary>Maintains a server-side WebSocket to the api-gateway and updates dashboard state.</summary>
public sealed class RealtimeConnectionService(
    IConfiguration configuration,
    DashboardState dashboardState,
    ILogger<RealtimeConnectionService> logger) : BackgroundService
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var webSocketUrl = configuration["Gateway:WebSocketUrl"] ?? "ws://localhost:8085/ws/realtime";

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                using var socket = new ClientWebSocket();
                await socket.ConnectAsync(new Uri(webSocketUrl), stoppingToken);
                dashboardState.SetRealtimeConnected(true);
                logger.LogInformation("Connected to realtime gateway at {Url}", webSocketUrl);

                await ReceiveLoopAsync(socket, stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                logger.LogWarning(ex, "Realtime WebSocket disconnected; reconnecting in 3s");
                dashboardState.SetRealtimeConnected(false);
                await Task.Delay(TimeSpan.FromSeconds(3), stoppingToken);
            }
        }

        dashboardState.SetRealtimeConnected(false);
    }

    private async Task ReceiveLoopAsync(ClientWebSocket socket, CancellationToken cancellationToken)
    {
        var buffer = new byte[8192];
        var messageBuilder = new StringBuilder();

        while (socket.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
        {
            messageBuilder.Clear();
            WebSocketReceiveResult result;

            do
            {
                result = await socket.ReceiveAsync(buffer, cancellationToken);
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "closing", cancellationToken);
                    return;
                }

                messageBuilder.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
            }
            while (!result.EndOfMessage);

            HandleMessage(messageBuilder.ToString());
        }
    }

    private void HandleMessage(string json)
    {
        try
        {
            using var document = JsonDocument.Parse(json);
            var root = document.RootElement;
            var type = root.GetProperty("type").GetString();
            var payload = root.GetProperty("payload");

            switch (type)
            {
                case "energy-state":
                    var energy = payload.Deserialize<EnergyStateDto>(JsonOptions);
                    if (energy is not null)
                    {
                        dashboardState.UpsertEnergyState(energy);
                    }
                    break;
                case "alert":
                    var alert = payload.Deserialize<AlertDto>(JsonOptions);
                    if (alert is not null)
                    {
                        dashboardState.PrependAlert(alert);
                    }
                    break;
                default:
                    logger.LogDebug("Ignoring unknown realtime message type {Type}", type);
                    break;
            }
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "Failed to parse realtime message");
        }
    }
}
