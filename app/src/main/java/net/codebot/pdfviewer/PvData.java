package net.codebot.pdfviewer;

import android.graphics.Path;
import android.util.Pair;
import androidx.core.content.res.TypedArrayUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class PvData {
    ArrayList<ArrayList<Pair<Path, Integer>>> paths;
    ArrayList<ArrayList<SerPair<SerPath, Integer>>> serPaths;
    int curPageIndex = -1;
    File data;

    public PvData(MainActivity context, String fileName, int pageNum) {
        try{
            data = new File(context.getCacheDir(), "data_"+fileName+".pvdtuw_cs349_v5");
            if(data.createNewFile()){
                init(pageNum);
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> File Not found created "+context.getCacheDir()+" >"+curPageIndex);
            }else{
                loadData(pageNum, true);
//                System.out.println("================================= File found "+context.getCacheDir()+" >"+curPageIndex);
                if(curPageIndex == -1){
                    init(pageNum);
                }
            }
        }catch (IOException e){
            System.err.println("*io error new data");
        }
    }

    private void init(int pageNum){
        pathInit(pageNum);
        curPageIndex = 0;
//        System.out.println("............................ init done [s]"+serPaths);
//        System.out.println("[p]"+paths);
//        System.out.println("[c]"+curPageIndex);
        saveData();
    }

    void loadData(int pageNum, boolean updPth){
        try {
            FileInputStream fi = new FileInputStream(data);
            ObjectInputStream oi = new ObjectInputStream(fi);
            try {
//                paths = (ArrayList<ArrayList<Path>>) oi.readObject();
//                System.out.println("load start "+pageNum+" | "+updPth);
//                if(serPaths!=null){
//                    System.out.println("sss "+serPaths);
//                }
//                System.out.println("updpth start");
                if(updPth){
                    serPaths = (ArrayList<ArrayList<SerPair<SerPath, Integer>>>) oi.readObject();
//                    System.out.println("[x^x]"+serPaths);
                    paths = new ArrayList<>();
                    for(int i=0; i<serPaths.size(); i++){
                        paths.add(getPA(serPaths.get(i)));
                    }
//                if(updPth)pathInit(pageNum);
                }else{
                    oi.readObject();//skip reading paths
                }
//                System.out.println("endiffff");
//                if(serPaths!=null){
//                    System.out.println("sss "+serPaths);
//                }
//                System.out.println("[x^x]" + (String)oi.readObject());
                curPageIndex = (int) oi.readObject();
//                System.out.println("load done "+curPageIndex);
            }catch(EOFException ex) {} catch (Exception e) {
                System.err.println("*error loading paths");
                e.printStackTrace();
            }
            if(oi!=null)oi.close();
            if(fi!=null)fi.close();
        }catch(EOFException ex) {} catch(Exception e){
            System.err.println("*error loading file streams ");
        }
    }

    void saveData(){
        try{
            FileOutputStream fo = new FileOutputStream(data);
            ObjectOutputStream oo = new ObjectOutputStream(fo);
//            oo.writeObject(paths);
            oo.writeObject(serPaths);
            oo.writeObject("------x|x------");//separator
            oo.writeObject(curPageIndex);
//            System.out.println("********************************** save done "+curPageIndex);
            if(oo!=null)oo.close();
            if(fo!=null)fo.close();
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("*error saving data");
        }
    }

    void storePaths(int pageIndex, ArrayList<Pair<Path, Integer>> arr, ArrayList<SerPair<SerPath, Integer>> sar){
        paths.set(pageIndex, (ArrayList<Pair<Path, Integer>>) arr.clone());
        serPaths.set(pageIndex, (ArrayList<SerPair<SerPath, Integer>>) sar.clone());
    }

    ArrayList<Pair<Path, Integer>> getPathsCopy(int pageIndex){
        return (ArrayList<Pair<Path, Integer>>) paths.get(pageIndex).clone();
    }

    ArrayList<SerPair<SerPath, Integer>> getSerPathsCopy(int pageIndex){
        return (ArrayList<SerPair<SerPath, Integer>>) serPaths.get(pageIndex).clone();
    }

    void pathInit(int pageNum){
        serPaths = new ArrayList<>();
        paths = new ArrayList<>();
        pathResize(pageNum);
    }

    void pathResize(int pageNum){
        for(int i=0; i<pageNum - paths.size(); i++){
            paths.add(new ArrayList<Pair<Path, Integer>>());
            serPaths.add(new ArrayList<SerPair<SerPath, Integer>>());
        }
    }

    //convert path to serpath & vice versa 2 methods
    ArrayList<Pair<Path, Integer>> getPA(ArrayList<SerPair<SerPath, Integer>> serp){
        ArrayList<Pair<Path, Integer>> res = new ArrayList<Pair<Path, Integer>>();
        for(SerPair<SerPath, Integer> pair: serp){
            res.add(new Pair(sToP(pair.first), pair.second));
        }
        return res;
    }

    Path sToP(SerPath s){
        Path res = new Path();
        for(SerPair<SerPair<Float, Float>, Integer> pair: s.points){
            if(pair.second == 0){
                res.moveTo(pair.first.first, pair.first.second);
            }else if(pair.second == 1){
                res.lineTo(pair.first.first, pair.first.second);
            }
        }
        return res;
    }
}
