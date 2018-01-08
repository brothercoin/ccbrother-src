package com.hykj.ccbrother.utils;

public class PHPBase64 {
    final static String encodingChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/ "; /**
     * Returns the base 64 encoded equivalent of a supplied string.
     * @param source the string to encode
     */
    public static String encode( String source ) {
        char[] sourceBytes = getPaddedBytes( source );
        int numGroups = (sourceBytes.length + 2) / 3;
        char[] targetBytes = new char[4];
        char[] target = new char[ 4 * numGroups ];

        for (int group = 0; group < numGroups; group++) {
            convert3To4( sourceBytes, group*3, targetBytes );
            for (int i = 0; i < targetBytes.length; i++) {
                target[ i + 4*group ] = encodingChar.charAt( targetBytes[i] );
            }
        }

        int numPadBytes = sourceBytes.length - source.length();

        for (int i = target.length-numPadBytes; i < target.length; i++) target[i] = '=';
        return new String( target );
    }


    private static char[] getPaddedBytes( String source ) {
        char[] converted = source.toCharArray();
        int requiredLength = 3 * ((converted.length+2) /3);
        char[] result = new char[ requiredLength ];
        System.arraycopy( converted, 0, result, 0, converted.length );
        return result;
    }


    private static void convert3To4( char[] source, int sourceIndex, char[] target ) {
        target[0] = (char) ( source[ sourceIndex ] >>> 2);
        target[1] = (char) (((source[ sourceIndex ] & 0x03) << 4) | (source[ sourceIndex+1 ] >>> 4));
        target[2] = (char) (((source[ sourceIndex+1 ] & 0x0f) << 2) | (source[ sourceIndex+2 ] >>> 6));
        target[3] = (char) ( source[ sourceIndex+2 ] & 0x3f);
    }


    /**
     * Returns the plaintext equivalent of a base 64-encoded string.
     * @param source a base 64 string (which must have a multiple of 4 characters)
     */
    public static String decode( String source ) {
        if (source.length()%4 != 0) throw new RuntimeException( "valid Base64 codes have a multiple of 4 characters " );
        int numGroups = source.length() / 4;
        int numExtraBytes = source.endsWith( "== " ) ? 2 : (source.endsWith( "= " ) ? 1 : 0);
        byte[] targetBytes = new byte[ 3*numGroups ];
        byte[] sourceBytes = new byte[4];
        for (int group = 0; group < numGroups; group++) {
            for (int i = 0; i < sourceBytes.length; i++) {
                sourceBytes[i] = (byte) Math.max( 0, encodingChar.indexOf( source.charAt( 4*group+i ) ) );
            }
            convert4To3( sourceBytes, targetBytes, group*3 );
        }
        return new String( targetBytes, 0, targetBytes.length - numExtraBytes );
    }


    private static void convert4To3( byte[] source, byte[] target, int targetIndex ) {
        target[ targetIndex ] = (byte) (( source[0] << 2) | (source[1] >>> 4));
        target[ targetIndex+1 ] = (byte) (((source[1] & 0x0f) << 4) | (source[2] >>> 2));
        target[ targetIndex+2 ] = (byte) (((source[2] & 0x03) << 6) | (source[3]));
    }

}