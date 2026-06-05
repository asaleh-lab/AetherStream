namespace AetherStream.Dashboard.Models;

public sealed record TurbineDto(
    string TurbineId,
    double Rpm,
    double PowerOutput,
    double VibrationLevel,
    DateTimeOffset Timestamp);
