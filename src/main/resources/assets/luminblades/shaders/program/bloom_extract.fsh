#version 150

// Extrai apenas as áreas brilhantes (com "joelho" suave) para o Bloom.
uniform sampler2D DiffuseSampler;
uniform float Threshold;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = max(c.r, max(c.g, c.b));
    float k = smoothstep(Threshold, Threshold + 0.25, l);
    fragColor = vec4(c * k, 1.0);
}
