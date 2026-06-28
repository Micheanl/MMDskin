#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

#define MMD_MAX_BONES 256

layout(std140) uniform MmdBones {
    mat4 BoneMatrices[MMD_MAX_BONES];
};

layout(std140) uniform MmdDraw {
    vec4 DrawAlpha;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;

void main() {
    int b0 = UV1.x & 65535;
    int b1 = UV1.y & 65535;
    int b2 = UV2.x & 65535;
    int b3 = UV2.y & 65535;
    vec3 weights = Normal * 0.5 + 0.5;
    float w0 = clamp(weights.x, 0.0, 1.0);
    float w1 = clamp(weights.y, 0.0, 1.0);
    float w2 = clamp(weights.z, 0.0, 1.0);
    float w3 = max(0.0, 1.0 - w0 - w1 - w2);
    vec4 source = vec4(Position, 1.0);
    vec4 skinned =
        BoneMatrices[b0] * source * w0 +
        BoneMatrices[b1] * source * w1 +
        BoneMatrices[b2] * source * w2 +
        BoneMatrices[b3] * source * w3;

    gl_Position = ProjMat * ModelViewMat * skinned;
    vertexColor = vec4(Color.rgb, Color.a * DrawAlpha.x);
}
