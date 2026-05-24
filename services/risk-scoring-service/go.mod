module github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service

go 1.26.3

require (
        github.com/k4vrin/PaymentRiskPlatform/proto/gen/go v0.0.0
	golang.org/x/net v0.51.0 // indirect
	golang.org/x/sys v0.42.0 // indirect
	golang.org/x/text v0.34.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20260226221140-a57be14db171 // indirect
	google.golang.org/grpc v1.81.1 // indirect
	google.golang.org/protobuf v1.36.11 // indirect
)

        replace github.com/k4vrin/PaymentRiskPlatform/proto/gen/go => ../../proto/gen/go
