#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
//#include <opencv2/imgcodecs.hpp>
//#include "opencv2/highgui/highgui.hpp"
//#include "opencv2/highgui/highgui_c.h"

//#include "opencv2/imgproc.hpp"

using namespace cv;
using namespace std;

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


//extern "C"
//JNIEXPORT void JNICALL
//Java_com_example_camerapractice5_MainActivity_drawHough(JNIEnv *env, jobject thiz, jbyteArray in,
//                                                        jint image_width, jint image_height) {
    //가져온 byteArray 가리킬 포인터
//    jbyte* buf_ptr = env->GetByteArrayElements(in, nullptr);
//    //가져온 이미지 Mat으로
//    Mat img(image_height, image_width, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr));
//    LOGE("img  %d x %d x %d, buf_ptr %p img.data %p\n", img.rows, img.cols, img.channels(), buf_ptr, img.data);
//
//    Mat img_gray; //Edge detection을 위해 회색으로 변환
//    cvtColor(img, img_gray, COLOR_BGR2GRAY);
//    LOGE("img_gray  %d x %d x %d,  img_gray.data %p\n", img_gray.rows, img_gray.cols, img_gray.channels(), img_gray.data);
//
//    Mat img_canny; //Canny로 Edge Detection
//    Canny(img_gray, img_canny, 150, 255);
//
//    vector<Vec2f> lines; //Vec2f - 데이터가 2개인 평면
//    //img_canny에서 HoughLines 함수 통해 직선 검출하고 직선들은 lines라는 배열에 저장
//    HoughLines(img_canny, lines, 1, CV_PI / 180, 150);
//
//    Mat img_hough; //왜 복붙을 하는지 모르겠으나 나중에 이미지를 그릴때 img를 쓰면 안먹힘
//    img.copyTo(img_hough);
//
////    Mat img_lane; // 검은 이미지에 선만 보고 싶을때
////    threshold(img_canny, img_lane, 150, 255, THRESH_MASK);
//
//
//
//    for (size_t i = 0; i < lines.size(); i++)
//    {
//        float rho = lines[i][0], theta = lines[i][1];
//        Point pt1, pt2;
//        double a = cos(theta), b = sin(theta);
//        double x0 = a * rho, y0 = b * rho;
//        pt1.x = cvRound(x0 + 1000 * (-b));
//        pt1.y = cvRound(y0 + 1000 * (a));
//        pt2.x = cvRound(x0 - 1000 * (-b));
//        pt2.y = cvRound(y0 - 1000 * (a));
//        line(img_hough, pt1, pt2, Scalar(0,0,255), 2, 8);
//        //line(img_lane, pt1, pt2, Scalar::all(255), 1, 8);
//    }

    //namedWindow( "Display window", WINDOW_AUTOSIZE );
    //imshow("img_canny",img_hough);
    //cvShowImage("img_canny", img_hough);
    //imshow("img_lane", img_lane);

    //waitKey(0);
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_camerapractice5_MainActivity_drawHough(JNIEnv *env, jobject thiz, jbyteArray in, jbyteArray houghOut,
                                                        jint image_width, jint image_height) {

    jbyte* buf_ptr1 = env->GetByteArrayElements(in, nullptr);
    jbyte* buf_ptr2 = env->GetByteArrayElements(houghOut, nullptr);

    // src: in으로 가지고온 소스 mat / matOut: src로 바뀐걸로 main에서 활용할  mat
    Mat src(image_height, image_width, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr1));
    Mat matOut(image_height, image_width, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr2));

    //Canny Edge detection을 위해 흑백으로 우선 바꿔줌
    Mat img_gray;
    cvtColor(src, img_gray, COLOR_BGRA2GRAY);

    //회색인 이미지에서 캐니로 엣지 검출하기
    Mat img_canny;
    Canny(src, img_canny, 50, 300, 3);
    //점의 개수가 50보다 작으면 엣지가 아니라고 인식. 300보다 크면 엣지. 사이에 있으면 그 주위에 엣지가 있는지 확인 후 엣지라고 인식
    //aparture = 소벨 연산 마스크 크기? 디폴트: 3

    //HoughLinesP
    vector<Vec4i>linesP; // 4i = 4개의 integer(endpoints 4개)를 넣을 벡터타입 선언
    HoughLinesP(img_canny, linesP, 1, CV_PI/180, 200, 50,5); //linesP애 선둘을 저장할거임 (배열)
    //
    //minLineLength = 검출할 직선의 최소 길이 (단위는 픽셀)
    //max_line_gap = 검출할 선 위의 점들 사이의 죄대 거리 (점 사이의 거리가 이 값보다 크면 다른 선으로 간주)

    for (size_t i = 0; i < linesP.size(); i++) //모든 검출된 선들 순환
    {
        Vec4i l = linesP[i]; //지금 있는 선들이 저장되어있는 linesP의 i번째를 l에 저장
        line(matOut, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(255, 0, 0), 2); //matOut에 시작점 pt1부터 끝점 pt2까지 그림
        //Point(l[0], l[1]) = 시작점 , Point(l[2], l[3]) = 끝점 = line 함수가 이 둘을 이어줌
        //Scalar(255,0,0) = RGB로, 선은 빨간색으로 지정 / 2 = 선 굵기
    }

    //HoughLines

    std::vector<Vec2f> lines; // 허프 변환으로 검출된 직선을 저장할 어레이 (벡터 자료형으로, 2f는 데이터가 float형 2개)
    //2개 데이터는 각각 [rho, theta]
    HoughLines(img_canny, lines, 1, CV_PI / 180, 300, 0, 0); //HoughLines 함수 이용해서 img_canny로부터 직선 검출하고 lines 배열에 저장.
    //rho = 변환된 그래프에서 선에서 원점까지의 거리 (계산할 픽셀 해상도) 1 사용하면 됨
    //theta = 계산할 각도의 해상도. 모든 방향에서 직선을 검출할거면 PI/180사용하면 됨
    //직선 검출 반응이 민감해서 thereshold 높였음

    // 허프 트랜스폼

    for (size_t i = 0; i < lines.size(); i++) { // 검출된 모든 선 순회하기
        float rho = lines[i][0]; //i번째 검출된 선, lines 배열에서 첫번째인 rho
        float theta = lines[i][1]; // i번째 검출된 선, lines 배열에서 두번째인 theta
        Point pt1, pt2; // 시작점과 끝점 선언
        double a = cos(theta); //x,y축에 대한 삼각비
        double b = sin(theta);
        //(x0,y0) 좌표 구하기
        double x0 = a * rho; // cosθ * rho
        double y0 = b * rho; // sinθ * rho

        pt1.x = cvRound(x0 + 1000 * (-b)); // 시작점의 x좌표. 곱하는 선으로 선분 길이를 조정함.
        // 1000보다 작으면 더 짧은 선이, 크면 더 긴 선이 그려질거임
        pt1.y = cvRound(y0 + 1000 * (a));
        pt2.x = cvRound(x0 - 1000 * (-b)); // 끝점의 y좌표
        pt2.y = cvRound(y0 - 1000 * (a));
        line(matOut, pt1, pt2, Scalar(255, 0, 0), 3, LINE_AA); // 선 그리기
    }

    env->ReleaseByteArrayElements(in, buf_ptr1, 0);
    env->ReleaseByteArrayElements(houghOut, buf_ptr2, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_camerapractice5_MainActivity_drawCanny(JNIEnv *env, jobject thiz, jbyteArray in,
                                                        jbyteArray canny_out, jint width,
                                                        jint height) {

    jbyte* buf_ptr1 = env->GetByteArrayElements(in, nullptr);
    jbyte* buf_ptr2 = env->GetByteArrayElements(canny_out, nullptr);

    Mat src(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr1));
    Mat matOut(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(buf_ptr2));

//    Mat img_gray;
//    cvtColor(src, img_gray, COLOR_BGRA2GRAY);
//
//    Mat img_blur;
//    GaussianBlur(img_gray, img_blur, Size(3,3), 0);

//    Mat sobelx, sobely, sobelxy;
//    Sobel(img_blur, sobelx, CV_64F, 1, 0, 5);
//    Sobel(img_blur, sobely, CV_64F, 0, 1, 5);
//    Sobel(img_blur, sobelxy, CV_64F, 1, 1, 5);

    //vector<Vec4i>linesP;
    Mat edges;// 결과를 넣을 mat
    Canny(src, edges, 50, 150, 3); //검출된 에지는 edges에 들어감
    //apertureSize 3 = Sobel 커넬 사이즈 3 X 3
    //작은 사이즈: 검출 센서가 민감해서 얇은 선도 검출해내 더 선들을 검출하지만 잘못된 선까지 검출할 수 있음
    //큰 사이즈: 안정되어 확실한 선만 검출
    cvtColor(edges, matOut, COLOR_GRAY2BGRA);

//    for (size_t i = 0; i < linesP.size(); i++) //모든 검출된 선들 순환
//    {
//        LOGE("Drawing detected lines");
//        Vec4i l = linesP[i]; //지금 있는 선들이 저장되어있는 linesP의 i번째를 l에 저장
//        line(edges, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(255, 0, 0), 2); //matOut에 시작점 pt1부터 끝점 pt2까지 그림
//    }

    env->ReleaseByteArrayElements(in, buf_ptr1, 0);
    env->ReleaseByteArrayElements(canny_out, buf_ptr2, 0);

}