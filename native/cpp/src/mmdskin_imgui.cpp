#include <jni.h>
#include <imgui.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <memory>
#include <new>
#include <string>
#include <vector>

namespace {
struct UiContext {
    ImGuiContext* context;
    int width;
    int height;
    float scale;
    int actions;
    std::vector<unsigned char> fontPixels;
    int fontWidth;
    int fontHeight;

    UiContext(int width, int height, float scale) : context(ImGui::CreateContext()), width(width), height(height), scale(scale), actions(0), fontWidth(0), fontHeight(0) {
        ImGui::SetCurrentContext(this->context);
        ImGuiIO& io = ImGui::GetIO();
        io.IniFilename = nullptr;
        io.LogFilename = nullptr;
        io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;
        ImGui::StyleColorsDark();
        ImGuiStyle& style = ImGui::GetStyle();
        style.WindowRounding = 6.0f;
        style.FrameRounding = 4.0f;
        style.PopupRounding = 4.0f;
        style.ScrollbarRounding = 4.0f;
        style.GrabRounding = 4.0f;
        style.WindowBorderSize = 1.0f;
        style.FrameBorderSize = 1.0f;
        ImVec4* colors = style.Colors;
        colors[ImGuiCol_WindowBg] = ImVec4(0.07f, 0.08f, 0.09f, 0.94f);
        colors[ImGuiCol_ChildBg] = ImVec4(0.10f, 0.11f, 0.12f, 0.92f);
        colors[ImGuiCol_Border] = ImVec4(0.28f, 0.30f, 0.32f, 0.80f);
        colors[ImGuiCol_FrameBg] = ImVec4(0.14f, 0.15f, 0.16f, 1.00f);
        colors[ImGuiCol_FrameBgHovered] = ImVec4(0.18f, 0.22f, 0.24f, 1.00f);
        colors[ImGuiCol_FrameBgActive] = ImVec4(0.18f, 0.31f, 0.34f, 1.00f);
        colors[ImGuiCol_TitleBg] = ImVec4(0.10f, 0.11f, 0.12f, 1.00f);
        colors[ImGuiCol_TitleBgActive] = ImVec4(0.12f, 0.15f, 0.16f, 1.00f);
        colors[ImGuiCol_Button] = ImVec4(0.16f, 0.22f, 0.24f, 1.00f);
        colors[ImGuiCol_ButtonHovered] = ImVec4(0.21f, 0.32f, 0.35f, 1.00f);
        colors[ImGuiCol_ButtonActive] = ImVec4(0.24f, 0.46f, 0.48f, 1.00f);
        colors[ImGuiCol_CheckMark] = ImVec4(0.33f, 0.84f, 0.69f, 1.00f);
        colors[ImGuiCol_SliderGrab] = ImVec4(0.31f, 0.69f, 0.75f, 1.00f);
        colors[ImGuiCol_Header] = ImVec4(0.16f, 0.22f, 0.24f, 1.00f);
        colors[ImGuiCol_HeaderHovered] = ImVec4(0.21f, 0.32f, 0.35f, 1.00f);
        colors[ImGuiCol_HeaderActive] = ImVec4(0.24f, 0.46f, 0.48f, 1.00f);
        rebuildFont();
    }

    ~UiContext() {
        ImGui::SetCurrentContext(this->context);
        ImGui::DestroyContext(this->context);
    }

    void resize(int nextWidth, int nextHeight, float nextScale) {
        this->width = nextWidth;
        this->height = nextHeight;
        this->scale = nextScale;
        ImGui::SetCurrentContext(this->context);
        ImGuiIO& io = ImGui::GetIO();
        io.DisplaySize = ImVec2(static_cast<float>(std::max(1, nextWidth)), static_cast<float>(std::max(1, nextHeight)));
        io.DisplayFramebufferScale = ImVec2(nextScale, nextScale);
    }

