namespace AetherStream.Dashboard.Services;

/// <summary>Formats dashboard values for display (timezone-aware timestamps, efficiency).</summary>
public sealed class DashboardDisplay(IConfiguration configuration)
{
    private readonly TimeZoneInfo? _displayTimeZone = ResolveTimeZone(configuration["Dashboard:DisplayTimeZoneId"]);

    public string FormatTimestamp(DateTimeOffset timestamp)
    {
        if (_displayTimeZone is null)
        {
            return timestamp.ToUniversalTime().ToString("g") + " UTC";
        }

        return TimeZoneInfo.ConvertTime(timestamp, _displayTimeZone).ToString("g");
    }

    public static string FormatEfficiencyPercent(double efficiencyScore) =>
        (efficiencyScore * 100).ToString("0.#") + "%";

    public static int EfficiencyBarValue(double efficiencyScore) =>
        (int)Math.Clamp(Math.Round(efficiencyScore * 100, MidpointRounding.AwayFromZero), 0, 100);

    private static TimeZoneInfo? ResolveTimeZone(string? timeZoneId)
    {
        if (string.IsNullOrWhiteSpace(timeZoneId))
        {
            return null;
        }

        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById(timeZoneId);
        }
        catch (TimeZoneNotFoundException)
        {
            return null;
        }
        catch (InvalidTimeZoneException)
        {
            return null;
        }
    }
}
