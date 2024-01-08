package net.codebot.pdfviewer;

import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

public class SerPath implements Serializable {
    ArrayList<SerPair<SerPair<Float, Float>, Integer>> points;
    public SerPath(){
        points = new ArrayList<SerPair<SerPair<Float, Float>, Integer>>();
    }
}
