package minieiffel.util;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A simple stack implementation backed by a linked list.
 */
public class Stack <T> {
    
    private LinkedList<T> data = new LinkedList<T>();
    
    public T peek() {
        return data.getLast();
    }
    
    public T pop() {
        return data.removeLast();
    }
    
    public void push(T t) {
        data.addLast(t);
    }
    
    public ListIterator<T> iterateFromTop() {
        return data.listIterator(data.size());
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    public int size() {
        return data.size();
    }
    
    public String toString() {
        return data.toString();
    }
    
}
