using AetherStream.Dashboard.Models;

namespace AetherStream.Dashboard.Services;

/// <summary>Shared in-memory dashboard state updated by REST bootstrap and WebSocket push.</summary>
public sealed class DashboardState
{
    private readonly Lock _lock = new();

    public IReadOnlyList<EnergyStateDto> EnergyStates { get; private set; } = [];
    public IReadOnlyList<AlertDto> Alerts { get; private set; } = [];
    public IReadOnlyList<TurbineDto> Turbines { get; private set; } = [];
    public bool GatewayReachable { get; private set; }
    public bool RealtimeConnected { get; private set; }
    public string? LastError { get; private set; }

    public event Action? Changed;

    public void SetEnergyStates(IEnumerable<EnergyStateDto> states)
    {
        lock (_lock)
        {
            EnergyStates = states.ToList();
        }
        Notify();
    }

    public void UpsertEnergyState(EnergyStateDto state)
    {
        lock (_lock)
        {
            var list = EnergyStates.ToList();
            var index = list.FindIndex(s => s.Region == state.Region);
            if (index >= 0)
            {
                list[index] = state;
            }
            else
            {
                list.Add(state);
            }
            EnergyStates = list;
        }
        Notify();
    }

    public void SetAlerts(IEnumerable<AlertDto> alerts)
    {
        lock (_lock)
        {
            Alerts = alerts.ToList();
        }
        Notify();
    }

    public void PrependAlert(AlertDto alert)
    {
        lock (_lock)
        {
            var list = Alerts.ToList();
            if (list.All(a => a.Id != alert.Id))
            {
                list.Insert(0, alert);
            }
            Alerts = list;
        }
        Notify();
    }

    public void SetTurbines(IEnumerable<TurbineDto> turbines)
    {
        lock (_lock)
        {
            Turbines = turbines.ToList();
        }
        Notify();
    }

    public void SetGatewayReachable(bool reachable, string? error = null)
    {
        lock (_lock)
        {
            GatewayReachable = reachable;
            LastError = error;
        }
        Notify();
    }

    public void SetRealtimeConnected(bool connected)
    {
        lock (_lock)
        {
            RealtimeConnected = connected;
        }
        Notify();
    }

    private void Notify() => Changed?.Invoke();
}
