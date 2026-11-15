#version 330 core

in vec2 vUv;
layout(location = 0) out vec4 fragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uOffset;

void main() {
    vec2 o = uTexelSize * 0.5 * max(uOffset, 0.5);

    vec4 sum = texture(uSource, vUv + vec2(-o.x * 2.0, 0.0));
    sum += texture(uSource, vUv + vec2(-o.x, o.y)) * 2.0;
    sum += texture(uSource, vUv + vec2(0.0, o.y * 2.0));
    sum += texture(uSource, vUv + vec2(o.x, o.y)) * 2.0;
    sum += texture(uSource, vUv + vec2(o.x * 2.0, 0.0));
    sum += texture(uSource, vUv + vec2(o.x, -o.y)) * 2.0;
    sum += texture(uSource, vUv + vec2(0.0, -o.y * 2.0));
    sum += texture(uSource, vUv + vec2(-o.x, -o.y)) * 2.0;

    fragColor = sum / 12.0;
}
