#version 150

// Faixa cósmica: nebulosa (fbm) + estrelas cintilantes + bordas brancas brilhantes.
uniform float Time;
uniform float Progress;

in vec2 uv;
in vec4 vColor;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

void main() {
    float u = uv.x;
    float v = uv.y * 2.0 - 1.0;
    if (abs(v) > 1.0) discard;

    float head = clamp((Progress * 1.35 - u) / 0.30, 0.0, 1.0);
    float endFade = smoothstep(0.0, 0.05, u) * smoothstep(1.0, 0.95, u);
    float env = 1.0 - smoothstep(0.72, 1.0, Progress);

    vec2 q = vec2(u * 7.0 - Time * 0.18, v * 1.5);
    float n1 = fbm(q * 2.0 + vec2(0.0, Time * 0.05));
    float n2 = fbm(q * 4.6 - 3.7);
    vec3 deep = vec3(0.09, 0.04, 0.30);
    vec3 mid = vColor.rgb * 0.85;
    vec3 light = vec3(0.78, 0.68, 1.0);
    vec3 neb = mix(deep, mid, smoothstep(0.25, 0.7, n1));
    neb = mix(neb, light, smoothstep(0.55, 0.9, n2) * 0.85);
    float band = exp(-v * v * 2.4);
    neb *= 0.45 + 1.0 * band;

    vec2 sg = vec2(u * 70.0, v * 9.0);
    vec2 id = floor(sg);
    vec2 f = fract(sg) - 0.5;
    float h = hash(id);
    float star = step(0.93, h) * smoothstep(0.28, 0.0, length(f)) * (0.55 + 0.45 * sin(Time * 3.0 + h * 40.0));
    neb += vec3(1.0) * star * 1.6;

    float edge = smoothstep(0.84, 0.96, abs(v));
    vec3 col = neb + vec3(0.85, 0.93, 1.0) * edge * 2.2;
    float a = head * endFade * env * (0.8 + 0.2 * band) * vColor.a;
    fragColor = vec4(col, a);
}
