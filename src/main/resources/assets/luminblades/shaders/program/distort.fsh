#version 150

// 1) Lente gravitacional + horizonte + disco de acreção (buraco negro)
// 2) Onda de choque em anel com aberração cromática
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float ImpactX;
uniform float ImpactY;
uniform float ImpactRadius;
uniform float ImpactStrength;
uniform float LensX;
uniform float LensY;
uniform float LensRadius;
uniform float LensFade;
uniform float LensR;
uniform float LensG;
uniform float LensB;

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
    float aspect = OutSize.x / OutSize.y;
    vec2 uv = texCoord;
    bool lens = LensFade > 0.001 && LensRadius > 0.0005;
    vec2 lp = vec2(0.0);
    float lr = 100.0;

    if (lens) {
        lp = (texCoord - vec2(LensX, LensY)) * vec2(aspect, 1.0) / LensRadius;
        lr = length(lp);
        if (lr > 1.0 && lr < 9.0) {
            float srcR = max(0.0, lr - 1.7 * LensFade / lr);
            vec2 srcDir = lp / lr;
            uv = vec2(LensX, LensY) + srcDir * srcR * LensRadius / vec2(aspect, 1.0);
        }
    }

    vec3 color;
    if (ImpactStrength > 0.0005) {
        vec2 center = vec2(ImpactX, ImpactY);
        vec2 d = (uv - center) * vec2(aspect, 1.0);
        float dist = length(d);
        float x = (dist - ImpactRadius) / 0.07;
        float band = exp(-x * x);
        vec2 dir = dist > 0.0001 ? d / dist : vec2(0.0);
        vec2 offset = dir * band * ImpactStrength * vec2(1.0 / aspect, 1.0);
        vec2 ca = offset * 0.45;
        color = vec3(
            texture(DiffuseSampler, uv - offset - ca).r,
            texture(DiffuseSampler, uv - offset).g,
            texture(DiffuseSampler, uv - offset + ca).b);
    } else {
        color = texture(DiffuseSampler, uv).rgb;
    }

    if (lens && lr < 9.0) {
        float fade = LensFade;
        vec3 hot = vec3(1.0, 0.86, 0.62);
        vec3 cool = vec3(LensR, LensG, LensB);

        // disco achatado (plano inclinado)
        vec2 q = vec2(lp.x, lp.y * 5.0);
        float rd = length(q);
        float angD = atan(q.y, q.x);
        float band = smoothstep(1.3, 1.9, rd) * smoothstep(7.0, 3.5, rd);
        float sw = fbm(vec2(angD * 2.0 - Time * 1.3 + rd * 1.6, rd * 3.0));
        float dop = 0.6 + 0.4 * clamp(-lp.x / max(rd, 1.0) * 1.5 + 0.5, 0.0, 1.0);
        vec3 diskCol = mix(hot, cool, smoothstep(1.6, 5.5, rd)) * (0.5 + 1.4 * sw) * dop;
        float diskA = band * (0.35 + 0.85 * sw);
        if (lr < 1.0 && lp.y > 0.0) diskA = 0.0; // parte de trás do disco fica atrás do horizonte

        // anel de fótons + arco superior (luz do fundo do disco curvada)
        float ring = exp(-pow((lr - 1.16) / 0.09, 2.0));
        float arcMask = smoothstep(-0.3, 0.8, lp.y) * exp(-pow((lr - 1.35 - 0.25 * max(lp.y, 0.0)) / 0.30, 2.0));
        vec3 arcCol = mix(hot, cool, 0.45) * (0.6 + sw);

        float horizon = smoothstep(1.0, 0.96, lr);
        color = mix(color, vec3(0.0), horizon * fade);
        color += (diskCol * diskA * 1.4 + vec3(1.0, 0.95, 0.85) * ring * 1.5 + arcCol * arcMask * 1.2) * fade;
    }
    fragColor = vec4(color, 1.0);
}
