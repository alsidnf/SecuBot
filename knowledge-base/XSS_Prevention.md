# Cross-Site Scripting (XSS) Prevention

## Description
XSS attacks occur when an application includes untrusted data in a web page without proper validation or escaping.

## Prevention
1. **Output Encoding**: Escape user input before rendering it in the browser.
   - Context-aware encoding (HTML, JavaScript, CSS, URL).
2. **Input Validation**: Validate input against a strict allowlist.
3. **Content Security Policy (CSP)**: Implement CSP headers to restrict sources of executable scripts.

## Java/Spring Best Practices
- Use a template engine that auto-escapes (e.g., Thymeleaf).
- Be careful with `@ResponseBody` returning raw strings containing user input.

### Risky Patterns
- Reflecting user input directly:
  ```java
  return "Hello " + request.getParameter("name"); // VULNERABLE!
  ```
