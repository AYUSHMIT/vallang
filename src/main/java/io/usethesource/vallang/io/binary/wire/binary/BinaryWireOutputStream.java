/** 
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.io.binary.wire.binary;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import io.usethesource.vallang.io.binary.util.ByteBufferOutputStream;
import io.usethesource.vallang.io.binary.util.TaggedInt;
import io.usethesource.vallang.io.binary.util.TrackLastWritten;
import io.usethesource.vallang.io.binary.util.WindowCacheFactory;
import io.usethesource.vallang.io.binary.wire.FieldKind;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;


public class BinaryWireOutputStream implements IWireOutputStream {

    private static final byte[] WIRE_VERSION = new byte[] { 1, 0, 0 };
    private boolean closed = false;
    private final OutputStream __stream;
    private final TrackLastWritten<String> stringsWritten;

    public BinaryWireOutputStream(OutputStream stream, int stringSharingWindowSize) throws IOException {
        this(stream, stringSharingWindowSize, 8*1024);
    }
    public BinaryWireOutputStream(OutputStream stream, int stringSharingWindowSize, int bufferSize) throws IOException {
        if (stream instanceof BufferedOutputStream || stream instanceof ByteBufferOutputStream) {
            __stream = stream;
        }
        else {
            __stream = new BufferedOutputStream(stream, bufferSize);
        }
        __stream.write(WIRE_VERSION);
        encodeInteger(__stream, stringSharingWindowSize);
        this.stringsWritten = WindowCacheFactory.getInstance().getTrackLastWrittenObjectEquality(stringSharingWindowSize);
    }
    

    @Override
    public void flush() throws IOException {
        __stream.flush();
    }

    private void writeBytes(byte[] bytes) throws IOException {
        __stream.write(bytes);
    }

    private static void encodeInteger(OutputStream stream, int value) throws IOException {
        // unrolling this loop made it slower
        while((value & ~0x7F) != 0) {
            stream.write((byte)((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        stream.write((byte)value);
    }

    /*
     * LEB128 encoding (or actually LEB32) of positive and negative integers, negative integers always take 5 bytes, positive integers are compact.
     */
    private void encodeInteger(int value) throws IOException {
        encodeInteger(__stream, value);
    }

    /*
     * Strings are UTF8 encoded byte arrays prefixed with their length
     */
    private void encodeString(String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        encodeInteger(bytes.length);
        writeBytes(bytes);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                __stream.close();
            }
            finally {
                closed = true;
                WindowCacheFactory.getInstance().returnTrackLastWrittenObjectEquality(stringsWritten);
            }
        }
    }
    
    private void assertNotClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream already closed"); 
        }
    }

    private void writeFieldTag(final int fieldId, final int type) throws IOException {
        encodeInteger(TaggedInt.make(fieldId, type));
    }

    @Override
    public void startMessage(int messageId) throws IOException {
        assertNotClosed();
        writeFieldTag(messageId, 0);
    }

    @Override
    public void writeField(int fieldId, String value) throws IOException {
        assertNotClosed();
        int alreadyWritten = stringsWritten.howLongAgo(value);
        if (alreadyWritten != -1) {
            writeFieldTag(fieldId, FieldKind.PREVIOUS_STR);
            encodeInteger(TaggedInt.make(alreadyWritten, FieldKind.STRING));
        }
        else {
            writeFieldTag(fieldId, FieldKind.STRING);
            encodeString(value);
            stringsWritten.write(value);
        }
    }
    
    @Override
    public void writeField(int fieldId, int value) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.INT);
        encodeInteger(value);
    }
    
    @Override
    public void writeField(int fieldId, byte[] value) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.REPEATED);
        int size = value.length;
        if (size < TaggedInt.MAX_ORIGINAL_VALUE) {
            encodeInteger(TaggedInt.make(size, FieldKind.Repeated.BYTES));
        }
        else {
            encodeInteger(TaggedInt.make(TaggedInt.MAX_ORIGINAL_VALUE, FieldKind.Repeated.BYTES));
            encodeInteger(size);
        }
        writeBytes(value);
    }
    
    @Override
    public void writeField(int fieldId, int[] values) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.REPEATED);
        int size = values.length;
        if (size < TaggedInt.MAX_ORIGINAL_VALUE) {
            encodeInteger(TaggedInt.make(size, FieldKind.Repeated.INTS));
        }
        else {
            encodeInteger(TaggedInt.make(TaggedInt.MAX_ORIGINAL_VALUE, FieldKind.Repeated.INTS));
            encodeInteger(size);
        }
        for (int v : values) {
            encodeInteger(v);
        }
    }
    
    @Override
    public void writeField(int fieldId, String[] values) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.REPEATED);
        int size = values.length;
        if (size < TaggedInt.MAX_ORIGINAL_VALUE) {
            encodeInteger(TaggedInt.make(size, FieldKind.Repeated.STRINGS));
        }
        else {
            encodeInteger(TaggedInt.make(TaggedInt.MAX_ORIGINAL_VALUE, FieldKind.Repeated.STRINGS));
            encodeInteger(size);
        }
        for (String s : values) {
            writeNestedString(s);
        }
    }

    private void writeNestedString(String s) throws IOException {
        int alreadyWritten = stringsWritten.howLongAgo(s);
        if (alreadyWritten != -1) {
            encodeInteger(TaggedInt.make(alreadyWritten, FieldKind.PREVIOUS_STR));
        }
        else {
            encodeInteger(TaggedInt.make(0, FieldKind.STRING));
            encodeString(s);
            stringsWritten.write(s);
        }
    }
    
    @Override
    public void writeNestedField(int fieldId) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.NESTED);
        
    }
    @Override
    public void writeRepeatedNestedField(int fieldId, int numberOfNestedElements) throws IOException {
        assertNotClosed();
        writeFieldTag(fieldId, FieldKind.REPEATED);
        if (numberOfNestedElements <= TaggedInt.MAX_ORIGINAL_VALUE) {
            encodeInteger(TaggedInt.make(numberOfNestedElements, FieldKind.Repeated.NESTEDS));
        }
        else {
            encodeInteger(TaggedInt.make(TaggedInt.MAX_ORIGINAL_VALUE, FieldKind.Repeated.NESTEDS));
            encodeInteger(numberOfNestedElements);
        }
    }
    @Override
    public void endMessage() throws IOException {
        assertNotClosed();
        writeFieldTag(0, 0);
    }
}
