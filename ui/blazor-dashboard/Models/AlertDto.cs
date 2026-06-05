namespace AetherStream.Dashboard.Models;

public sealed record AlertDto(
    string Id,
    string Type,
    string Severity,
    string Source,
    string Message,
    DateTimeOffset Timestamp);
