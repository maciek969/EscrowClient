#version 330 core

// Fallback path without shader storage buffers: the same 144-byte record is fed as
// per-instance vertex attributes (divisor 1), see the attribute setup in GlBackend.
// Drawn with glDrawArraysInstanced(GL_TRIANGLES, 0, 6, instanceCount).

layout(location = 0) in vec2 aUnit;        // unit quad corner (0..1)
layout(location = 1) in vec4 aCornersTop;  // TL.xy, TR.xy (screen px)
layout(location = 2) in vec4 aCornersBot;  // BR.xy, BL.xy (screen px)
layout(location = 3) in ivec4 aClip;       // clip rect x, y, w, h
layout(location = 4) in vec4 aClipRound;   // clip corner radii
layout(location = 5) in vec4 aRect;        // local rect x, y, w, h
layout(location = 6) in ivec4 aColors;     // packed corner colors TL, TR, BR, BL
layout(location = 7) in vec4 aRadii;       // corner radii
layout(location = 8) in vec4 aUvRect;      // u0, v0, u1, v1
layout(location = 9) in int aFlags;
layout(location = 10) in int aTexSlot;

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

vec4 unpackColor(int bits) {
    uint v = uint(bits);
    return vec4(float(v & 0xFFu), float((v >> 8) & 0xFFu),
                float((v >> 16) & 0xFFu), float((v >> 24) & 0xFFu)) / 255.0;
}

void main() {
    vec2 top = mix(aCornersTop.xy, aCornersTop.zw, aUnit.x);
    vec2 bottom = mix(aCornersBot.zw, aCornersBot.xy, aUnit.x);
    vec2 screen = mix(top, bottom, aUnit.y);

    vClip = aClip;
    vClipRound = aClipRound;
    vRect = aRect;
    vColorTL = unpackColor(aColors.x);
    vColorTR = unpackColor(aColors.y);
    vColorBR = unpackColor(aColors.z);
    vColorBL = unpackColor(aColors.w);
    vRadii = aRadii;
    vUvRect = aUvRect;
    vFlags = aFlags;
    vTexSlot = aTexSlot;

    vLocalPos = aRect.xy + aUnit * aRect.zw;
    vScreenPos = screen;

    vec2 ndc = screen / max(uViewport, vec2(1.0)) * 2.0 - 1.0;
    gl_Position = vec4(ndc.x, -ndc.y, 0.0, 1.0);
}
