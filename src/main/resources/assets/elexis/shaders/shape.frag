#version 330 core

// Shared fragment stage for the instanced 2D backend (see GlBackend).
// Instance fields are decoded in the vertex stage and handed over as varyings.
// Blending is premultiplied: glBlendFuncSeparate(ONE, ONE_MINUS_SRC_ALPHA, ...),
// so every path must output a premultiplied color.

#define MAX_TEXTURE_SLOTS 16

#define TYPE_RECT_FILL    0
#define TYPE_RECT_OUTLINE 1
#define TYPE_CIRCLE       2
#define TYPE_TEXTURED     3

#define FLAG_TEXTURE_RGBA        (1 << 2)
#define FLAG_FORCE_OPAQUE        (1 << 3)
#define FLAG_TEXTURE_MSDF        (1 << 4)
#define FLAG_TEXTURE_SCREENSPACE (1 << 5)
#define FLAG_TEXTURE_PREMULT     (1 << 6)
#define FLAG_SHADOW              (1 << 26)

uniform sampler2D uTextures[MAX_TEXTURE_SLOTS];

flat in vec4 vRect;       // x, y, w, h (local pixel space)
flat in vec4 vRadii;      // per-corner radius (tl, tr, br, bl); for MSDF .x holds pxRange
flat in vec4 vUvRect;     // u0, v0, u1, v1 (reused for shadow / screen-space transform)
flat in vec4 vColorTL;
flat in vec4 vColorTR;
flat in vec4 vColorBR;
flat in vec4 vColorBL;
flat in vec4 vClipRound;  // clip rounded corners (tl, tr, br, bl)
flat in ivec4 vClip;      // clip rect: x, y, w, h (pixel space)
flat in int vFlags;
flat in int vTexSlot;

in vec2 vLocalPos;        // interpolated local pixel position
in vec2 vScreenPos;       // interpolated on-screen pixel position (top-left origin)

layout(location = 0) out vec4 fragColor;

const float TWO_PI = 6.28318530718;

float median3(vec3 v) {
    return max(min(v.x, v.y), min(max(v.x, v.y), v.z));
}

