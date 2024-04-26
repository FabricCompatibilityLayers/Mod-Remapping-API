package fr.catcore.modremapperapi.utils;

import java.util.ArrayList;

@Deprecated
public class BArrayList<T> extends ArrayList<T> {

    public BArrayList<T> put(T entry) {
        this.add(entry);
        return this;
    }
}
