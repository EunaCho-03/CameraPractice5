#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_camerapractice5_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                               jlong mat_addr_input,
                                                               jlong mat_addr_result) {
    // TODO: implement ConvertRGBtoGray()
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_camerapractice5_MainActivity_ConvertRGBtoGray_1withoutCV(JNIEnv *env, jobject thiz,
                                                                          jbyteArray in, jbyteArray out, jint w,
                                                                          jint h) {
    // TODO: implement ConvertRGBtoGray_withoutCV()
    unsigned char *buf_ptr = ...;
    Mat mat_inout(h, w, CV_8UC3, in);
}