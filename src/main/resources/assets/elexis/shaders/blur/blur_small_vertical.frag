#version 330 core

in vec2 vUv;
layout(location = 0) out vec4 fragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uRadius;

const int MAX_TAPS = 8;

void main() {
    float radius = max(uRadius, 0.5);
    float sigma = max(radius * 0.5, 0.5);
    float denom = 2.0 * sigma * sigma;
    float taps = min(ceil(radius * 1.5), float(MAX_TAPS));
    vec2 stepUv = vec2(0.0, uTexelSize.y);

    vec4 sum = texture(uSource, vUv);
    float weightSum = 1.0;
    for (int i = 1; i <= MAX_TAPS; i++) {
        float fi = float(i);
        if (fi > taps) {
            break;
        }
        float w = exp(-(fi * fi) / denom);
        sum += (texture(uSource, vUv + stepUv * fi) + texture(uSource, vUv - stepUv * fi)) * w;
        weightSum += 2.0 * w;
    }

    fragColor = sum / weightSum;
}