    void rebuildFont() {
        ImGui::SetCurrentContext(this->context);
        unsigned char* pixels = nullptr;
        ImGui::GetIO().Fonts->GetTexDataAsRGBA32(&pixels, &this->fontWidth, &this->fontHeight);
        this->fontPixels.assign(pixels, pixels + static_cast<size_t>(this->fontWidth) * static_cast<size_t>(this->fontHeight) * 4);
    }
};

UiContext* ctx(jlong handle) {
    return reinterpret_cast<UiContext*>(static_cast<uintptr_t>(handle));
}

void addKeyModifiers(ImGuiIO& io, int modifiers) {
    io.AddKeyEvent(ImGuiMod_Ctrl, (modifiers & 2) != 0);
    io.AddKeyEvent(ImGuiMod_Shift, (modifiers & 1) != 0);
    io.AddKeyEvent(ImGuiMod_Alt, (modifiers & 4) != 0);
    io.AddKeyEvent(ImGuiMod_Super, (modifiers & 8) != 0);
}

ImGuiKey keyFromGlfw(int key) {
    if (key >= 65 && key <= 90) {
        return static_cast<ImGuiKey>(ImGuiKey_A + key - 65);
    }
    if (key >= 48 && key <= 57) {
        return static_cast<ImGuiKey>(ImGuiKey_0 + key - 48);
    }
    if (key >= 290 && key <= 314) {
        return static_cast<ImGuiKey>(ImGuiKey_F1 + key - 290);
    }
    switch (key) {
        case 256: return ImGuiKey_Escape;
        case 257: return ImGuiKey_Enter;
        case 258: return ImGuiKey_Tab;
        case 259: return ImGuiKey_Backspace;
        case 260: return ImGuiKey_Insert;
        case 261: return ImGuiKey_Delete;
        case 262: return ImGuiKey_RightArrow;
        case 263: return ImGuiKey_LeftArrow;
        case 264: return ImGuiKey_DownArrow;
        case 265: return ImGuiKey_UpArrow;
        case 266: return ImGuiKey_PageUp;
        case 267: return ImGuiKey_PageDown;
        case 268: return ImGuiKey_Home;
        case 269: return ImGuiKey_End;
        case 32: return ImGuiKey_Space;
        case 39: return ImGuiKey_Apostrophe;
        case 44: return ImGuiKey_Comma;
        case 45: return ImGuiKey_Minus;
        case 46: return ImGuiKey_Period;
        case 47: return ImGuiKey_Slash;
        case 59: return ImGuiKey_Semicolon;
        case 61: return ImGuiKey_Equal;
        case 91: return ImGuiKey_LeftBracket;
        case 92: return ImGuiKey_Backslash;
        case 93: return ImGuiKey_RightBracket;
        case 96: return ImGuiKey_GraveAccent;
        default: return ImGuiKey_None;
    }
}

jstring string(JNIEnv* env, const char* value) {
    return env->NewStringUTF(value);
}

jobject vertex(JNIEnv* env, jclass cls, jmethodID ctor, const ImDrawVert& value) {
    uint32_t color = value.col;
    uint32_t argb = ((color >> 24) & 255U) << 24
            | (color & 255U) << 16
            | ((color >> 8) & 255U) << 8
            | ((color >> 16) & 255U);
    return env->NewObject(cls, ctor, value.pos.x, value.pos.y, value.uv.x, value.uv.y, static_cast<jint>(argb));
}

jobject command(JNIEnv* env, jclass cls, jmethodID ctor, int indexOffset, const ImDrawCmd& value) {
    return env->NewObject(
            cls,
            ctor,
            static_cast<jint>(indexOffset),
            static_cast<jint>(value.ElemCount),
            static_cast<jint>(value.VtxOffset),
            static_cast<jint>(std::max(0.0f, value.ClipRect.x)),
            static_cast<jint>(std::max(0.0f, value.ClipRect.y)),
            static_cast<jint>(std::max(value.ClipRect.x, value.ClipRect.z)),
            static_cast<jint>(std::max(value.ClipRect.y, value.ClipRect.w))
    );
}

void panel(UiContext* ui, bool enabled, int modelVertices, int modelIndices, int modelMaterials, int animationCount, const char* backend) {
    ui->actions = 0;
    ImGui::SetNextWindowPos(ImVec2(18.0f, 18.0f), ImGuiCond_Always);
    ImGui::SetNextWindowSize(ImVec2(std::min(420.0f, std::max(320.0f, ui->width - 36.0f)), std::min(330.0f, std::max(260.0f, ui->height - 36.0f))), ImGuiCond_Always);
    ImGui::Begin("MMD Skin", nullptr, ImGuiWindowFlags_NoCollapse | ImGuiWindowFlags_NoSavedSettings);
    ImGui::TextUnformatted("Runtime");
    ImGui::Separator();
    ImGui::Text("Renderer  %s", backend == nullptr ? "UNKNOWN" : backend);
    ImGui::Text("Model     %d vertices / %d indices", modelVertices, modelIndices);
    ImGui::Text("Materials %d", modelMaterials);
    ImGui::Text("Actions   %d", animationCount);
    ImGui::Spacing();
    bool nextEnabled = enabled;
    if (ImGui::Checkbox("Enable MMD model", &nextEnabled) && nextEnabled != enabled) {
        ui->actions |= 1;
    }
    if (ImGui::Button("Refresh model")) {
        ui->actions |= 2;
    }
    ImGui::SameLine();
    if (ImGui::Button("Close")) {
        ui->actions |= 4;
    }
    ImGui::Spacing();
    float gpuValue = modelVertices > 0 ? 1.0f : 0.0f;
    ImGui::ProgressBar(gpuValue, ImVec2(-1.0f, 0.0f), modelVertices > 0 ? "model loaded" : "no model");
    ImGui::End();
}
}

