package risk

import "testing"

func TestDecisionPolicyDecide(t *testing.T) {
	t.Parallel()

	policy, err := NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	tests := []struct {
		name     string
		score    int
		expected Decision
	}{
		{
			name:     "zero score approved",
			score:    0,
			expected: DecisionApproved,
		},
		{
			name:     "approve threshold approved",
			score:    49,
			expected: DecisionApproved,
		},
		{
			name:     "one above approve threshold review required",
			score:    50,
			expected: DecisionReviewRequired,
		},
		{
			name:     "review threshold review required",
			score:    79,
			expected: DecisionReviewRequired,
		},
		{
			name:     "one above review threshold declined",
			score:    80,
			expected: DecisionDeclined,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			decision := policy.Decide(tt.score)

			if decision != tt.expected {
				t.Fatalf("expected %s, got %s", tt.expected, decision)
			}
		})
	}
}

func TestNewDecisionPolicyRejectsInvalidThresholds(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name            string
		approveMaxScore int
		reviewMaxScore  int
	}{
		{
			name:            "negative approve max score",
			approveMaxScore: -1,
			reviewMaxScore:  79,
		},
		{
			name:            "negative review max score",
			approveMaxScore: 49,
			reviewMaxScore:  -1,
		},
		{
			name:            "approve equal to review",
			approveMaxScore: 79,
			reviewMaxScore:  79,
		},
		{
			name:            "approve greater than review",
			approveMaxScore: 90,
			reviewMaxScore:  79,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewDecisionPolicy(tt.approveMaxScore, tt.reviewMaxScore)

			if err == nil {
				t.Fatal("expected error, got nil")
			}
		})
	}
}
