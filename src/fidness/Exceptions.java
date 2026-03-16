package fidness;

public class Exceptions {

    public static class AuthException extends Exception {
        public AuthException(String message) { super(message); }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }
}