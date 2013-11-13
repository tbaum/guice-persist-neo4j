package com.google.inject.extensions.neo4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListResult<E extends Serializable> implements Serializable {

    private final List<E> list;
    private final int total;

    public ListResult(Collection<E> list, int total) {
        this.list = new ArrayList<>(list);
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public List<E> getEntries() {
        return list;
    }
}
