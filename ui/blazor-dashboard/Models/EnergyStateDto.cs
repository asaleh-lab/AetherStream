namespace AetherStream.Dashboard.Models;

public sealed record EnergyStateDto(
    string Region,
    double TotalWindPower,
    double GridDemand,
    double EfficiencyScore,
    DateTimeOffset Timestamp);
