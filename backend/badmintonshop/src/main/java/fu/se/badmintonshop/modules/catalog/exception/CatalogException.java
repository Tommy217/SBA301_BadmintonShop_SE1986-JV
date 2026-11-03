package fu.se.badmintonshop.modules.catalog.exception;

public class CatalogException extends RuntimeException {
    public final int status;
    public CatalogException(int status, String message) { super(message); this.status = status; }
}
