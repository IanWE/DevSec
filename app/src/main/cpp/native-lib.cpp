#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <libflush/libflush.h>
#include <libflush/hit.h>
#include <libflush/calibrate.h>
#include "split.c"
#include "ReadOffset.h"
#include "logoutput.h"
//#include "CheckFlags.cpp"


int finishtrial1 = 0;
jint* filter;
int t[] = {0,0};

int pausescan = 0;
int continueRun = 0;
int threshold = 0;
int *flags;
int sum_length = 0;
size_t* addr= NULL;

int compiler_position = 5;
int log_length = 0;
int logs[300000] = {0};
long times[300000] = {0};
int thresholds[300000] = {0};
int length_of_camera_audio[2] = {0,0};
pthread_mutex_t g_lock;

int* camera_pattern;
int* audio_pattern;
int camera_audio[] = {1,2};//indexes of camera list and audio list
std::vector<std::string> camera_list;
std::vector<std::string> audio_list;

extern "C" JNIEXPORT jstring JNICALL
Java_com_SMU_DevSec_SideChannelJob_scan(
        JNIEnv *env,
        jobject thiz,jintArray ptfilter) {
    int* arrp = env->GetIntArrayElements(ptfilter,0);
    continueRun = 1;
    for(int i=0;i<length_of_camera_audio[0];i++)//camera
    {
        if (arrp[i] == 1) {
            *((size_t *) addr[camera_audio[0]] + i) = 0;
        }
        //else if(*((size_t *) addr[camera_audio[0]] + i)!=0){
        //    LOGD("Camera function %s are reserved",camera_list[i].c_str());
        //}
    }
    for(int i=length_of_camera_audio[0];i<length_of_camera_audio[0]+length_of_camera_audio[1];i++)//audio
    {
        if(arrp[i]==1) {
            *((size_t *) addr[camera_audio[1]] + i - length_of_camera_audio[0]) = 0;
        }
        //else if(*((size_t *) addr[camera_audio[i]] + i)!=0){
        //    LOGD("Audio function %s are reserved",audio_list[i-length_of_camera_audio[0]].c_str());
        //}
    }
    for(int i=1;i<3;i++){
        int c = i-1;
        int t0=0;
        int t1=0;
        for (int j = 0; j < length_of_camera_audio[c]; j++) {
            size_t target = *((size_t *) addr[i] + j);
            if (target == 0) {//if the target is 0, skip it.
                t0++;
                continue;
            }
            t1++;
        }
        if(i==1){
            LOGD("In Camera List, %d are null functions, %d are available functions.\n",t0,t1);
            t[0] = t1;
        }
        else {
            LOGD("In Audio List, %d are null functions, %d are available functions.\n", t0, t1);
            t[1] = t1;
        }
    }
    hit(&g_lock, compiler_position, &continueRun,
        threshold, flags, times, thresholds, logs, log_length,sum_length,
        camera_pattern, audio_pattern, length_of_camera_audio, addr, &pausescan);
    LOGD("Finished scanning");
    return env->NewStringUTF("");
}

