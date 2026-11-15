#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in float lineSide;

out vec4 fragColor;

void main() {
    // Bright core that falls off towards the edges of the expanded line quad.
    // The pipeline blends additively, so the wide low-alpha pass reads as glow.
    float d = clamp(abs(lineSide), 0.0, 1.0);
    float falloff = 1.0 - d * d;
    falloff *= falloff;

    vec4 color = vertexColor * ColorModulator;
    color.a *= falloff;
    if (color.a < 0.002) {
        discard;
    }
    fragColor = color;
}
