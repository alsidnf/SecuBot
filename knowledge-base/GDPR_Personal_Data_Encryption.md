# GDPR Personal Data Encryption & Protection

## Description
Under GDPR (General Data Protection Regulation), personal data must be processed securely. A key measure is encryption and pseudonymization.

## Requirements
1. **Encrypt Data at Rest**: All Personally Identifiable Information (PII) stored in databases must be encrypted.
   - Examples of PII: Name, Email, Phone Number, SSN, IP Address, etc.
2. **Encrypt Data in Transit**: Use TLS 1.2+ for all data transmission.
3. **Pseudonymization**: Where possible, replace identifying fields with artificial identifiers.

## Implementation Guidelines (Java)
- Do NOT store PII in plain text.
- Use strong encryption algorithms (e.g., AES-256).

### Risky Patterns to Avoid
- Logging PII:
  ```java
  log.info("User registered: " + user.getEmail()); // VULNERABLE!
  ```
- Storing passwords in plain text or using weak hashing (MD5, SHA-1). Use BCrypt or Argon2.
