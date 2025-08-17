//#include "com_fsoft_FP_sDraw_instruments_Filler.h"
#include "arrayqueue.c"
#include <jni.h>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdio.h>
#include <unistd.h>


#define  LOG_TAG    "fillNative"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

void queue(int width,int height, int cx, int cy);

char *queued;	// to check if the element queued
int top = -1;
int bottom = -1;
int left = -1;
int right = -1;


#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wimplicit-function-declaration"
JNIEXPORT jstring  JNICALL Java_com_fsoft_FP_1sDraw_instruments_Filler_fillNative (JNIEnv * env, jobject obj, jobject bitmap, jint x, jint y, jint threshold, jint overfill, int color, int writeLog)
{
    top = -1;
    bottom = -1;
    left = -1;
    right = -1;

    if(writeLog)LOGI("Color to fill = %08x", color);
    AndroidBitmapInfo bitmapInfo;
    unsigned int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0)
        return (*env)->NewStringUTF(env, "Error: AndroidBitmap_getInfo() failed !");
    if(writeLog)LOGI("AndroidBitmap_getInfo ... OK");


    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return (*env)->NewStringUTF(env, "Error: Bitmap format is not RGBA_8888!");
    if(writeLog)LOGI("check bitmapInfo.format ... OK");


    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
        return (*env)->NewStringUTF(env, "Error: AndroidBitmap_lockPixels() failed!");
    if(writeLog)LOGI("AndroidBitmap_lockPixels ... OK");

    int width = (int)bitmapInfo.width;
    int height = (int)bitmapInfo.height;
    int maxSide = width > height?width:height;
    if(writeLog)LOGI("Bitmap width = %d", width);
    if(writeLog)LOGI("Bitmap height = %d", height);
    if(writeLog)LOGI("Bitmap maxSide = %d", maxSide);
    if (x >width-1 || y > height-1 || x < 0 || y < 0)
        return (*env)->NewStringUTF(env, "Error: Coordinates is not valid");
    if(writeLog)LOGI("checkCoordinates ... OK");

    ret = InitQueue(maxSide*4);
    if(ret == 0)
        return (*env)->NewStringUTF(env, "Error: No memory to init theQueue");
    if(writeLog)LOGI("InitQueue ... OK");

    int* pixels = (int*) bitmapPixels;
    if(writeLog)LOGI("get pixels ... OK");

    Enqueue(x);
    Enqueue(y);
    int oldColor = pixels[y*bitmapInfo.width+x];
    if(writeLog)LOGI("oldColor = %08x", oldColor);
    int oldAlpha = (int) ((oldColor & 0xFF000000) >> 24);
    int oldRed = (int) ((oldColor & 0xFF0000) >> 16);
    int oldGreen = (int)((oldColor & 0x00FF00) >> 8);
    int oldBlue = (int) (oldColor & 0x0000FF);
    queued = malloc (sizeof(char) * width*height);
    if(queued == 0)
        return (*env)->NewStringUTF(env, "Error: No memory to init queued array");
    for(int i=0; i<sizeof(char) * width*height; i++)
        queued[i] = 0;
    if(writeLog)LOGI("Enqueue ... OK");

    int alpha = (int) ((color & 0xFF000000) >> 24);
    //LOGI("alpha = %d", alpha);
    int red = (int) ((color & 0xFF0000) >> 16); //this is red
    int green = (int)((color & 0x00FF00) >> 8); //this is green
    int blue = (int) (color & 0x0000FF); //this is blue
    if(writeLog)LOGI("Calculate components ... OK");

    if(writeLog)LOGI("FILL ... BEGIN");
    int cnt = 0;
    while(!isEmpty()){//not empty
        cnt++;
        int cx = Dequeue();
        int cy = Dequeue();

        if(cx >= 0 && cy >= 0 && cx < width && cy < height) {
            int currentPixel =pixels[cy * width + cx];
            bool valid = currentPixel == oldColor;
            //LOGI("PIXEL = %d, %d, %d, %d", cnt, currentPixel, oldColor, valid);
            if(!valid && threshold > 1){
                //check with threshold
                int currentAlpha = (int) ((currentPixel & 0xFF000000) >> 24);
                int currentRed = (int) ((currentPixel & 0xFF0000) >> 16);
                int currentGreen = (int)((currentPixel & 0x00FF00) >> 8);
                int currentBlue = (int) (currentPixel & 0x0000FF);
                int dif = abs(currentAlpha-oldAlpha) + abs(currentRed-oldRed) + abs(currentGreen-oldGreen) + abs(currentBlue-oldBlue);
//                LOGI("-------------");
//                LOGI("currentRed = %d", currentRed);
//                LOGI("currentGreen = %d", currentGreen);
//                LOGI("currentBlue = %d", currentBlue);
//                LOGI("red = %d", red);
//                LOGI("green = %d", green);
//                LOGI("blue = %d", blue);
//                LOGI("dif = %d", dif);
                if(dif < threshold)
                    valid = true;
            }
            if (valid) {
                queue(width, height, cx + 1, cy);
                queue(width, height, cx - 1, cy);
                queue(width, height, cx, cy + 1);
                queue(width, height, cx, cy - 1);
            }
            /*From android ARGB_8888 documentation:
             * Use this formula to pack into 32 bits:
             * int color = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);*/
            if(overfill == 1 || valid){
                //0xAABBGGRR
                //pixels[cy * width + cx] = 0x22FFFFFF;
                pixels[cy * width + cx] = (alpha & 0xff) << 24 | ((blue*alpha/255) & 0xff) << 16 | ((green*alpha)/255 & 0xff) << 8 | ((red*alpha/255) & 0xff);
                /*
                 * Dr Failov, [11.02.2023 11:14]
                    Я понял почему в андроиде такой хреновый альфа-блендинг

                    Dr Failov, [11.02.2023 11:14]
                    Вот схема пикселя в ARGB8888
                    //0xAABBGGRR

                    Dr Failov, [11.02.2023 11:15]
                    А теперь я просто покажу тебе два цвета

                    Dr Failov, [11.02.2023 11:15]
                    0x22FFFFFF - полностью белый непрозрачный

                    Dr Failov, [11.02.2023 11:15]
                    0x22AAAAAA - частично прозрачный белый

                    Dr Failov, [11.02.2023 11:16]
                    т.е. предельным значением цвета для полупрозрачного цвета является значение альфа

                    Dr Failov, [11.02.2023 11:16]
                    таким образом, чем прозрачнее альфа, тем меньше получается битность цвета*/
                //pixels[cy * width + cx] = pixels[cy * width + cx] | (0xFF000000);
                //pixels[cy * width + cx] = (alpha << 24) | (blue << 16) | (green << 8) | red;
            }
        }
    }
    if(writeLog)LOGI("FILL ... OK");

    AndroidBitmap_unlockPixels(env, bitmap);
    if(writeLog)LOGI("AndroidBitmap_unlockPixels ... OK");

    release();
    free(queued);
    if(writeLog)LOGI("release memory ... OK");

    char str[150];
    sprintf(str, "{\"result\":\"SUCCESS\", \"top\":%d, \"bottom\":%d, \"left\":%d, \"right\":%d}", top, bottom, left, right);
    return (*env)->NewStringUTF(env, str);
}
#pragma clang diagnostic pop

int abs(int i){
    if(i < 0)
        return -i;
    return i;
}
void queue(int width,int height, int cx, int cy){//return size
    //LOGI("queue = W%d, H%d, X%d, Y%d", width, height, cx, cy);
    if (cx >width-1 || cy > height-1 || cx < 0 || cy < 0)
        return;


    if(top == -1 || cy < top)
        top = cy;
    if(bottom == -1 || cy > bottom)
        bottom = cy;
    if(left == -1 || cx < left)
        left = cx;
    if(right == -1 || cx > right)
        right = cx;

    if(queued[cy*width+cx] == 0){
        Enqueue(cx);
        Enqueue(cy);
        queued[cy*width+cx] = 1;
    }
    return;
}