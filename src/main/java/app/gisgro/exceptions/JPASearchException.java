package app.gisgro.exceptions;

public class JPASearchException extends RuntimeException {
    public JPASearchException(String reason) {
        super(reason);
    }
}