# Sensitive Data Exposure

## Description
Sensitive data (API keys, passwords, PII, tokens) must never be exposed in logs, error messages, or code.

## Prevention
1. **No Hardcoded Secrets**: Never hardcode keys or passwords. Use environment variables or a secrets manager.
2. **Log Sanitization**: Ensure logs do not contain sensitive data.
3. **Generic Error Messages**: Do not return stack traces or internal details to the client.

### Risky Patterns
- Hardcoded keys:
  ```java
  String apiKey = "AIzaSy..."; // VULNERABLE!
  ```
- Print Stack Trace:
  ```java
  e.printStackTrace(); // AVOID in production
  ```
