package elexis.client.utils.render.main.oldrender.system.sys2d;

import com.mojang.blaze3d.opengl.GlStateManager;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@Getter
public class Shader {
    private final int id;

    public Shader() {
        try {
            this.id = GL30.glCreateProgram();

            int vertex = this.create(GL30.GL_VERTEX_SHADER, """
                    #version 330 core

                    layout (location = 0) in vec3 position;
                    layout (location = 1) in vec2 VtexCoords;
                    layout (location = 2) in vec4 Vcolor;
                    layout (location = 3) in vec2 Vsize;
                    layout (location = 4) in vec4 Vround;
                    layout (location = 5) in float Vshader;
                    layout (location = 6) in vec2 Vsmooth;
                    layout (location = 7) in float Vthickness;
                    layout (location = 8) in float VmsdfRange;
                    layout (location = 9) in float VblurRadius;
                    layout (location = 10) in vec4 Vscissor;
                    layout (location = 11) in vec2 VfragCoord;
                    layout (location = 12) in vec2 Vuv;
                    layout (location = 13) in float Vhatch;
                    layout (location = 14) in float VuseCircle;

                    uniform mat4 ProjMat, ModelViewMat;

                    out vec3 pos;
                    out vec2 texCoords;
                    out vec4 vertexColor;
                    out vec2 size;
                    out vec4 u_round;
                    out float u_shaderId;
                    out vec2 smoothness;
                    out float thickness;
                    out float texid;
                    out float msdfRange;
                    out float blurRadius;
                    out vec4 scissor;
                    out vec2 fragCoord;
                    out vec2 uv;
                    out float hatch;
                    out float useCircle;

                    void main() {
                        gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
                        pos = position;
                        texCoords = VtexCoords;
                        vertexColor = Vcolor;
                        size = Vsize;
                        u_round = Vround;
                        u_shaderId = Vshader;
                        smoothness = Vsmooth;
                        thickness = Vthickness;
                        texid = 0;
                        msdfRange = VmsdfRange;
                        blurRadius = VblurRadius;
                        scissor = Vscissor;
                        fragCoord = VfragCoord;
                        uv = Vuv;
                        hatch = Vhatch;
                        useCircle = VuseCircle;
                    }
                    """);

            int fragment = this.create(GL30.GL_FRAGMENT_SHADER, """
                    #version 150

                    float roundSDF(vec2 p, vec2 b, float r) {
                        return length(max(abs(p) - b, 0.0)) - r;
                    }
                    float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {
                        vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;
                        vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;
                        vec2 coords = abs(vec_1) - vec_2 + vec_4.x;
                        return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;
                    }
                    float rdist(vec2 pos, vec2 size, vec4 radius) {
                        radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
                        radius.x = (pos.y > 0.0) ? radius.x : radius.y;

                        vec2 v = abs(pos) - size + radius.x;
                        return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
                    }

                    float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
                        vec2 center = size * 0.5;
                        float dist = rdist(center - (coord * size), center - 1.0, radius);
                        return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
                    }
                    float median(vec3 color) {
                        return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
                    }
                    vec3 sampleLinearMSDF(sampler2D tex, vec2 uv) {
                        vec2 texSize = vec2(textureSize(tex, 0));
                        vec2 pixel = uv * texSize;

                        vec2 base = floor(pixel - 0.5) + 0.5;
                        vec2 f = fract(pixel - 0.5);

                        vec3 c00 = texture(tex, (base + vec2(0.0, 0.0)) / texSize).rgb;
                        vec3 c10 = texture(tex, (base + vec2(1.0, 0.0)) / texSize).rgb;
                        vec3 c01 = texture(tex, (base + vec2(0.0, 1.0)) / texSize).rgb;
                        vec3 c11 = texture(tex, (base + vec2(1.0, 1.0)) / texSize).rgb;

                        vec3 cx0 = mix(c00, c10, f.x);
                        vec3 cx1 = mix(c01, c11, f.x);

                        return mix(cx0, cx1, f.y);
                    }
                    vec3 hsv2rgb(vec3 c) {
                        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                    }

                    uniform sampler2D tex;
                    uniform vec2 iResolution;

                    in vec3 pos;
                    in vec2 texCoords;
                    in vec4 vertexColor;
                    in vec2 size;
                    in vec4 u_round;
                    in float u_shaderId;
                    in vec2 smoothness;
                    in float thickness;
                    in float texid;
                    in float msdfRange;
                    in float blurRadius;
                    in vec4 scissor;
                    in vec2 fragCoord;
                    in vec2 uv;
                    in float hatch;
                    in float useCircle;

                    out vec4 outColor;

                    const float blurAmount = 1.0;
                    const float edgeWarpIntensity = 1.0;
                    const vec3 tintColor = vec3(0.0, 0.0, 0.0);
                    const float DPI = 6.28318530718;
                    const float STEP = DPI / 16.0;

                    void main() {
                        if (useCircle != 0.0) {
                            float d1 = distance(texCoords, vec2(-0.3 + useCircle, 0.2)) - useCircle;
                            float d2 = distance(texCoords, vec2(1.2, 0.9)) - useCircle;
                            if (d1 < 0.1 || d2 < 0.05) {
                                discard;
                            }
                        }
                        if (gl_FragCoord.x < scissor.x || gl_FragCoord.x > scissor.x + scissor.z || gl_FragCoord.y < scissor.y || gl_FragCoord.y > scissor.y + scissor.w) {
                            discard;
                        }
                        int shaderId = int(round(u_shaderId));

                        if (shaderId == 0) {
                            float alpha = ralpha(size, texCoords, u_round, smoothness.x);
                            outColor = vec4(vertexColor.rgb, vertexColor.a * alpha);

                        } else if (shaderId == 1) {
                            vec2 center = size * 0.5;
                            float dist = rdist(center - (texCoords.xy * size), center - 1.0, u_round);
                            float alpha = smoothstep(1.0 - thickness - smoothness.x - smoothness.y,
                            1.0 - thickness - smoothness.y, dist);
                            alpha *= 1.0 - smoothstep(1.0 - smoothness.y, 1.0, dist);
                            outColor = vec4(vertexColor.rgb, vertexColor.a * alpha);

                        } else if (shaderId == 2) {
                            vec2 resolution = textureSize(tex, 0);
                            vec2 multiplier = 10 / resolution;

                            vec2 refractedCoord = texCoords;

                            vec3 average = texture(tex, refractedCoord).rgb;
                            for (float d = 0.0; d < 32; d += 1) {
                                for (float i = 0.2; i <= 1.0; i += 0.2) {
                                    average += texture(tex, refractedCoord + vec2(cos(d), sin(d)) * multiplier * i).rgb;
                                }
                            }
                            average /= 160.0;

                            vec4 color = vec4(average, 1.0) * vertexColor;
                            color.a *= ralpha(size, uv, u_round, smoothness.x);

                            if (color.a == 0.0) {
                                discard;
                            }
                            outColor = color;
                        } else if (shaderId == 3) {
                            vec4 t = texture(tex, texCoords);
                            t.a *= ralpha(size, uv, u_round, smoothness.x);
                            if (t.a == 0.0) {
                                discard;
                            }
                            outColor = vec4(vertexColor.rgb * t.rgb, t.a * vertexColor.a);
                        } else if (shaderId == 4) {
                            vec3 msdfSample = sampleLinearMSDF(tex, texCoords);
                            float dist = median(msdfSample) - 0.48;

                            vec2 h = vec2(dFdx(texCoords.x), dFdy(texCoords.y)) * textureSize(tex, 0);
                            float pixels = msdfRange * inversesqrt(h.x * h.x + h.y * h.y);

                            float alpha = smoothstep(-smoothness.x, smoothness.x, dist * pixels);
                            outColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
                        } else if (shaderId == 5) {
                            vec2 center = vec2(0.5);
                            float dist = clamp(0.5 - distance(center, texCoords), 0, 1);
                            outColor = vec4(vertexColor.rgb, vertexColor.a * pow(dist, 2) * smoothness.x);
                        } else if (shaderId == 6) {
                            float alpha = ralpha(size, texCoords, u_round, smoothness.x);
                            outColor = vec4(hsv2rgb(vec3(texCoords.x, 1.0, 1.0)), alpha * vertexColor.a);
                        } else if (shaderId == 7) {
                            vec4 cl = vec4(vertexColor.rgb, 1.0);
                            vec4 c = mix(mix(vec4(1.0), cl, uv.x), vec4(0.0), uv.y);
                            float alpha = ralpha(size, texCoords, u_round, smoothness.x);
                            outColor = vec4(c.rgb, vertexColor.a * alpha);
                        }

                        if (hatch > 0.0) {
                            float x = mod(texCoords.x * (size.x / size.y) - 0.05, hatch);
                            if (x < 0.05) x = 0.0;
                            else x = 1.0;

                            float y = mod(texCoords.y - 0.05, hatch);
                            if (y < 0.05) y = 0.0;
                            else y = 1.0;
                            outColor.a *= x * y;
                        }
                    }
                    """);

            GL30.glAttachShader(this.id, vertex);
            GL30.glAttachShader(this.id, fragment);
            GL20.glLinkProgram(this.id);

            int linked = GL20.glGetProgrami(this.id, GL20.GL_LINK_STATUS);
            if (linked == 0) {
                String log = GL20.glGetProgramInfoLog(this.id);
                throw new RuntimeException("Shader link failed:\n" + log);
            }

            GL30.glDeleteShader(vertex);
            GL30.glDeleteShader(fragment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int create(int type, String source) {
        int shaderId = GL30.glCreateShader(type);

        GL30.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        int compiled = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            String log = GL20.glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Shader compile failed:\n" + log + "\nSource:\n" + source);
        }

        return shaderId;
    }

    public void bind() {
        GlStateManager._glUseProgram(this.id);
    }

    public void uploadMatrix(Matrix4f matrix4f, Matrix4f matrix4f2) {
        float[] proj = new float[16];
        matrix4f.get(proj);
        GL30.glUniformMatrix4fv(GL20.glGetUniformLocation(this.id, "ProjMat"), false, proj);

        float[] modelView = new float[16];
        matrix4f2.get(modelView);
        GL30.glUniformMatrix4fv(GL20.glGetUniformLocation(this.id, "ModelViewMat"), false, modelView);
    }

    public void setUniform1i(String name, int value) {
        GL30.glUniform1i(GL30.glGetUniformLocation(this.id, name), value);
    }
}
