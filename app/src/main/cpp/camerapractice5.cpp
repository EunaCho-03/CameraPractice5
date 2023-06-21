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

//    int sz1 = env->GetArrayLength(in);
//    int sz2 = env->GetArrayLength(out);
    //LOGE("sz1 %d sz2 %d\n", sz1, sz2);

    jbyte* buf_ptr1 = env->GetByteArrayElements(in, nullptr);
    jbyte* buf_ptr2 = env->GetByteArrayElements(out, nullptr);

    Mat matIn(h, w, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr1));
    Mat matOut(h, w, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr2));

    //LOGE("matOut 1 %d %d %d %p\n", matOut.rows, matOut.cols, matOut.channels(), matOut.data);

    Mat t; //회색 변환때 쓰일 템포러리 Mat
    cvtColor(matIn, t, COLOR_BGRA2GRAY);
    //cvtColor(t,matOut,COLOR_GRAY2BGR); ->회색에서 컬러로 바뀌어도 BGRA 값에는 회색만 담길거니깐
    //다시 matOut에 집어넣어보았지만 그러면 matOut.data로 나오는 주소값이 바뀌어버린다
    //LOGE("t %d %d %d\n", t.rows, t.cols, t.channels());

    //Mat alpha(h, w, CV_8UC1, Scalar(255));

    std::vector<Mat> chan_dst; //채널 도착지점. 벡터는 행렬, 여기서 < >안에 넣을 데이터타입을 선언
    cv::split(matOut, chan_dst); //4채널을 가진 matOut을 split해서 각각의 한채널을 chan_dst 배열에 담는다

    for(int i=0; i<3; i++)
        t.copyTo(chan_dst[i]); // 순서대로 chan_dst에 0,1,2에 t의 B,G,R 값을 넣는다
    chan_dst[3].setTo(255); // BGRA에서 0123, 3은 A인데 이 값은 디폴트로 255를 준다 (A: 투명도)

    cv::merge(chan_dst, matOut); // 다시 하나의 채널로 합치기 위해 값이 담긴 배열인 chan_dst의 결과를 matOut에 담는다

    //LOGE("matOut 3 %d %d %d %p\n", matOut.rows, matOut.cols, matOut.channels(), matOut.data);
//    LOGE("matIn  %d x %d x %d, buf_ptr1 %p matIn.data %p\n", matIn.rows, matIn.cols, matIn.channels(), buf_ptr1, matIn.data);
//    LOGE("matOut %d x %d x %d, buf_ptr2 %p matIn.data %p\n", matOut.rows, matOut.cols, matOut.channels(), buf_ptr2, matOut.data);
    //LOGE("t  %d x %d x %d, t.data %p\n", t.rows, t.cols, t.channels(), t.data);

    env->ReleaseByteArrayElements(in, buf_ptr1, 0);
    env->ReleaseByteArrayElements(out, buf_ptr2, 0);
}