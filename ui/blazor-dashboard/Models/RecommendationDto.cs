namespace AetherStream.Dashboard.Models;

public sealed record RecommendationDto(
    string Id,
    string Region,
    string Suggestion,
    DateTimeOffset Timestamp);
