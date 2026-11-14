#version 150

// Círculo mágico procedural: anéis, duas faixas de glifos girando em sentidos opostos, hexagrama e marcas.
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

float sdSeg(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

vec3 hsv(float h, float s, float v) {
    vec3 k = clamp(abs(fract(h + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return v * mix(vec3(1.0), k, s);
}

mat2 rot(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, -s, s, c);
}

float glyphRing(vec2 p, float r0, float r1, float cells, out float cid) {
    float r = length(p);
    float a = atan(p.y, p.x) / 6.2831853 * cells;
    float id = floor(a);
    cid = id;
    float f = fract(a) - 0.5;
    float t = (r - r0) / (r1 - r0) - 0.5;
    if (t < -0.5 || t > 0.5) return 0.0;
    vec2 lp = vec2(f * 1.2, t);
    float d = 10.0;
    for (int k = 0; k < 3; k++) {
        float fk = float(k);
        vec2 a1 = vec2(hash(vec2(id, fk)) - 0.5, hash(vec2(id, fk + 7.0)) - 0.5) * 0.8;
        vec2 b1 = vec2(hash(vec2(id, fk + 13.0)) - 0.5, hash(vec2(id, fk + 19.0)) - 0.5) * 0.8;
        d = min(d, sdSeg(lp, a1, b1));
    }
    return smoothstep(0.07, 0.025, d);
}

void main() {
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.0) discard;

    float env = smoothstep(0.0, 0.2, Progress) * (1.0 - smoothstep(0.8, 1.0, Progress));
    float reach = smoothstep(0.0, 0.35, Progress);
    float mask = smoothstep(reach + 0.001, reach - 0.06, r);
    float t = Time;

    float ringA = smoothstep(0.012, 0.0, abs(r - 0.96));
    ringA += smoothstep(0.008, 0.0, abs(r - 0.84));
    ringA += smoothstep(0.008, 0.0, abs(r - 0.58));
    ringA += smoothstep(0.010, 0.0, abs(r - 0.30));

    float c1;
    float c2;
    float g1 = glyphRing(rot(t * 0.25) * p, 0.62, 0.82, 28.0, c1);
    float g2 = glyphRing(rot(-t * 0.40) * p, 0.34, 0.56, 14.0, c2);

    vec2 hp = rot(t * 0.2) * p;
    float hd = 10.0;
    for (int i = 0; i < 6; i++) {
        float a0 = float(i) * 1.0471976;
        float a1 = float(i + 2) * 1.0471976;
        hd = min(hd, sdSeg(hp, 0.28 * vec2(cos(a0), sin(a0)), 0.28 * vec2(cos(a1), sin(a1))));
    }
    float hex = smoothstep(0.012, 0.0, hd);

    vec2 tp = rot(t * 0.1) * p;
    float tick = step(0.9, fract(atan(tp.y, tp.x) / 6.2831853 * 72.0)) * smoothstep(0.03, 0.0, abs(r - 0.9));

    vec3 tint = vColor.rgb;
    vec3 colG1 = mix(tint, hsv(fract(c1 * 0.07 + t * 0.05), 0.7, 1.0), 0.45);
    vec3 colG2 = mix(tint, hsv(fract(c2 * 0.11 + 0.5 + t * 0.05), 0.7, 1.0), 0.45);
    vec3 col = tint * ringA * 1.2 + colG1 * g1 * 1.4 + colG2 * g2 * 1.4 + vec3(1.0) * hex * 0.9 + tint * tick;
    col += tint * 0.15 * (1.0 - r);

    float a = clamp(max(max(ringA, g1), max(g2, max(hex, tick))) + 0.14 * (1.0 - r), 0.0, 1.0) * env * mask * vColor.a;
    fragColor = vec4(col, a);
}
