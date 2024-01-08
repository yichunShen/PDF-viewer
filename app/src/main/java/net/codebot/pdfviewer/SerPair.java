package net.codebot.pdfviewer;

import java.io.Serializable;

public class SerPair<T1, T2> implements Serializable {
    T1 first;
    T2 second;
    public SerPair(T1 f, T2 s){
        first = f;
        second = s;
    }
}
