#version 150

// Aura luminosa procedural: núcleo quente + halo com redemoinho animado.
uniform float Time;

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

void main() {
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);
    if (r >= 1.0) discard;
    float ang = atan(p.y, p.x);
    float swirl = noise(vec2(ang * 2.0 + Time * 1.6 + r * 3.0, r * 4.0 - Time * 0.9));
    float core = exp(-r * r * 6.0);
    float halo = smoothstep(1.0, 0.15, r) * (0.30 + 0.70 * swirl);
    float a = clamp((core * 1.2 + halo) * vColor.a, 0.0, 1.0);
    vec3 col = mix(vColor.rgb, vec3(1.0), core * 0.65);
    fragColor = vec4(col, a);
}
