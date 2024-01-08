package net.codebot.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provide this code.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;
    private int curIndex = -1;
    final int minWHei = 20;
    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;
    PvData fileData = null;
    ArrayDeque<Object> undo = new ArrayDeque<Object>();
    ArrayDeque<Integer> undoInd = new ArrayDeque<Integer>();
    ArrayDeque<Object> redo = new ArrayDeque<Object>();
    ArrayDeque<Integer> redoInd = new ArrayDeque<Integer>();

    Context appCont;
    Button slT, pnT, hiT, erT, preP, nexP, udBut, rdBut;
    TextView pgTxt;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appCont = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);

        TextView nameTxt = findViewById(R.id.fileName);
        nameTxt.setText(FILENAME);

        LinearLayout topBar = findViewById(R.id.TopBar);//top bar
        topBar.setEnabled(true);
        topBar.setMinimumHeight(minWHei);
        LinearLayout editSec = findViewById(R.id.EditSec);
        editSec.setEnabled(true);
        editSec.setMinimumHeight(minWHei);
        udBut = findViewById(R.id.undoButton);
        udBut.setMinimumHeight(minWHei);
        udBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undoAct();
            }
        });
        rdBut = findViewById(R.id.redoButton);
        rdBut.setMinimumHeight(minWHei);
        rdBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redoAct();
            }
        });
        setAbleButton(udBut, false);
        setAbleButton(rdBut, false);

        LinearLayout drTol = findViewById(R.id.DrawToolSec);
        drTol.setEnabled(true);
        drTol.setMinimumHeight(minWHei);
        slT = findViewById(R.id.selButton);
        slT.setMinimumHeight(minWHei);
        pnT = findViewById(R.id.penButton);
        pnT.setMinimumHeight(minWHei);
        hiT = findViewById(R.id.hilButton);
        hiT.setMinimumHeight(minWHei);
        erT = findViewById(R.id.ersButton);
        erT.setMinimumHeight(minWHei);
        slT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(slT);
            }
        });
        pnT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(pnT);
            }
        });
        hiT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(hiT);
            }
        });
        erT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(erT);
            }
        });
        selectTool(pnT);

        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);


        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            readData(FILENAME);

            showPage(curIndex);

            LinearLayout pgBar = findViewById(R.id.PageBar);
            pgBar.setEnabled(true);
            pgBar.setMinimumHeight(minWHei);
            preP = findViewById(R.id.prePage);
            preP.setMinimumHeight(minWHei);
            if(curIndex == 0){
                setAbleButton(preP, false);
            }
            pgTxt = findViewById(R.id.pageNum);
            pgTxt.setText(" "+(curIndex+1)+" / "+pdfRenderer.getPageCount()+" ");
            nexP = findViewById(R.id.nexPage);
            nexP.setMinimumHeight(minWHei);
            preP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    movePage(true);
                    undo.addLast("pre");
                    undoInd.addLast(0);
                    setAbleButton(udBut, !undo.isEmpty());
                }
            });
            nexP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    movePage(false);
                    undo.addLast("nex");
                    undoInd.addLast(1);
                    setAbleButton(udBut, !undo.isEmpty());
                }
            });

            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void movePage(boolean up){
        if(up){
            try {
                openRenderer(appCont);
                if ((curIndex - 1) >= 0) {
                    showPage(--curIndex);
                    pgTxt.setText(" "+(curIndex+1)+" / "+pdfRenderer.getPageCount()+" ");
                }
                setAbleButton(preP, !(curIndex == 0));
                setAbleButton(nexP, !(curIndex == (pdfRenderer.getPageCount() - 1)));
                closeRenderer();
            } catch (IOException e) {
                Log.d(LOGNAME, "Error opening PDF previous page");
            }
        }else{
            try {
                openRenderer(appCont);
                if (pdfRenderer.getPageCount() > (curIndex+1)) {
                    showPage(++curIndex);
                    pgTxt.setText(" "+(curIndex+1)+" / "+pdfRenderer.getPageCount()+" ");
                }
                setAbleButton(preP, !(curIndex == 0));
                setAbleButton(nexP, !(curIndex == (pdfRenderer.getPageCount() - 1)));
                closeRenderer();
            } catch (IOException e) {
                Log.d(LOGNAME, "Error opening PDF next page");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void undoAct(){
        int ind = undoInd.pollLast();
        if(ind == 0){
            movePage(false);
            undo.pollLast();
            redo.addLast("pre");
        }else if(ind == 1){
            movePage(true);
            undo.pollLast();
            redo.addLast("nex");
        }else if(ind > 1 && ind < 4){
            Object tmp = undo.pollLast();
            redo.addLast(tmp);
            pageImage.paths.remove(tmp);
        }
        redoInd.addLast(ind);
        setAbleButton(udBut, !undo.isEmpty());
        setAbleButton(rdBut, !redo.isEmpty());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void redoAct(){
        int ind = redoInd.pollLast();
        if(ind == 0){
            movePage(true);
            redo.pollLast();
            undo.addLast("pre");
        }else if(ind == 1){
            movePage(false);
            redo.pollLast();
            undo.addLast("nex");
        }else if(ind > 1 && ind < 4){
            Object tmp = redo.pollLast();
            pageImage.paths.add((Pair<Path, Integer>) tmp);
            undo.addLast(tmp);
        }
        undoInd.addLast(ind);
        setAbleButton(udBut, !undo.isEmpty());
        setAbleButton(rdBut, !redo.isEmpty());
    }

    int pnSt(){
        if(slT.isSelected()) return 0;
        if(pnT.isSelected()) return 1;
        if(hiT.isSelected()) return 2;
        return 3;
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeData(FILENAME);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume(){
        super.onResume();
        try{
            openRenderer(this);
            readData(FILENAME);
            closeRenderer();
        }catch (Exception e){
            System.out.println("*error reading data resuming");
        }
    }

    //read stored path information from data
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void readData(String fileName){
        if(fileData == null){
            fileData = new PvData(this, fileName, pdfRenderer.getPageCount());//load stored page draws
        }else{
            fileData.loadData(pdfRenderer.getPageCount(), false);
        }
        curIndex = fileData.curPageIndex;
//        System.out.println("----------------------------------- read data done "+curIndex);
    }

    //store path information to data
    private void storeData(String fileName){
        fileData.curPageIndex = curIndex;
        fileData.saveData();
    }

    //tool selected
    private void selectTool(Button button){
        setSelButton(slT, button==slT);
        setSelButton(pnT, button==pnT);
        setSelButton(hiT, button==hiT);
        setSelButton(erT, button==erT);
    }

    void setAbleButton(Button button, boolean enable){
        button.setTextColor(enable?Color.BLACK:Color.GRAY);
        button.setEnabled(enable);
    }

    void setSelButton(Button button, boolean selected){
        button.setBackgroundColor(selected?Color.DKGRAY:Color.LTGRAY);
        button.setTextColor(selected?Color.WHITE:Color.BLACK);
        button.setSelected(selected);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            if(fileData != null){
                fileData.pathResize(pdfRenderer.getPageCount());
            }
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            try{
                currentPage.close();
            }catch (Exception e){
                System.out.println("*current page closed already "+curIndex);
            }
        }
        try{
            pdfRenderer.close();
        }catch (Exception e){
            System.out.println("*renderer closed already");
        }
        try{
            parcelFileDescriptor.close();
        }catch (Exception e){
            System.out.println("*parcelFileDescription closed already");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            try {
                currentPage.close();
            }catch (Exception e){
                System.out.println("*current page showing closed already "+index);
            }
        }
        // Use `openPage` to open a specific page in PDF.
        if(pageImage.indexInFile != -1) {
            fileData.storePaths(pageImage.indexInFile, pageImage.paths, pageImage.serPaths);
        }
        currentPage = pdfRenderer.openPage(index);
        pageImage.indexInFile = index;
        pageImage.paths = fileData.getPathsCopy(index);
        pageImage.serPaths = fileData.getSerPathsCopy(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }
}
