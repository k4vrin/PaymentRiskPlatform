package risk

import "testing"

func TestRepeatedDeviceRuleNormalDevice(t *testing.T) {
	t.Parallel()

	rule := NewRepeatedDeviceRule()

	hits := rule.Evaluate(ScoringRequest{
		DeviceFingerprint: "device_123",
	})

	if len(hits) != 0 {
		t.Fatalf("expected no rule hits, got %d", len(hits))
	}
}

func TestRepeatedDeviceRuleRepeatedDevicePlaceholder(t *testing.T) {
	t.Parallel()

	rule := NewRepeatedDeviceRule()

	hits := rule.Evaluate(ScoringRequest{
		DeviceFingerprint: "repeat_device_123",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}

	hit := hits[0]

	if hit.RuleID != RepeatedDeviceRuleID {
		t.Fatalf("expected rule id %s, got %s", RepeatedDeviceRuleID, hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeRepeatedDevice {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeRepeatedDevice, hit.ReasonCode)
	}

	if hit.ScoreDelta != RepeatedDeviceScoreDelta {
		t.Fatalf("expected score delta %d, got %d", RepeatedDeviceScoreDelta, hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected rule hit message to be present")
	}
}

func TestRepeatedDeviceRuleIsCaseInsensitiveAndTrimsWhitespace(t *testing.T) {
	t.Parallel()

	rule := NewRepeatedDeviceRule()

	hits := rule.Evaluate(ScoringRequest{
		DeviceFingerprint: "  REPEAT_device_123  ",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}
}
