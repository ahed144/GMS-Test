package com.example.ahed;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class storing one element of caricature template
 * tied to landmarks with given positions
 */
public class TemplateElement {


    public static class LandmarkPoint    {
        public PointF point;
        LandmarkType type;
        public LandmarkPoint(PointF _point,LandmarkType _type){
            point = _point;
            type = _type;
        }
        public  LandmarkPoint(){}
    }
    public static enum LandmarkType{
        FACE_TOP_LEFT,
        FACE_TOP_RIGHT,
        FACE_BOTTOM_LEFT,
        FACE_BOTTOM_RIGHT,
        EYES_LEFT,
        EYES_RIGHT,
        MOUTH_LEFT,
        MOUTH_RIGHT
    }
    List<LandmarkPoint> landmarks;
    Bitmap bitmap;
    Rect position;
    List<LandmarkPoint> frame_landmarks;
    private int bitmapWidth,bitmapHeight;

    public TemplateElement()
    {
        landmarks = new ArrayList<>();
    }

    /*
    This function adds landmark base point to picture
    Coordinates are given within template picture.
    When drawing on camera frame, template element is moved and resized
    to make landmarks base point on template match landmarks on camera frame
     */
    void addLandmark(LandmarkType type,PointF point)    {
        landmarks.add(new LandmarkPoint(point,type));
    }

    void setBitmap(Bitmap _bitmap){
        bitmap = _bitmap;
        bitmapHeight = bitmap.getHeight();
        bitmapWidth = bitmap.getWidth();
    }

    void setBitmapHeight(int height)
    {
        bitmapHeight = height;
    }

    public void setBitmapWidth(int width) {
        this.bitmapWidth = width;
    }

    void setBitmapSize(int width,int height)
    {
        bitmapHeight = height;
        bitmapWidth = width;
    }

    Set<LandmarkType> getRequiredLandmarks()    {
        Set<LandmarkType> res = new HashSet<LandmarkType>();
        for (LandmarkPoint base_point:
             landmarks) {
            res.add(base_point.type);
        }
        return  res;
    }

    void setFrameLandmarks(List<LandmarkPoint> frameLandmarks){
        frame_landmarks = frameLandmarks;
    }

    void draw(Canvas canvas)    {
        Log.d("DRAW", "drawing element");
        if(bitmap == null)
            return;
        if(landmarks.isEmpty())
            return;
        if(landmarks.size()==1)
        {
            LandmarkPoint base = null;
            for (LandmarkPoint p:
                 frame_landmarks) {
                if(p.type == landmarks.get(0).type)
                    base = p;
            }
            if(base == null)
                return;
            int left = (int)base.point.x-bitmap.getWidth()/2;
            if(left < 0 )
                left = 0;
            int top = (int)base.point.y-bitmap.getHeight()/2;
            if(top < 0 )
                top = 0;
            int right = left + bitmap.getWidth();
            if(right>canvas.getWidth())
                right = canvas.getWidth();
            int bottom = top + bitmap.getHeight();
            if(bottom>canvas.getHeight())
                bottom = canvas.getHeight();
            position = new Rect(left,top,right,bottom);
            canvas.drawBitmap(bitmap,new Rect(0,0,bitmap.getWidth(), bitmap.getHeight()),
                    position,null);
            return;
        }
        if(landmarks.size() == 2)
        {
            Log.d("DRAW", "two landmarks " + frame_landmarks.size());

            LandmarkPoint one = null;
            LandmarkPoint two = null;
            for (LandmarkPoint p:
                    frame_landmarks) {
                if(p.type == landmarks.get(0).type)
                    one = p;
                if(p.type == landmarks.get(1).type)
                    two = p;
            }
            if(one == null || two == null || one.type == two.type)
                return;
            LandmarkPoint leftFramePoint = null,rightFramePoint = null;
            if(one.point.x < two.point.x)
            {
                leftFramePoint = one;
                rightFramePoint = two;
            }
            else
            {
                leftFramePoint = two;
                rightFramePoint = one;
            }
            LandmarkPoint leftBasePoint = null,rightBasePoint = null;
            if(landmarks.get(0).point.x < landmarks.get(1).point.x)
            {
                leftBasePoint = landmarks.get(0);
                rightBasePoint = landmarks.get(1);
            }
            else
            {
                leftBasePoint = landmarks.get(1);
                rightBasePoint = landmarks.get(0);
            }

            int left,right,top,bottom;

            float frameDx = (leftFramePoint.point.x-rightFramePoint.point.x);
            float frameDy = (leftFramePoint.point.y-rightFramePoint.point.y);
            float baseDx = (leftBasePoint.point.x-rightBasePoint.point.x);
            float baseDy = (leftBasePoint.point.y-rightBasePoint.point.y);

            float frameDist = (float) Math.sqrt(frameDx * frameDx + frameDy * frameDy);
            float baseDist = (float) Math.sqrt(baseDx * baseDx + baseDy * baseDy);

            float scale_x = frameDx/baseDx;
            float scale_y = frameDy/baseDy;
            float scale=scale_x;
            /*if(scale_y>scale_x)
                scale = scale_y;*/

            float frameAngle = (float)(Math.asin(frameDy / frameDx)*180/ Math.PI);
            float baseAngle = (float)(Math.asin(baseDy / baseDx)*180/ Math.PI);
            float angle = frameAngle-baseAngle;

            float xBase = ((leftBasePoint.point.x+rightBasePoint.point.x)/2);
            float xFrame = ((leftFramePoint.point.x+rightFramePoint.point.x)/2);
            left = (int)(xFrame-xBase*scale);
            //left = (int)(leftFramePoint.point.x - leftBasePoint.point.x*scale);
            if(left<0)
                left = 0;

            float yBase = ((leftBasePoint.point.y+rightBasePoint.point.y)/2);
            float yFrame = ((leftFramePoint.point.y+rightFramePoint.point.y)/2);
            top = (int)(yFrame - scale * yBase);
            if(top < 0 )
                top = 0;

            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE); //no fill
            paint.setStrokeWidth(2);

            float imageScaleX = (float)bitmap.getWidth()/bitmapWidth;
            float imageScaleY = (float)bitmap.getHeight()/bitmapHeight;
            Log.d("DETECT", "image scale " + imageScaleX + " " + imageScaleY);
            Log.d("DETECT", "bitmap " + bitmap.getWidth() + " " + bitmap.getHeight());

            canvas.save();
            canvas.translate(left, top);
            canvas.scale(scale_x / imageScaleX, scale_x / imageScaleY);
            canvas.rotate(angle);
            canvas.drawBitmap(bitmap, 0, 0, null);

            canvas.restore();
            return;

        }
        else
            return;


    }
}
