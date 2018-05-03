package one.helfy;

/**
 * @author serkan
 */
public final class HelfyNative {

    static {
        System.loadLibrary("helfy");
    }

    public static native Object getObject(long addressLocation);

}
