#version 150

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

layout(std140) uniform Globals {
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
};

in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 base = vertexColor * ColorModulator;
    if (base.a <= 0.0) {
        discard;
    }

    float sweep = 0.82 + 0.18 * sin((gl_FragCoord.x + gl_FragCoord.y) * 0.10 + GameTime * 18.0);
    float span = max(ScreenSize.x, 1.0);
    float horizon = 0.94 + 0.06 * cos((gl_FragCoord.x / span) * 6.2831853);
    float glow = clamp(sweep * horizon, 0.78, 1.10);

    fragColor = vec4(base.rgb * glow, base.a);
}
