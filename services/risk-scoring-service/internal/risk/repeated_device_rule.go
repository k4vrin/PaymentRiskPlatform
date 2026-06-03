package risk

import "strings"

const (
	RepeatedDeviceRuleID = "REPEATED_DEVICE_RULE"

	// Phase 4 deterministic placeholder.
	// A real repeated-device rule would need historical device usage from storage.
	RepeatedDeviceFingerprintPrefix = "repeat_"

	RepeatedDeviceScoreDelta = 20
)

type RepeatedDeviceRule struct{}

func NewRepeatedDeviceRule() RepeatedDeviceRule {
	return RepeatedDeviceRule{}
}

func (r RepeatedDeviceRule) Evaluate(request ScoringRequest) []RuleHit {
	deviceFingerprint := strings.ToLower(strings.TrimSpace(request.DeviceFingerprint))

	if !strings.HasPrefix(deviceFingerprint, RepeatedDeviceFingerprintPrefix) {
		return []RuleHit{}
	}

	return []RuleHit{
		NewRuleHit(
			RepeatedDeviceRuleID,
			ReasonCodeRepeatedDevice,
			RepeatedDeviceScoreDelta,
			"device fingerprint matched repeated-device placeholder heuristic",
		),
	}
}
