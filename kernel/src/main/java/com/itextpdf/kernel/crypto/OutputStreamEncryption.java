package com.itextpdf.kernel.crypto;

import com.itextpdf.kernel.PdfException;
import java.io.IOException;

public class OutputStreamEncryption extends java.io.OutputStream {

    protected java.io.OutputStream out;
    protected ARCFOUREncryption arcfour;
    protected AESCipher cipher;
    private byte[] sb = new byte[1];
    private static final int AES_128 = 4;
    private static final int AES_256 = 5;
    private boolean aes;
    private boolean finished;

    /**
     * Creates a new instance of OutputStreamCounter
     */
    public OutputStreamEncryption(java.io.OutputStream out, byte key[], int off, int len, int revision) {
        this.out = out;
        aes = (revision == AES_128 || revision == AES_256);
        if (aes) {
            byte[] iv = IVGenerator.getIV();
            byte[] nkey = new byte[len];
            System.arraycopy(key, off, nkey, 0, len);
            cipher = new AESCipher(true, nkey, iv);
            try {
                write(iv);
            } catch (IOException e) {
                throw new PdfException(PdfException.PdfEncryption, e);
            }
        } else {
            arcfour = new ARCFOUREncryption();
            arcfour.prepareARCFOURKey(key, off, len);
        }
    }

    public OutputStreamEncryption(java.io.OutputStream out, byte key[], int revision) {
        this(out, key, 0, key.length, revision);
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of {@code close}
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     * <p/>
     * The {@code close} method of {@code OutputStream} does nothing.
     *
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        finish();
        out.close();
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of {@code flush} is
     * that calling it is an indication that, if any bytes previously
     * written have been buffered by the implementation of the output
     * stream, such bytes should immediately be written to their
     * intended destination.
     * <p/>
     * The {@code flush} method of {@code OutputStream} does nothing.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Writes {@code b.length} bytes from the specified byte array
     * to this output stream. The general contract for {@code write(b)}
     * is that it should have exactly the same effect as the call
     * {@code write(b, 0, b.length)}.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs.
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for {@code write} is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument {@code b}. The 24
     * high-order bits of {@code b} are ignored.
     * <p/>
     * Subclasses of {@code OutputStream} must provide an
     * implementation for this method.
     *
     * @param b the {@code byte}.
     * @throws IOException if an I/O error occurs. In particular, an {@code IOException} may be thrown if the
     *                     output stream has been closed.
     */
    public void write(int b) throws IOException {
        sb[0] = (byte) b;
        write(sb, 0, 1);
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this output stream.
     * The general contract for {@code write(b, off, len)} is that
     * some of the bytes in the array {@code b} are written to the
     * output stream in order; element {@code b[off]} is the first
     * byte written and {@code b[off+len-1]} is the last byte written
     * by this operation.
     * <p/>
     * The {@code write} method of {@code OutputStream} calls
     * the write method of one argument on each of the bytes to be
     * written out. Subclasses are encouraged to override this method and
     * provide a more efficient implementation.
     * <p/>
     * If {@code b} is {@code null}, a
     * {@code NullPointerException} is thrown.
     * <p/>
     * If {@code off} is negative, or {@code len} is negative, or
     * {@code off+len} is greater than the length of the array
     * {@code b}, then an <tt>IndexOutOfBoundsException</tt> is thrown.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs. In particular,
     *                     an {@code IOException} is thrown if the output
     *                     stream is closed.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        if (aes) {
            byte[] b2 = cipher.update(b, off, len);
            if (b2 == null || b2.length == 0)
                return;
            out.write(b2, 0, b2.length);
        } else {
            byte[] b2 = new byte[Math.min(len, 4192)];
            while (len > 0) {
                int sz = Math.min(len, b2.length);
                arcfour.encryptARCFOUR(b, off, sz, b2, 0);
                out.write(b2, 0, sz);
                len -= sz;
                off += sz;
            }
        }
    }

    public void finish() {
        if (!finished) {
            finished = true;
            if (aes) {
                byte[] b = cipher.doFinal();
                try {
                    out.write(b, 0, b.length);
                } catch (IOException e) {
                    throw new PdfException(PdfException.PdfEncryption, e);
                }
            }
        }
    }
}