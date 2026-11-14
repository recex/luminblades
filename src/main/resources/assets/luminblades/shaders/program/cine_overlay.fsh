#version 150

// Overlay cinematográfico em tela cheia: 1 = olho de dragão, 2 = sol.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float OverlayMode;
uniform float OverlayAlpha;
uniform float OverlayProgress;
uniform float OverlayR;
uniform float OverlayG;
uniform float OverlayB;

in vec2 texCoord;
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
    vec3 base = texture(DiffuseSampler, texCoord).rgb;
    if (OverlayMode < 0.5 || OverlayAlpha < 0.002) {
        fragColor = vec4(base, 1.0);
        return;
    }
    float aspect = OutSize.x / OutSize.y;
    vec2 p = (texCoord - 0.5) * vec2(aspect, 1.0);
    float r = length(p);
    float ang = atan(p.y, p.x);
    vec3 tint = vec3(OverlayR, OverlayG, OverlayB);
    float al = OverlayAlpha;
    vec3 col = base;

    if (OverlayMode < 1.5) {
        // ---------- OLHO DE DRAGÃO ----------
        float halfW = 0.80;
        float open = smoothstep(0.0, 0.30, OverlayProgress) * (1.0 - 0.85 * smoothstep(0.78, 1.0, OverlayProgress));
        float lid = 0.30 * pow(max(0.0, 1.0 - (p.x * p.x) / (halfW * halfW)), 0.85) * open;
        float inside = smoothstep(lid, lid - 0.01, abs(p.y)) * step(abs(p.x), halfW);

        // vinheta + raios de velocidade fora do olho
        col *= 1.0 - 0.65 * smoothstep(0.25, 1.0, r) * al;
        float rays = pow(abs(sin(ang * 38.0 + Time * 0.6 + fbm(vec2(ang * 3.0, Time * 0.2)) * 3.0)), 10.0) * smoothstep(0.3, 0.95, r);
        col += tint * rays * 0.8 * al;

        // brilho na borda das pálpebras
        float edgeD = abs(abs(p.y) - lid);
        float glow = exp(-edgeD * 28.0) * step(abs(p.x), halfW) * smoothstep(0.02, 0.2, lid);
        col += tint * glow * 1.5 * al;

        // íris
        float n = fbm(vec2(ang * 3.0, r * 14.0 - Time * 0.4));
        float veins = pow(abs(sin(ang * 30.0 + n * 4.0)), 6.0);
        vec3 iris = mix(tint * 0.18, tint * 1.25, n) + tint * veins * 0.45;
        iris *= 0.55 + 0.9 * smoothstep(0.55, 0.05, r);
        // pupila em fenda que se estreita
        float pw = mix(0.075, 0.022, smoothstep(0.1, 0.55, OverlayProgress));
        float pupil = 1.0 - smoothstep(0.85, 1.0, (p.x * p.x) / (pw * pw) + (p.y * p.y) / (lid * lid * 0.9 + 0.0001));
        iris = mix(iris, vec3(0.0), pupil);
        // reflexo
        iris += vec3(1.0) * smoothstep(0.03, 0.0, length(p - vec2(-0.12, 0.07))) * 0.7;
        col = mix(col, iris, inside * al);
    } else {
        // ---------- SOL ----------
        float sunR = 0.22 * (0.75 + 0.25 * smoothstep(0.0, 0.3, OverlayProgress));
        float disk = smoothstep(sunR, sunR - 0.012, r);
        float gran = fbm(p * 16.0 + vec2(Time * 0.3, 0.0));
        vec3 surf = mix(tint * 1.15, vec3(1.0, 0.96, 0.72), gran);
        float limb = 1.0 - 0.45 * pow(clamp(r / sunR, 0.0, 1.0), 2.0);
        float outside = max(r - sunR, 0.0);
        float rayN = fbm(vec2(ang * 4.0 + Time * 0.15, outside * 3.0));
        float lines = pow(abs(sin(ang * 26.0 + rayN * 3.0 + Time * 0.2)), 12.0) * exp(-outside * 2.6);
        float corona = exp(-outside * 5.0) * (0.6 + rayN);
        float halo = exp(-r * 2.6);
        vec3 add = tint * (halo * 1.2 + corona * 1.4) + vec3(1.0, 0.92, 0.65) * lines * 1.0;
        col += add * al;
        col = mix(col, surf * limb * 1.7, disk * al);
        col += vec3(1.0, 0.95, 0.8) * smoothstep(0.25, 0.0, OverlayProgress) * al * 0.5;
    }
    fragColor = vec4(col, 1.0);
}
