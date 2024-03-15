/**
 * Thrown when there is an attempt to modify a SuperPath that has already been
 * marked as complete.
 */
public class UnmodifiablePathException extends RuntimeException {
	public UnmodifiablePathException() {}

	public UnmodifiablePathException(String message) {
		super(message);
	}

	public UnmodifiablePathException(Throwable cause) {
		super(cause);
	}

	public UnmodifiablePathException(String message, Throwable cause) {
		super(message, cause);
	}
}
