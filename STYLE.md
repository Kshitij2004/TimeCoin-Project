# P_05 Style Guide
Unless otherwise specified below, we follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
# Java
## Formatting
- Use 4 spaces for indentation (**NO** tabs).
- Use K&R braces (opening brace on the same line):
```java
for (int i = 0; i < n; i++) {
    System.out.println("Hello!");
}
```
- Always use braces for `if/for/while` statements (even single-line bodies):
```java
if (x) { counter++; } // WRONG.

if (x) {
    counter++; // CORRECT.
}
```
- Line limit is 100 characters when reasonable (wrap long strings/args cleanly):
```java
public class IndentationDemo {
    // 100 Character limit guide (Visual Marker) ------------------------------------------------->|
    
    public void example() {
        // Wrapping Long Strings: Break and indent by 8 spaces (continuation indent)
        String userAlert = "The system has encountered a critical synchronization error while "
                + "attempting to contact the remote database server.";

        // Wrapping Arguments: Break after the comma for clarity
        boolean success = dataProcessor.executeTransaction("User_01", 1024, "RETRY_ON_FAIL", 
                "A descriptive log message that would otherwise push this line over the limit");
    }
}
```
## Naming
- Classes, Interfaces, and Enums: `PascalCase`
- Methods, fields, local variables: `camelCase`
- Constants (`static final`): `UPPER_SNAKE_CASE`
- Packages: `lowercase` (no underscores)
## Imports
- No wildcard imports (`import x.*`).
- Imports should be organized: `java.* / javax.*`, then third-party, then project packages.
- Remove unused imports.
## Comments & JavaDoc

## Tests

---

v0
