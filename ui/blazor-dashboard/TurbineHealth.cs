using Radzen;

namespace AetherStream.Dashboard;

public static class TurbineHealth
{
    private const double VibrationWarningThreshold = 0.7;
    private const double VibrationCriticalThreshold = 1.0;

    public static string Evaluate(double vibrationLevel) =>
        vibrationLevel >= VibrationCriticalThreshold ? "Critical"
        : vibrationLevel >= VibrationWarningThreshold ? "Warning"
        : "Healthy";

    public static BadgeStyle BadgeStyleFor(double vibrationLevel) =>
        vibrationLevel >= VibrationCriticalThreshold ? BadgeStyle.Danger
        : vibrationLevel >= VibrationWarningThreshold ? BadgeStyle.Warning
        : BadgeStyle.Success;
}
