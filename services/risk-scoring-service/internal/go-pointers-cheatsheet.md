# Go Pointers Cheat Sheet

## Mental Model

```text
& = "Where is it?"
* = "What's there?"
```

---

# Address Operator (&)

Gets the memory address of a value.

```go
x := 10

ptr := &x
```

```text
ptr
 │
 ▼
 x = 10
```

Read as:

> ptr points to x

---

# Pointer Type (\*Type)

Declares a pointer.

```go
var ptr *int
```

Read as:

> ptr is a pointer to an int

Examples:

```go
var logger *slog.Logger
var server *grpc.Server
var user *User
```

---

# Dereference (\*value)

Follow the pointer and get the value.

```go
x := 10
ptr := &x

fmt.Println(*ptr)
```

Output:

```text
10
```

Read as:

> Follow ptr and give me the value.

---

# Modify Through Pointer

```go
x := 10

ptr := &x

*ptr = 20

fmt.Println(x)
```

Output:

```text
20
```

The value stored at x was modified through the pointer.

---

# Struct Values vs Pointers

## Value

```go
server := RiskScoringServer{}
```

Creates a struct value.

Passing it around copies the struct.

---

## Pointer

```go
server := &RiskScoringServer{}
```

Creates a pointer.

Passing it around shares the same object.

---

# Constructor Pattern

Most services, repositories, and servers return pointers.

```go
func NewRiskScoringServer() *RiskScoringServer {
    return &RiskScoringServer{}
}
```

Read as:

> Create a server and return a pointer to it.

---

# Method Receivers

## Value Receiver

```go
func (s RiskScoringServer) DoSomething() {
}
```

Behavior:

```text
Struct is copied before method call
```

Use for:

- Small immutable structs
- Value objects

---

## Pointer Receiver

```go
func (s *RiskScoringServer) DoSomething() {
}
```

Behavior:

```text
Method uses the original object
No struct copy
Can modify fields
```

Use for:

- Services
- Repositories
- gRPC servers
- Database clients
- Large structs

---

# Why Do We Usually Use Pointer Receivers?

Suppose:

```go
type Server struct {
    logger *slog.Logger
}
```

Value receiver:

```go
func (s Server) Start() {}
```

Go copies the struct before calling Start.

Pointer receiver:

```go
func (s *Server) Start() {}
```

Go uses the existing object.

More efficient and idiomatic.

---

# Common Backend Types

Usually pointers:

```go
*sql.DB
*grpc.Server
*slog.Logger
*http.Client
*RiskScoringServer
*PaymentRepository
*PaymentService
```

---

# Small Value Types

Usually values:

```go
Money
Currency
PaymentID
RuleHit
ScoringRequest
ScoringResult
```

Because they are small data containers.

---

# Java/Kotlin Comparison

Java:

```java
User user = new User();
```

Closest Go equivalent:

```go
user := &User{}
```

Java objects are references.

Go structs are values unless you use pointers.

---

# Quick Memory Aid

```text
&value
    Give me the address

*Type
    Pointer to Type

*pointer
    Follow the pointer

& creates pointers
* uses pointers
```

---

# Visual Example

```go
x := 10
ptr := &x
```

```text
ptr
 │
 ▼
+-------+
|  10   |
+-------+
    ^
    |
    x
```

```go
*ptr = 20
```

```text
ptr
 │
 ▼
+-------+
|  20   |
+-------+
    ^
    |
    x
```

Both ptr and x refer to the same underlying value.

---

# Rule of Thumb

```text
DTOs, Commands, Results
    -> values

Services, Repositories, Servers
    -> pointers

If in doubt for a long-lived backend component
    -> use a pointer receiver
```
