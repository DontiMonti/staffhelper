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

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float sdRoundedRect(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - (halfSize - vec2(radius));
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec4 base = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (base.a <= 0.0) {
        discard;
    }

    vec2 uvFw = max(fwidth(texCoord0), vec2(1e-5));
    vec2 panelSize = vec2(1.0 / uvFw.x, 1.0 / uvFw.y);
    vec2 p = (texCoord0 - vec2(0.5)) * panelSize;
    float radius = clamp(min(panelSize.x, panelSize.y) * 0.22, 2.5, 14.0);
    float dist = sdRoundedRect(p, panelSize * 0.5, radius);

    float aa = max(fwidth(dist) * 0.70, 0.45);
    float fillMask = smoothstep(aa, -aa, dist);
    if (fillMask <= 0.001) {
        discard;
    }

    float localY = clamp(texCoord0.y, 0.0, 1.0);
    float topLift = 1.0 - localY;
    float shade = 0.93 + (topLift * 0.07);

    fragColor = vec4(base.rgb * shade, base.a * fillMask);
}
