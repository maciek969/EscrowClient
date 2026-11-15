#version 430 core

// SSBO path: one 144-byte record per instance (36 words), packed by GlBackend.writeInstanceEx.
// Drawn with glDrawArrays(GL_TRIANGLES, 0, instanceCount * 6), so each instance owns 6 vertices.
//
// Word layout:
//   0..7   quad corners TL, TR, BR, BL (screen px, float)
//   8..11  clip rect x, y, w, h (int)
//   12..15 clip corner radii tl, tr, br, bl (float)
//   16..19 local rect x, y, w, h (float)
//   20..23 corner colors TL, TR, BR, BL (packed for unpackUnorm4x8)
//   24..27 corner radii tl, tr, br, bl (float)
//   28..31 uv u0, v0, u1, v1 (float)
//   32     flags, 33 texture slot, 34..35 reserved (int)

const int WORDS = 36;

layout(std430, binding = 0) readonly buffer Instances {
    uint data[];
};

uniform vec2 uViewport;

flat out vec4 vRect;
flat out vec4 vRadii;
flat out vec4 vUvRect;
flat out vec4 vColorTL;
flat out vec4 vColorTR;
flat out vec4 vColorBR;
flat out vec4 vColorBL;
flat out vec4 vClipRound;
flat out ivec4 vClip;
flat out int vFlags;
flat out int vTexSlot;

out vec2 vLocalPos;
out vec2 vScreenPos;

const int CORNERS[6] = int[](0, 1, 2, 0, 2, 3);
const vec2 UNIT[4] = vec2[](vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0));

float wf(int base, int i) {
    return uintBitsToFloat(data[base + i]);
}

int wi(int base, int i) {
    return int(data[base + i]);
}

vec4 wf4(int base, int i) {
    return vec4(wf(base, i), wf(base, i + 1), wf(base, i + 2), wf(base, i + 3));
}

void main() {
    int instance = gl_VertexID / 6;
    int corner = CORNERS[gl_VertexID % 6];
    int base = instance * WORDS;

    vec2 screen = vec2(wf(base, corner * 2), wf(base, corner * 2 + 1));

    vClip = ivec4(wi(base, 8), wi(base, 9), wi(base, 10), wi(base, 11));
    vClipRound = wf4(base, 12);
    vRect = wf4(base, 16);
    vColorTL = unpackUnorm4x8(data[base + 20]);
    vColorTR = unpackUnorm4x8(data[base + 21]);
    vColorBR = unpackUnorm4x8(data[base + 22]);
    vColorBL = unpackUnorm4x8(data[base + 23]);
    vRadii = wf4(base, 24);
    vUvRect = wf4(base, 28);
    vFlags = wi(base, 32);
    vTexSlot = wi(base, 33);

    vLocalPos = vRect.xy + UNIT[corner] * vRect.zw;
    vScreenPos = screen;

    vec2 ndc = screen / max(uViewport, vec2(1.0)) * 2.0 - 1.0;
    gl_Position = vec4(ndc.x, -ndc.y, 0.0, 1.0);
}
