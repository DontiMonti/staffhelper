#version 150

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
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

    float outside = max(dist, 0.0);
    float glowRadius = clamp(min(panelSize.x, panelSize.y) * 0.065, 1.25, 4.0);
    float sigma = max(glowRadius, 1e-3);
    float glow = exp(-(outside * outside) / (2.0 * sigma * sigma));
    glow = pow(glow, 1.65);
    float outsideMask = smoothstep(-0.10, 0.85, dist);
    float alpha = base.a * glow * outsideMask * 0.70;

    if (alpha <= 0.003) {
        discard;
    }

    fragColor = vec4(base.rgb, alpha);
}
