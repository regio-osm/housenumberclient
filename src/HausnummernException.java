public class HausnummernException extends Exception  // oder RuntimeException
{
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */

	public HausnummernException()                                  { super(); }
	public HausnummernException( String message )                  { super( message ); }
	public HausnummernException( Throwable cause )                 { super( cause ); }
	public HausnummernException( String message, Throwable cause ) { super( message, cause ); }
}
