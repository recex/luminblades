#version 150

// Fita arco-íris: matiz percorre a fita; vColor.a carrega o deslocamento de matiz; Tint mistura com a cor da espada.
uniform float Time;
uniform float Progress;
uniform float Tint;

in vec2 uv;
in vec4 vColor;
out vec4 fragColor;

vec3 hsv(float h, float s, float v) {
    vec3 k = clamp(abs(fract(h + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return v * mix(vec3(1.0), k, s);
}

void main() {
    float u = uv.x;
    float v = uv.y * 2.0 - 1.0;
    float head = clamp((Progress * 1.4 - u) / 0.25, 0.0, 1.0);
    float env = 1.0 - smoothstep(0.72, 1.0, Progress);
    float endFade = smoothstep(0.0, 0.08, u) * smoothstep(1.0, 0.92, u);
    float edge = smoothstep(1.0, 0.6, abs(v));
    float hue = fract(u * 0.8 + vColor.a + Time * 0.15);
    vec3 rb = hsv(hue, 0.55, 1.0);
    vec3 col = mix(rb, vColor.rgb, Tint);
    float shine = 0.75 + 0.25 * sin((v * 0.5 + 0.5) * 3.14159);
    fragColor = vec4(col * shine * 1.3, head * env * endFade * edge);
}
