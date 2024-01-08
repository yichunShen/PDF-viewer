package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";
    int indexInFile = -1;

    // drawing path
    Path path = null;
    SerPath serPath = null;
    ArrayList<Pair<Path, Integer>> paths = new ArrayList();
    ArrayList<SerPair<SerPath, Integer>> serPaths = new ArrayList<>();
    MainActivity owner;
    ArrayList<Point> drawingPts = new ArrayList<Point>();
    Paint drawingPaint = new Paint();

    // image to display
    Bitmap bitmap;
    Paint paintPn;
    Paint paintHi;

    //zoom pan
    // we save a lot of points because they need to be processed
    // during touch events e.g. ACTION_MOVE
    float x1, x2, y1, y2, old_x1, old_y1, old_x2, old_y2;
    float mid_x = -1f, mid_y = -1f, old_mid_x = -1f, old_mid_y = -1f;
    int p1_id, p1_index, p2_id, p2_index;

    // store cumulative transformations
    // the inverse matrix is used to align points with the transformations - see below
    Matrix matrix = new Matrix();
    Matrix inverse = new Matrix();

    // constructor
    public PDFimage(MainActivity context) {
        super(context);
        owner = context;
        paintPn = new Paint();
        paintPn.setColor(Color.BLUE);
        paintPn.setStrokeWidth(8);
        paintPn.setStyle(Paint.Style.STROKE);
        paintPn.setAlpha(255);
        paintHi = new Paint();
        paintHi.setColor(Color.YELLOW);
        paintHi.setStrokeWidth(32);
        paintHi.setStyle(Paint.Style.STROKE);
        paintHi.setAlpha(120);
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getPointerCount()) {
            // 1 point is drawing or erasing
            case 1:
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                // invert using the current matrix to account for pan/scale
                // inverts in-place and returns boolean
                inverse = new Matrix();
                matrix.invert(inverse);

                // mapPoints returns values in-place
                float[] inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);
                x1 = inverted[0];
                y1 = inverted[1];

                if(owner.pnSt() == 0){
                    return true;
                }
                drawingPts.add(new Point((int)event.getX(), (int)event.getY()));
                drawingPaint = getPaint(owner.pnSt());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(LOGNAME, "Action down");
                        path = new Path();
                        serPath = new SerPath();
                        path.moveTo(event.getX(), event.getY());
                        serPath.points.add(new SerPair(new SerPair(event.getX(), event.getY()), 0));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(LOGNAME, "Action move");
                        path.lineTo(event.getX(), event.getY());
                        serPath.points.add(new SerPair(new SerPair(event.getX(), event.getY()), 1));
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(LOGNAME, "Action up");
                        paths.add(new Pair(path, owner.pnSt()));
                        serPaths.add(new SerPair(serPath, owner.pnSt()));
                        drawingPts.clear();
                        owner.undo.addLast(paths.get(paths.size()-1));
                        owner.undoInd.addLast(1+owner.pnSt());
                        owner.setAbleButton(owner.udBut, !owner.undo.isEmpty());
                        break;
                }
                break;
            // 2 points is zoom/pan
            case 2:
                // point 1
                p1_id = event.getPointerId(0);
                p1_index = event.findPointerIndex(p1_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p1_index), event.getY(p1_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x1 < 0 || old_y1 < 0) {
                    old_x1 = x1 = inverted[0];
                    old_y1 = y1 = inverted[1];
                } else {
                    old_x1 = x1;
                    old_y1 = y1;
                    x1 = inverted[0];
                    y1 = inverted[1];
                }

                // point 2
                p2_id = event.getPointerId(1);
                p2_index = event.findPointerIndex(p2_id);

                // mapPoints returns values in-place
                inverted = new float[] { event.getX(p2_index), event.getY(p2_index) };
                inverse.mapPoints(inverted);

                // first pass, initialize the old == current value
                if (old_x2 < 0 || old_y2 < 0) {
                    old_x2 = x2 = inverted[0];
                    old_y2 = y2 = inverted[1];
                } else {
                    old_x2 = x2;
                    old_y2 = y2;
                    x2 = inverted[0];
                    y2 = inverted[1];
                }

                // midpoint
                mid_x = (x1 + x2) / 2;
                mid_y = (y1 + y2) / 2;
                old_mid_x = (old_x1 + old_x2) / 2;
                old_mid_y = (old_y1 + old_y2) / 2;

                // distance
                float d_old = (float) Math.sqrt(Math.pow((old_x1 - old_x2), 2) + Math.pow((old_y1 - old_y2), 2));
                float d = (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

                // pan and zoom during MOVE event
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d(LOGNAME, "Multitouch move");
                    // pan == translate of midpoint
                    float dx = mid_x - old_mid_x;
                    float dy = mid_y - old_mid_y;
                    matrix.preTranslate(dx, dy);
                    Log.d(LOGNAME, "translate: " + dx + "," + dy);

                    // zoom == change of spread between p1 and p2
                    float scale = d/d_old;
                    scale = Math.max(0, scale);
                    matrix.preScale(scale, scale, mid_x, mid_y);
                    Log.d(LOGNAME, "scale: " + scale);

                    // reset on up
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    old_x1 = -1f;
                    old_y1 = -1f;
                    old_x2 = -1f;
                    old_y2 = -1f;
                    old_mid_x = -1f;
                    old_mid_y = -1f;
                }
                break;
            // I have no idea what the user is doing for 3+ points
            default:
                break;
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
//    public void setBrush(Paint paint) {
//        this.paint = paint;
//    }

    @Override
    protected void onDraw(Canvas canvas) {
//        System.out.println("-----------------------on draw");
        // apply transformations from the event handler above
        canvas.concat(matrix);

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        drawingPaint.setStrokeWidth(drawingPaint.getStrokeWidth()/2);
        for(Point point: drawingPts){
            canvas.drawCircle(point.x, point.y, drawingPaint.getStrokeWidth(), drawingPaint);
        }
        drawingPaint.setStrokeWidth(drawingPaint.getStrokeWidth()*2);
        // draw lines over it
        for (Pair<Path, Integer> pair : paths) {
            canvas.drawPath(pair.first, getPaint(pair.second));
        }
        super.onDraw(canvas);
    }

    Paint getPaint(int ptInd){
        if(ptInd == 1){
            return paintPn;
        }else if(ptInd == 2){
            return paintHi;
        }
        return null;
    }
}
