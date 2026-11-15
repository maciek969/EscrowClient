#version 330 core

in vec2 vUv;
layout(location = 0) out vec4 fragColor;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uOffset;

void main() {
    vec2 o = uTexelSize * 0.5 * max(uOffset, 0.5);

    vec4 sum = texture(uSource, vUv) * 4.0;
    sum += texture(uSource, vUv - o);
    sum += texture(uSource, vUv + o);
    sum += texture(uSource, vUv + vec2(o.x, -o.y));
    sum += texture(uSource, vUv - vec2(o.x, -o.y));

    fragColor = sum * 0.125;
}