// Signed distance to a rounded box; negative inside. Radius picked per quadrant.
float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 radius) {
    vec2 rxy = (p.x > 0.0) ? radius.yz : radius.xw; // right: tr,br ; left: tl,bl
    float r = (p.y > 0.0) ? rxy.y : rxy.x;          // bottom picks .y, top picks .x
    r = min(r, min(halfSize.x, halfSize.y));
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

float coverageFromSDF(float dist) {
    float aa = max(fwidth(dist), 1.0e-4);
    return clamp(0.5 - dist / aa, 0.0, 1.0);
}

vec4 bilinearColor(vec2 n) {
    vec4 top = mix(vColorTL, vColorTR, n.x);
    vec4 bottom = mix(vColorBL, vColorBR, n.x);
    return mix(top, bottom, n.y);
}

float clipCoverage() {
    vec2 size = vec2(vClip.zw);
    if (size.x <= 0.0 || size.y <= 0.0) {
        return 1.0;
    }
    vec2 center = vec2(vClip.xy) + size * 0.5;
    float dist = roundedBoxSDF(vScreenPos - center, size * 0.5, vClipRound);
    return coverageFromSDF(dist);
}

// GLSL 3.30 only allows constant indices into sampler arrays, so each slot is spelled out.
vec4 sampleSlot(vec2 uv) {
    switch (vTexSlot) {
        case 0: return texture(uTextures[0], uv);
        case 1: return texture(uTextures[1], uv);
        case 2: return texture(uTextures[2], uv);
        case 3: return texture(uTextures[3], uv);
        case 4: return texture(uTextures[4], uv);
        case 5: return texture(uTextures[5], uv);
        case 6: return texture(uTextures[6], uv);
        case 7: return texture(uTextures[7], uv);
        case 8: return texture(uTextures[8], uv);
        case 9: return texture(uTextures[9], uv);
        case 10: return texture(uTextures[10], uv);
        case 11: return texture(uTextures[11], uv);
        case 12: return texture(uTextures[12], uv);
        case 13: return texture(uTextures[13], uv);
        case 14: return texture(uTextures[14], uv);
        case 15: return texture(uTextures[15], uv);
    }
    return vec4(0.0);
}

ivec2 slotSize() {
    switch (vTexSlot) {
        case 0: return textureSize(uTextures[0], 0);
        case 1: return textureSize(uTextures[1], 0);
        case 2: return textureSize(uTextures[2], 0);
        case 3: return textureSize(uTextures[3], 0);
        case 4: return textureSize(uTextures[4], 0);
        case 5: return textureSize(uTextures[5], 0);
        case 6: return textureSize(uTextures[6], 0);
        case 7: return textureSize(uTextures[7], 0);
        case 8: return textureSize(uTextures[8], 0);
        case 9: return textureSize(uTextures[9], 0);
        case 10: return textureSize(uTextures[10], 0);
        case 11: return textureSize(uTextures[11], 0);
        case 12: return textureSize(uTextures[12], 0);
        case 13: return textureSize(uTextures[13], 0);
        case 14: return textureSize(uTextures[14], 0);
        case 15: return textureSize(uTextures[15], 0);
    }
    return ivec2(1);
}

void main() {
    int type = vFlags & 3;

    vec2 center = vRect.xy + vRect.zw * 0.5;
    vec2 halfSize = vRect.zw * 0.5;
    vec2 p = vLocalPos - center;
    vec2 n = clamp((vLocalPos - vRect.xy) / max(vRect.zw, vec2(1.0e-4)), 0.0, 1.0);

    float clip = clipCoverage();
    if (clip <= 0.0) {
        discard;
    }

    vec4 result = vec4(0.0);

    if ((vFlags & FLAG_SHADOW) != 0) {
        // vUvRect = (origW, origH, blur, spread); vRect is the expanded quad.
        vec2 innerHalf = vUvRect.xy * 0.5 + vec2(vUvRect.w);
        float blur = max(vUvRect.z, 1.0e-3);
        float dist = roundedBoxSDF(p, innerHalf, vRadii);
        float shadow = 1.0 - smoothstep(-blur, blur, dist);
        vec4 c = bilinearColor(n); // premultiplied rgba
        result = c * shadow;
    } else if (type == TYPE_RECT_FILL) {
        float dist = roundedBoxSDF(p, halfSize, vRadii);
        float cov = coverageFromSDF(dist);
        vec4 c = bilinearColor(n);
        float a = c.a * cov;
        result = vec4(c.rgb * a, a);
    } else if (type == TYPE_RECT_OUTLINE) {
        float thickness = float((vFlags >> 2) & 0xFF);
        float dist = roundedBoxSDF(p, halfSize, vRadii);
        float outer = coverageFromSDF(dist);
        float inner = coverageFromSDF(dist + max(thickness, 1.0));
        float cov = clamp(outer - inner, 0.0, 1.0);
        vec4 c = bilinearColor(n);
        float a = c.a * cov;
        result = vec4(c.rgb * a, a);
    } else if (type == TYPE_CIRCLE) {
        float radius = min(halfSize.x, halfSize.y);
        float dist = length(p) - radius;
        float cov = coverageFromSDF(dist);

        int startByte = (vFlags >> 10) & 0xFF;
        int pctByte = (vFlags >> 18) & 0xFF;
        float pct = float(pctByte) / 255.0;
        if (pct < 0.999) {
            float start = float(startByte) / 255.0;
            float ang = atan(p.x, -p.y) / TWO_PI; // 0 at top, clockwise
            ang = fract(ang - start);
            if (ang > pct) {
                cov = 0.0;
            }
        }
        vec4 c = bilinearColor(n);
        float a = c.a * cov;
        result = vec4(c.rgb * a, a);
    } else { // TYPE_TEXTURED
        vec2 uv;
        if ((vFlags & FLAG_TEXTURE_SCREENSPACE) != 0) {
            uv = vScreenPos * vUvRect.xy + vUvRect.zw;
        } else {
            uv = mix(vUvRect.xy, vUvRect.zw, n);
        }

        float dist = roundedBoxSDF(p, halfSize, vRadii);
        float rectCov = coverageFromSDF(dist);
        vec4 tint = bilinearColor(n);

        if ((vFlags & FLAG_TEXTURE_MSDF) != 0) {
            vec4 texel = sampleSlot(uv);
            float sd = median3(texel.rgb);
            float pxRange = max(vRadii.x, 1.0e-3);
            vec2 unitRange = vec2(pxRange) / vec2(max(slotSize(), ivec2(1)));
            vec2 screenTexSize = vec2(1.0) / max(fwidth(uv), vec2(1.0e-6));
            float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
            float cov = clamp(screenPxRange * (sd - 0.5) + 0.5, 0.0, 1.0);
            float a = tint.a * cov * rectCov;
            result = vec4(tint.rgb * a, a);
        } else {
            vec4 texel = sampleSlot(uv);
            if ((vFlags & FLAG_FORCE_OPAQUE) != 0) {
                texel.a = 1.0;
            }
            if ((vFlags & FLAG_TEXTURE_PREMULT) != 0) {
                float a = texel.a * tint.a * rectCov;
                result = vec4(texel.rgb * tint.a * rectCov, a);
            } else {
                float a = texel.a * tint.a * rectCov;
                result = vec4(texel.rgb * tint.rgb * a, a);
            }
        }
    }

    result *= clip;
    if (result.a <= 0.0 && result.rgb == vec3(0.0)) {
        discard;
    }
    fragColor = result;
}
