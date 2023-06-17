#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
using namespace cv;

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"TEST",__VA_ARGS__)

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
JNIEXPORT void  JNICALL
Java_com_example_camerapractice5_MainActivity_ConvertRGBtoGray_1withoutCV(JNIEnv *env, jobject thiz,
                                                                          jbyteArray in, jbyteArray out, jint w,
                                                                          jint h) {
//    // TODO: implement ConvertRGBtoGray_withoutCV()
//    //Mat (int rows, int cols, int type, void *data, size_t step=AUTO_STEP)
//    //int len = env->GetArrayLength(in);
//    unsigned char* buf_ptr1 = ...;
//    //unsigned char* buf_ptr1 = (unsigned char*)malloc(sizeof(in));
//    //buf_ptr1 = (unsigned char*) &in[0];
//    unsigned char *buf_ptr2 = (unsigned char*)malloc(sizeof(out));
//    Mat mat_in(h, w, CV_8UC3, *buf_ptr1);
//    Mat mat_out(h, w, CV_8UC3, *buf_ptr2);
//    cvtColor(mat_in, mat_out, COLOR_RGBA2GRAY);
////    for(int i = 0; i < h * w; i++){
////        out[i] = mat_out.data[i];
////    }
//    std::memcpy(buf_ptr2,buf_ptr1,sizeof(in));
//    free(buf_ptr1);
//    free(buf_ptr2);

    int sz1 = env->GetArrayLength(in);
    int sz2 = env->GetArrayLength(out);

    LOGE("sz1 %d sz2 %d\n", sz1, sz2);

    jbyte* buf_ptr1 = env->GetByteArrayElements(in, nullptr);
    jbyte* buf_ptr2 = env->GetByteArrayElements(out, nullptr);

    Mat matIn(h, w, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr1));
    Mat matOut(h, w, CV_8UC1, reinterpret_cast<unsigned char*>(buf_ptr2));

    cvtColor(matIn, matOut, cv::COLOR_BGRA2GRAY);

    //cvtColor(matIn, matOut, COLOR_RGB2GRAY);

    LOGE("matIn  %d x %d x %d, buf_ptr1 %p matIn.data %p\n", matIn.rows, matIn.cols, matIn.channels(), buf_ptr1, matIn.data);
    LOGE("matOut %d x %d x %d, buf_ptr2 %p matIn.data %p\n", matOut.rows, matOut.cols, matOut.channels(), buf_ptr2, matOut.data);
    //LOGE("t  %d x %d x %d, t.data %p\n", t.rows, t.cols, t.channels(), t.data);

    env->ReleaseByteArrayElements(in, buf_ptr1, 0);
    env->ReleaseByteArrayElements(out, buf_ptr2, 0);
}