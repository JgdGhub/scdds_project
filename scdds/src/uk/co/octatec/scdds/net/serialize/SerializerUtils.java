package uk.co.octatec.scdds.net.serialize;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public class SerializerUtils {

    public static void writeIntToBytes(int n, byte [] b4) {
        writeIntToBytes(n, b4, 0);
    }

    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_DOUBLE = 8;
    public static final int SIZE_OF_FLOAT = 4;
    public static final int SIZE_OF_SHORT = 2;

    public static final int SIZE_OF_DATE = SIZE_OF_LONG;

    public static final int SIZE_OF_STRING(String s)  {
        return s==null ? SIZE_OF_SHORT: s.length()+SIZE_OF_SHORT ;
    }

    public static final int SIZE_OF_SHORT_STRING(String s)  {
        return s==null  ? 1 : s.length()+1 ;
    }

    public static final int SIZE_OF_LONG_STRING(String s)  {
        return s==null  ? SIZE_OF_INT : s.length()+SIZE_OF_INT ;
    }

    public static  void writeDateToBytes(Date d, byte[] b, int offset) {
        if( d == null ) {
            writeLongToBytes(-1, b, offset);
        }
        else {
            writeLongToBytes(d.getTime(), b, offset);
        }
    }

    public static  Date readDateFromBytes(byte[] b, int offset) {
        long l = readLongFromBytes(b, offset);
        if( l == -1 ){
            return null;
        }
        else {
            return new Date(l);
        }
    }

    public static int writeStringToBytes(String s, byte[] b, int offset) {
        if( s == null ) {
            writeShortToBytes((short)-1, b, offset);
            return SIZE_OF_SHORT;
        }
        short n = (short) s.length();
        writeShortToBytes(n, b, offset);
        byte[] str = s.getBytes() ;
        System.arraycopy(str, 0, b, offset+SIZE_OF_SHORT, n);
        return n+SIZE_OF_SHORT;
    }

    public static String readStringFromBytes(byte[] b, int offset) {
        short n = readShortFromBytes(b, offset);
        if( n == -1) {
            return null;
        }
        String s = new String(b, offset+SIZE_OF_SHORT, n);
        return s;
    }

    public static int writeShortStringToBytes(String s, byte[] b, int offset) {
        if( s == null ) {
            b[offset] = -1;
            return 1;
        }
        byte n = (byte) s.length();
        b[offset] = n;
        byte[] str = s.getBytes() ;
        System.arraycopy(str, 0, b, offset+1, n);
        return n+1;
    }

    public static String readShortStringFromBytes(byte[] b, int offset) {
        short n = b[offset];
        if( n == -1) {
            return null;
        }
        String s = new String(b, offset+1, n);
        return s;
    }

    public static int writeLongStringToBytes(String s, byte[] b, int offset) {
        if( s == null ) {
            writeIntToBytes(-1, b, offset);
            return SIZE_OF_INT;
        }
        int n = s.length();
        writeIntToBytes(n, b, offset);
        byte[] str = s.getBytes() ;
        System.arraycopy(str, 0, b, offset+SIZE_OF_INT, n);
        return n+SIZE_OF_INT;
    }

    public static String readLongStringFromBytes(byte[] b, int offset) {
        int n = readIntFromBytes(b, offset);
        if( n == -1) {
            return null;
        }
        String s = new String(b, offset+SIZE_OF_INT, n);
        return s;
    }

    public static void writeIntToBytes(int n, byte [] b4, int offset) {
        int byte0 = n & 0xff;
        int byte1 = (n>>8) & 0xff;
        int byte2 = (n>>16) & 0xff;
        int byte3 = (n>>24) & 0xff;

        b4[3+offset] = (byte)byte0;
        b4[2+offset] = (byte)byte1;
        b4[1+offset] = (byte)byte2;
        b4[0+offset] = (byte)byte3;
    }

    public static void writeIntToByteBuffer(int n, ByteBuffer b4, int offset) {
        int byte0 = n & 0xff;
        int byte1 = (n>>8) & 0xff;
        int byte2 = (n>>16) & 0xff;
        int byte3 = (n>>24) & 0xff;

        b4.put(3+offset, (byte)byte0);
        b4.put(2+offset, (byte)byte1);
        b4.put(1+offset, (byte)byte2);
        b4.put(0+offset, (byte)byte3);
    }

    public static int readIntFromBytes(byte [] b4)	{
        return readIntFromBytes(b4, 0);
    }

    public static int readIntFromBytes(byte [] b4, int offset)	{
        int byte0 = b4[3+offset]& 0xff;
        int byte1 = b4[2+offset]& 0xff;
        int byte2 = b4[1+offset]& 0xff;
        int byte3 = b4[0+offset]& 0xff;

        return   (int)  ( (byte3<<24) | (byte2<<16) | (byte1<<8) | byte0 );
    }

    public static long readLongFromBytes(byte [] b8){
        return readLongFromBytes(b8, 0);
    }

    public static long readLongFromBytes(byte [] b8, int offset){

        long byte0 = b8[7+offset]& 0xff;
        long byte1 = b8[6+offset]& 0xff;
        long byte2 = b8[5+offset]& 0xff;
        long byte3 = b8[4+offset]& 0xff;
        long byte4 = b8[3+offset]& 0xff;
        long byte5 = b8[2+offset]& 0xff;
        long byte6 = b8[1+offset]& 0xff;
        long byte7 = b8[0+offset]& 0xff;

        return   (long)  ( (byte7<<56)  | (byte6<<48) | (byte5<<40) | (byte4<<32) | (byte3<<24) | (byte2<<16) | (byte1<<8) | byte0 );
    }

    public static void writeLongToBytes(long n, byte [] b8) {
        writeLongToBytes(n, b8, 0);
    }

    public static void writeLongToBytes(long n, byte [] b8, int offset) {
        long byte0 = n & 0xff;
        long byte1 = (n>>8) & 0xff;
        long byte2 = (n>>16) & 0xff;
        long byte3 = (n>>24) & 0xff;
        long byte4 = (n>>32) & 0xff;
        long byte5 = (n>>40) & 0xff;
        long byte6 = (n>>48) & 0xff;
        long byte7 = (n>>56) & 0xff;

        b8[7+offset] = (byte)byte0;
        b8[6+offset] = (byte)byte1;
        b8[5+offset] = (byte)byte2;
        b8[4+offset] = (byte)byte3;
        b8[3+offset] = (byte)byte4;
        b8[2+offset] = (byte)byte5;
        b8[1+offset] = (byte)byte6;
        b8[0+offset] = (byte)byte7;
    }

    public static short readShortFromBytes(byte [] b2, int offset) {

        int byte1 = b2[0+offset]& 0xff;
        int byte0 = b2[1+offset]& 0xff;
        return (short) ((byte1<<8) | byte0);
    }

    public static void writeShortToBytes(short i, byte [] b2, int offset) {
        b2[0] = 0;
        b2[1] = 0;
        int byte1 = i & 0xff;
        int byte0 = (i>>8) & 0xff;
        b2[0+offset] = (byte)byte0;
        b2[1+offset] = (byte)byte1;
    }

    public static char readCharFromBytes(byte [] b2, int offset) {

        int byte1 = b2[0+offset]& 0xff;
        int byte0 = b2[1+offset]& 0xff;
        return (char) ((byte1<<8) | byte0);
    }

    public static void writeCharToBytes(char c, byte [] b2, int offset) {
        b2[0] = 0;
        b2[1] = 0;
        int byte1 = c & 0xff;
        int byte0 = (c>>8) & 0xff;
        b2[0+offset] = (byte)byte0;
        b2[1+offset] = (byte)byte1;
    }

    public static double readDoubleFromBytes(byte [] b8, int offset) {

        long byte0 = b8[7+offset]& 0xff;
        long byte1 = b8[6+offset]& 0xff;
        long byte2 = b8[5+offset]& 0xff;
        long byte3 = b8[4+offset]& 0xff;
        long byte4 = b8[3+offset]& 0xff;
        long byte5 = b8[2+offset]& 0xff;
        long byte6 = b8[1+offset]& 0xff;
        long byte7 = b8[0+offset]& 0xff;

        long l =   (long)  ( (byte7<<56) | (byte6<<48) | (byte5<<40) | (byte4<<32) | (byte3<<24) | (byte2<<16) | (byte1<<8) | byte0 );

        return  Double.longBitsToDouble(l);
    }

    public static void writeDoubleToBytes(double d, byte [] b8, int offset) {

        long l = Double.doubleToLongBits(d);

        long byte7 = l & 0xff;
        long byte6 = (l>>8) & 0xff;
        long byte5 = (l>>16) & 0xff;
        long byte4 = (l>>24) & 0xff;
        long byte3 = (l>>32) & 0xff;
        long byte2 = (l>>40) & 0xff;
        long byte1 = (l>>48) & 0xff;
        long byte0 = (l>>56) & 0xff;

        b8[0+offset] = (byte)byte0;
        b8[1+offset] = (byte)byte1;
        b8[2+offset] = (byte)byte2;
        b8[3+offset] = (byte)byte3;
        b8[4+offset] = (byte)byte4;
        b8[5+offset] = (byte)byte5;
        b8[6+offset] = (byte)byte6;
        b8[7+offset] = (byte)byte7;
    }

    public static float readFloatFromBytes(byte [] b4, int offset) {

        int byte0 = b4[3+offset]& 0xff;
        int byte1 = b4[2+offset]& 0xff;
        int byte2 = b4[1+offset]& 0xff;
        int byte3 = b4[0+offset]& 0xff;

        int i =   (int)  ( (byte3<<24) | (byte2<<16) | (byte1<<8) | byte0 );

        return  Float.intBitsToFloat(i);
    }

    public static void writeFloatToBytes(float f, byte [] b4, int offset) {

        int i = Float.floatToIntBits(f);

        int byte3 = i & 0xff;
        int byte2 = (i>>8) & 0xff;
        int byte1 = (i>>16) & 0xff;
        int byte0 = (i>>24) & 0xff;

        b4[0+offset] = (byte)byte0;
        b4[1+offset] = (byte)byte1;
        b4[2+offset] = (byte)byte2;
        b4[3+offset] = (byte)byte3;
    }

}
