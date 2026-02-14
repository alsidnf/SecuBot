# SQL Injection Prevention

## Description
SQL Injection (SQLi) is a web security vulnerability that allows an attacker to interfere with the queries that an application makes to its database.

## Prevention
1. **Use Prepared Statements (Parameterized Queries)**: This is the most effective defense.
   ```java
   String query = "SELECT * FROM users WHERE username = ?";
   PreparedStatement pstmt = connection.prepareStatement(query);
   pstmt.setString(1, username);
   ResultSet results = pstmt.executeQuery();
   ```

2. **Input Validation**: Validate all user inputs against a strict allowlist.

3. **Least Privilege**: Ensure the database user has only the minimum necessary privileges.

## Risky Patterns to Avoid
- String concatenation in SQL queries:
  ```java
  String query = "SELECT * FROM users WHERE username = '" + username + "'"; // VULNERABLE!
  ```
