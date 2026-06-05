using Radzen;

namespace AetherStream.Dashboard;

public static class AlertBadgeStyle
{
    public static BadgeStyle ForSeverity(string severity) =>
        severity.ToUpperInvariant() switch
        {
            "CRITICAL" => BadgeStyle.Danger,
            "WARNING" => BadgeStyle.Warning,
            _ => BadgeStyle.Info
        };
}