template <typename T>
void swap(T *a,T *b)
{
    T temp;
    temp=*a;
    *a=*b;
    *b=temp;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_TrialModelStages_trial1(
        JNIEnv *env, jobject thiz) {
    LOGD("Start trial 1.\n");
    int length_alive_function = length_of_camera_audio[0]+length_of_camera_audio[1];
    memset(filter,0,length_alive_function*sizeof(int));
    stage1_(filter, threshold, length_of_camera_audio, addr, camera_audio, &finishtrial1,sum_length); //eliminate all poping functions.
    LOGD("Finish TrialMode 1");
    return;
}

extern "C" JNIEXPORT void JNICALL
Java_com_SMU_DevSec_SideChannelJob_trial2(
        JNIEnv *env, jobject thiz) {
    LOGD("Start trial 2.\n");
    continueRun = 1;
    hit(&g_lock, compiler_position, &continueRun,
        threshold, flags, times, thresholds, logs, log_length, sum_length,
        camera_pattern, audio_pattern, length_of_camera_audio, addr, &pausescan);
    LOGD("Finish TrialMode 2");
}

int address_check(std::string function){
    //std::string a[] = {"AudioManager.javaupdateAudioPortCache","AudioVolumeGroupChangeHandler.java<init>","AudioMixPort.javabuildConfig","AudioManager.javaupdatePortConfig","AudioManager.javabroadcastDeviceListChange_sync","AudioDevicePort.javabuildConfig","AudioAttributes.java<init>","AudioManager.javainfoListFromPortList","AudioRecord.java<init>","AudioAttributes.java<init>","AudioPortEventHandler.javahandleMessage","CallAudioState.java<init>","AudioManager.javagetDevices","AudioHandle.javaequals","AudioManager.javacalcListDeltas","CameraMetadataNative.java<init>","CameraMetadataNative.javaregisterAllMarshalers","CameraCharacteristics.javaget","CameraMetadataNative.javanativeClose","CameraManager.javagetCameraIdList","CameraMetadataNative.javanativeGetTypeFromTag","CameraManager.javaconnectCameraServiceLocked","CameraManager.javaonTorchStatusChangedLocked","CameraManager.javacompare","ICameraService.javaisHiddenPhysicalCamera","CameraManager.javaonStatusChangedLocked","CameraManager.javaonTorchStatusChanged","CameraCharacteristics.java<init>","ICameraServiceProxy.javaonTransact","CameraManager.javacompare","ICameraServiceProxy.java<init>","ICameraService.javagetCameraCharacteristics","CameraMetadataNative.javanativeReadValues","CameraMetadataNative.javanativeWriteToParcel"};
    //std::string a[] = {"AudioHandle.javaequals","AudioManager.javaupdateAudioPortCache","AudioManager.javabroadcastDeviceListChange_sync","AudioManager.javacalcListDeltas","AudioManager.javaupdatePortConfig","AudioPortEventHandler.javahandleMessage","AudioRecord.java<init>","CameraManager.javacompare","CameraManager.javagetCameraIdList","CameraMetadataNative.javanativeReadValues","CameraMetadataNative.javanativeWriteToParcel","CameraMetadataNative.javaregisterAllMarshalers","ICameraService.javagetCameraCharacteristics","ICameraService.javaisHiddenPhysicalCamera","ICameraServiceProxy.java<init>","CameraManager.javaconnectCameraServiceLocked","CameraManager.javaonTorchStatusChangedLocked","CameraManager.javagetCameraIdList","CameraManager.javaonTorchStatusChanged"};
    std::string a[] = {"AudioVolumeGroupChangeHandler.java<init>","AudioManager.javainfoListFromPortList","AudioDevicePort.javabuildConfig","AudioHandle.javaequals","AudioManager.javabroadcastDeviceListChange_sync","AudioManager.javacalcListDeltas","AudioPortEventHandler.javahandleMessage","AudioManager.javaupdateAudioPortCache","AudioManager.javaupdatePortConfig","AudioRecord.java<init>",\
    "CameraMetadataNative.javanativeClose","CameraManager.javagetCameraIdList","CameraMetadataNative.javanativeReadValues","CameraMetadataNative.javanativeWriteToParcel","CameraMetadataNative.javaregisterAllMarshalers","ICameraService.javaisHiddenPhysicalCamera","ICameraServiceProxy.java<init>","CameraManager.javaconnectCameraServiceLocked","CameraManager.javaonTorchStatusChanged"};
    size_t cnt=sizeof(a)/sizeof(std::string);
    for(int i=0;i<cnt;i++){
        if(function==a[i])
            return 1;
    }
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_SMU_DevSec_CacheScan_init(
        JNIEnv *env,
        jobject thiz,
        jobjectArray dexlist, jobjectArray filenames, jobjectArray func_lists) {
    pthread_mutex_init(&g_lock, NULL);
    jsize size = env->GetArrayLength(dexlist);
    char** func_list; //functions' offsets of every library;
    //get address list
    for(int i=0;i<size;i++)
    {
        jstring obj = (jstring)env->GetObjectArrayElement(dexlist,i);
        std::string dex = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(filenames,i);
        std::string filename = env->GetStringUTFChars(obj,NULL);
        obj = (jstring)env->GetObjectArrayElement(func_lists,i);
        int length=0;
        func_list  = split(',',(char*)env->GetStringUTFChars(obj,NULL), &length);//split a string into function list
        LOGD("Filename %s, Length %d.", filename.c_str(), length);
        //expand addr[];
        sum_length = sum_length + length;
        addr = static_cast<size_t *>(realloc(addr,sum_length*sizeof(size_t)));
        ReadOffset(env,dex,addr,func_list,length,filename,camera_list,audio_list);
    }
    LOGD("Functions Length %d",sum_length);
    LOGD("Camera List: %d, Audio List: %d",length_of_camera_audio[0],length_of_camera_audio[1]);
    threshold = get_threshold();
    threshold = adjust_threshold(threshold, length_of_camera_audio, addr, camera_audio, &finishtrial1);//
    //threshold = 9999;
    camera_pattern = (int*)malloc(sizeof(int)*length_of_camera_audio[0]);
    memset(camera_pattern,0,sizeof(int)*length_of_camera_audio[0]);
    audio_pattern = (int*)malloc(sizeof(int)*length_of_camera_audio[1]);
    memset(audio_pattern,0,sizeof(int)*length_of_camera_audio[1]);
    filter = (int*)malloc(sizeof(int)*(length_of_camera_audio[0]+length_of_camera_audio[1]));
    //disorder the array
    /*
    srand(1);
    for(int i=0;i<length_of_camera_audio[0];i++){
        int t = rand()%length_of_camera_audio[0];
        if(!address_check(camera_list[i])){
            *((size_t*)addr[1]+i) = 0;
        }
        if(!address_check(camera_list[i])){
            *((size_t*)addr[1]+t) = 0;
        }
        swap(((size_t*)addr[1]+i),((size_t*)addr[1]+t));
        swap(camera_list[i],camera_list[t]);
    }
    srand(1);
    for(int i=0;i<length_of_camera_audio[1];i++){
        int t = rand()%length_of_camera_audio[1];
        if(!address_check(audio_list[i])){
            *((size_t*)addr[2]+i) = 0;
        }
        if(!address_check(audio_list[i])){
            *((size_t*)addr[2]+t) = 0;
        }
        //LOGD("KKKKKKKKKKKKKKKKK %s",audio_list[i].c_str());
        swap(((size_t*)addr[2]+i),((size_t*)addr[2]+t));
        swap(audio_list[i],audio_list[t]);
    }
     */

    //LOGD("xxxxxxxxxxxxxxxxxxxxxxxxxxx ");
    std::string temp="";
    int found = 0;
    for(int i=0;i<length_of_camera_audio[0];i++) {
        //if (i == 916)
        //    LOGD("KKKKKKKKKKKKKKKKK %s", camera_list[i].c_str());
        if (temp == camera_list[i] && found == 1) {//only retain one function with the same name
            *((size_t *) addr[1] + i) = 0;
            filter[i] = 1;
            continue;
        }
        if (!address_check(camera_list[i])) {
            *((size_t *) addr[1] + i) = 0;
            filter[i] = 1;
        } else if (*((size_t *) addr[1] + i) != 0) {
            LOGD("Keep %s", camera_list[i].c_str());
            temp = camera_list[i];
            found = 1;
        } else
            found = 0;
    }
    found = 0;
    for(int i=0;i<length_of_camera_audio[1];i++){
        //if (i == 932||i==1746)
        //    LOGD("KKKKKKKKKKKKKKKKK %s", audio_list[i].c_str());
        if(temp==audio_list[i]&&found==1) {
            *((size_t*)addr[2]+i) = 0;
            filter[i] = 1;
            continue;
        }
        if(!address_check(audio_list[i])){
            *((size_t*)addr[2]+i) = 0;
            filter[i+length_of_camera_audio[0]] = 1;
        } else if(*((size_t*)addr[2]+i)!=0){
            LOGD("Keep %s", audio_list[i].c_str());
            temp = audio_list[i];
            found = 1;
        } else
            found = 0;
    }
    flags = (int*)malloc(sum_length*sizeof(int));
    memset(flags,0,sum_length*sizeof(int));
    LOGD("Finish Initializtion");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_SMU_DevSec_SideChannelJob_pause(JNIEnv *env, jobject thiz) {
    // to stop scanning;
    continueRun=0;
}