extern "C" JNIEXPORT jint JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_nativeVersion(JNIEnv*, jclass) {
    return 1;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_create(JNIEnv*, jclass, jint width, jint height, jfloat scale) {
    try {
        auto* ui = new UiContext(width, height, scale);
        ui->resize(width, height, scale);
        return static_cast<jlong>(reinterpret_cast<uintptr_t>(ui));
    } catch (...) {
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_destroy(JNIEnv*, jclass, jlong handle) {
    delete ctx(handle);
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_resize(JNIEnv*, jclass, jlong handle, jint width, jint height, jfloat scale) {
    if (auto* ui = ctx(handle)) {
        ui->resize(width, height, scale);
    }
}

extern "C" JNIEXPORT jint JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_frame(JNIEnv* env, jclass, jlong handle, jboolean enabled, jint modelVertices, jint modelIndices, jint modelMaterials, jint animationCount, jstring backendName) {
    auto* ui = ctx(handle);
    if (ui == nullptr) {
        return 0;
    }
    const char* backend = backendName == nullptr ? nullptr : env->GetStringUTFChars(backendName, nullptr);
    ImGui::SetCurrentContext(ui->context);
    ImGuiIO& io = ImGui::GetIO();
    io.DeltaTime = 1.0f / 60.0f;
    io.DisplaySize = ImVec2(static_cast<float>(std::max(1, ui->width)), static_cast<float>(std::max(1, ui->height)));
    io.DisplayFramebufferScale = ImVec2(ui->scale, ui->scale);
    ImGui::NewFrame();
    panel(ui, enabled == JNI_TRUE, modelVertices, modelIndices, modelMaterials, animationCount, backend);
    ImGui::Render();
    if (backend != nullptr) {
        env->ReleaseStringUTFChars(backendName, backend);
    }
    return ui->actions;
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_mouseMove(JNIEnv*, jclass, jlong handle, jfloat x, jfloat y) {
    if (auto* ui = ctx(handle)) {
        ImGui::SetCurrentContext(ui->context);
        ImGui::GetIO().AddMousePosEvent(x, y);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_mouseButton(JNIEnv*, jclass, jlong handle, jint button, jboolean down) {
    if (auto* ui = ctx(handle)) {
        ImGui::SetCurrentContext(ui->context);
        if (button >= 0 && button < 5) {
            ImGui::GetIO().AddMouseButtonEvent(button, down == JNI_TRUE);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_mouseWheel(JNIEnv*, jclass, jlong handle, jfloat x, jfloat y) {
    if (auto* ui = ctx(handle)) {
        ImGui::SetCurrentContext(ui->context);
        ImGui::GetIO().AddMouseWheelEvent(x, y);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_key(JNIEnv*, jclass, jlong handle, jint key, jint, jint modifiers, jboolean down) {
    if (auto* ui = ctx(handle)) {
        ImGui::SetCurrentContext(ui->context);
        ImGuiIO& io = ImGui::GetIO();
        addKeyModifiers(io, modifiers);
        ImGuiKey imguiKey = keyFromGlfw(key);
        if (imguiKey != ImGuiKey_None) {
            io.AddKeyEvent(imguiKey, down == JNI_TRUE);
        }
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_character(JNIEnv*, jclass, jlong handle, jint codepoint) {
    if (auto* ui = ctx(handle)) {
        ImGui::SetCurrentContext(ui->context);
        ImGui::GetIO().AddInputCharacter(static_cast<unsigned int>(codepoint));
    }
}

extern "C" JNIEXPORT jint JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_fontWidth(JNIEnv*, jclass, jlong handle) {
    auto* ui = ctx(handle);
    return ui == nullptr ? 0 : ui->fontWidth;
}

extern "C" JNIEXPORT jint JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_fontHeight(JNIEnv*, jclass, jlong handle) {
    auto* ui = ctx(handle);
    return ui == nullptr ? 0 : ui->fontHeight;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_fontPixelsRgba(JNIEnv* env, jclass, jlong handle) {
    auto* ui = ctx(handle);
    if (ui == nullptr) {
        return env->NewByteArray(0);
    }
    jbyteArray result = env->NewByteArray(static_cast<jsize>(ui->fontPixels.size()));
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(ui->fontPixels.size()), reinterpret_cast<const jbyte*>(ui->fontPixels.data()));
    return result;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_micheanl_model_client_imgui_MMDImGuiNative_drawData(JNIEnv* env, jclass, jlong handle) {
    auto* ui = ctx(handle);
    if (ui == nullptr) {
        return nullptr;
    }
    ImGui::SetCurrentContext(ui->context);
    ImDrawData* drawData = ImGui::GetDrawData();
    if (drawData == nullptr) {
        return nullptr;
    }
    int vertexCount = 0;
    int indexCount = 0;
    int commandCount = 0;
    for (int listIndex = 0; listIndex < drawData->CmdListsCount; listIndex++) {
        const ImDrawList* list = drawData->CmdLists[listIndex];
        vertexCount += list->VtxBuffer.Size;
        indexCount += list->IdxBuffer.Size;
        commandCount += list->CmdBuffer.Size;
    }
    jclass vertexClass = env->FindClass("com/micheanl/model/client/imgui/MMDImGuiDrawData$Vertex");
    jclass commandClass = env->FindClass("com/micheanl/model/client/imgui/MMDImGuiDrawData$Command");
    jclass dataClass = env->FindClass("com/micheanl/model/client/imgui/MMDImGuiDrawData");
    jmethodID vertexCtor = env->GetMethodID(vertexClass, "<init>", "(FFFFI)V");
    jmethodID commandCtor = env->GetMethodID(commandClass, "<init>", "(IIIIIII)V");
    jmethodID dataCtor = env->GetMethodID(dataClass, "<init>", "(IIII[Lcom/micheanl/model/client/imgui/MMDImGuiDrawData$Vertex;[I[Lcom/micheanl/model/client/imgui/MMDImGuiDrawData$Command;)V");
    jobjectArray vertices = env->NewObjectArray(vertexCount, vertexClass, nullptr);
    jintArray indices = env->NewIntArray(indexCount);
    jobjectArray commands = env->NewObjectArray(commandCount, commandClass, nullptr);
    std::vector<jint> indexBuffer(static_cast<size_t>(indexCount));
    int vertexOffset = 0;
    int indexOffset = 0;
    int commandOffset = 0;
    for (int listIndex = 0; listIndex < drawData->CmdListsCount; listIndex++) {
        const ImDrawList* list = drawData->CmdLists[listIndex];
        for (int vertexIndex = 0; vertexIndex < list->VtxBuffer.Size; vertexIndex++) {
            env->SetObjectArrayElement(vertices, vertexOffset + vertexIndex, vertex(env, vertexClass, vertexCtor, list->VtxBuffer[vertexIndex]));
        }
        for (int i = 0; i < list->IdxBuffer.Size; i++) {
            indexBuffer[static_cast<size_t>(indexOffset + i)] = static_cast<jint>(list->IdxBuffer[i]);
        }
        for (int commandIndex = 0; commandIndex < list->CmdBuffer.Size; commandIndex++) {
            ImDrawCmd cmd = list->CmdBuffer[commandIndex];
            cmd.VtxOffset += vertexOffset;
            env->SetObjectArrayElement(commands, commandOffset++, command(env, commandClass, commandCtor, indexOffset + static_cast<int>(cmd.IdxOffset), cmd));
        }
        vertexOffset += list->VtxBuffer.Size;
        indexOffset += list->IdxBuffer.Size;
    }
    env->SetIntArrayRegion(indices, 0, indexCount, indexBuffer.data());
    return env->NewObject(dataClass, dataCtor, ui->fontWidth, ui->fontHeight, ui->width, ui->height, vertices, indices, commands);
}
