package risk

import "fmt"

type DecisionPolicy struct {
	approveMaxScore int
	reviewMaxScore  int
}

func NewDecisionPolicy(approveMaxScore int, reviewMaxScore int) (DecisionPolicy, error) {
	if approveMaxScore < 0 {
		return DecisionPolicy{}, fmt.Errorf("approve max score must be >= 0")
	}

	if reviewMaxScore < 0 {
		return DecisionPolicy{}, fmt.Errorf("review max score must be >= 0")
	}

	if approveMaxScore >= reviewMaxScore {
		return DecisionPolicy{}, fmt.Errorf("approve max score must be less than review max score")
	}

	return DecisionPolicy{
		approveMaxScore: approveMaxScore,
		reviewMaxScore:  reviewMaxScore,
	}, nil
}

func (p DecisionPolicy) Decide(score int) Decision {
	if score <= p.approveMaxScore {
		return DecisionApproved
	}

	if score <= p.reviewMaxScore {
		return DecisionReviewRequired
	}

	return DecisionDeclined
}
