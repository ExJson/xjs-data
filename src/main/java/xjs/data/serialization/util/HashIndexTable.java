package xjs.data.serialization.util;

import java.util.Arrays;
import java.util.List;

public class HashIndexTable {
    protected final byte[] indices = new byte[32];

    public void init(final List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            this.add(values.get(i), i);
        }
    }

    public void add(final Object key, final int index) {
        int slot = getSlot(key);
        if (index < 0xff) {
            // increment by 1, 0 stands for empty
            this.indices[slot] = (byte) (index + 1);
        } else {
            this.indices[slot] = 0;
        }
    }

    public void remove(final int index) {
        for (int i = 0; i < this.indices.length; i++) {
            final int current = this.indices[i] & 0xff;
            if (current == index + 1) {
                this.indices[i] = 0;
            } else if (current > index + 1) {
                this.indices[i]--;
            }
        }
    }

    public int get(final Object key) {
        // subtract 1, 0 stands for empty
        return (this.indices[this.getSlot(key)] & 0xff) - 1;
    }

    private int getSlot(final Object key) {
        return key.hashCode() & this.indices.length - 1;
    }

    public void clear() {
        Arrays.fill(this.indices, (byte) 0);
    }
}