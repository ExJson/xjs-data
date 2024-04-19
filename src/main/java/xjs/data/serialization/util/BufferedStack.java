package xjs.data.serialization.util;

/**
 * A highly-optimized utility for neatly storing data
 * in recursive rw operations.
 */
public abstract class BufferedStack {
    protected Object[] stack = new Object[10];
    protected int index;

    public static <T1, T2> OfTwo<T1, T2> ofTwo() {
        return new OfTwo<>();
    }

    public boolean isEmpty() {
        return this.index == 0;
    }

    protected void grow() {
        final Object[] newStack = new Object[this.stack.length + 10];
        System.arraycopy(this.stack, 0, newStack, 0, this.index);
        this.stack = newStack;
    }

    public static class OfTwo<T1, T2> extends BufferedStack {
        public void push(final T1 t1, final T2 t2) {
            if (this.index >= this.stack.length) {
                this.grow();
            }
            this.stack[this.index] = t1;
            this.stack[this.index + 1] = t2;
            this.index += 2;
        }

        public void pop() {
            this.index -= 2;
        }

        @SuppressWarnings("unchecked")
        public T1 getFirst() {
            return (T1) this.stack[this.index];
        }

        @SuppressWarnings("unchecked")
        public T2 getSecond() {
            return (T2) this.stack[this.index + 1];
        }

        public int getIndex() {
            return this.index != 0 ? this.index / 2 : 0;
        }
    }
}
