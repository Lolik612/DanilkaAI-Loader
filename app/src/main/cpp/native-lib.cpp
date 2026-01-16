#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "DanilkaAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_danilaai_ChatActivity_loadModel(
    JNIEnv* env,
    jobject thiz,
    jstring model_path) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model from: %s", path);
    env->ReleaseStringUTFChars(model_path, path);
    
    // Заглушка - всегда успешно
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_danilaai_ChatActivity_generateResponse(
    JNIEnv* env,
    jobject thiz,
    jstring prompt) {
    
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating response for: %s", prompt_str);
    
    std::string response = "🤖 DANILKA AI отвечает:\n\n";
    response += "Ваш запрос: \"";
    response += prompt_str;
    response += "\"\n\n";
    response += "Это демо-режим. В полной версии здесь будет реальная генерация от GGUF модели!\n\n";
    response += "💡 Совет: Загрузите реальную модель (Llama, DeepSeek, Mistral) в формате GGUF.";
    
    env->ReleaseStringUTFChars(prompt, prompt_str);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_danilaai_ChatActivity_unloadModel(
    JNIEnv* env,
    jobject thiz) {
    LOGI("Model unloaded");
}
