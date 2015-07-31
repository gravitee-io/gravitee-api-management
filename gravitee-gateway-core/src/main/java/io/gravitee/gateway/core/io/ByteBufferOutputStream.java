/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by david on 30/07/15.
 */
public class ByteBufferOutputStream extends OutputStream{
    private ByteBuffer buffer;
    private boolean onHeap;
    private float increasing = DEFAULT_INCREASING_FACTOR;

    public static final float DEFAULT_INCREASING_FACTOR = 1.5f;

    public ByteBufferOutputStream(int size) {
        this(size, DEFAULT_INCREASING_FACTOR, false);
    }

    public ByteBufferOutputStream(int size, boolean onHeap) {
        this(size, DEFAULT_INCREASING_FACTOR, onHeap);
    }

    public ByteBufferOutputStream(int size, float increasingBy) {
        this(size, increasingBy, false);
    }

    public ByteBufferOutputStream(int size, float increasingBy, boolean onHeap) {
        if(increasingBy <= 1){
            throw new IllegalArgumentException("Increasing Factor must be greater than 1.0");
        }
        if(onHeap){
            buffer = ByteBuffer.allocate(size);
        }else{
            buffer = ByteBuffer.allocateDirect(size);
        }
        this.onHeap = onHeap;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int position = buffer.position();
        int limit = buffer.limit();

        long newTotal = position + len;
        if(newTotal > limit){
            int capacity = (int) (buffer.capacity()*increasing);
            while(capacity <= newTotal){
                capacity = (int) (capacity*increasing);
            }

            increase(capacity);
        }

        buffer.put(b, 0, len);
    }

    @Override
    public void write(int b) throws IOException {
        if(!buffer.hasRemaining()){
            increase((int) (buffer.capacity()*increasing));
        }
        buffer.put((byte)b);
    }

    protected void increase(int newCapacity){
        buffer.limit(buffer.position());
        buffer.rewind();

        ByteBuffer newBuffer;
        if(onHeap){
            newBuffer = ByteBuffer.allocate(newCapacity);
        }else{
            newBuffer = ByteBuffer.allocateDirect(newCapacity);
        }

        newBuffer.put(buffer);
        buffer.clear();
        buffer = newBuffer;
    }

    public long size(){
        return buffer.position();
    }

    public long capacity(){
        return buffer.capacity();
    }

    public ByteBuffer buffer(){
        return buffer;
    }
}