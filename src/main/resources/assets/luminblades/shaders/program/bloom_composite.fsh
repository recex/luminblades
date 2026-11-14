#version 150

// Soma aditiva do halo borrado sobre a imagem original.
uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 base = texture(DiffuseSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    fragColor = vec4(base + bloom * Intensity, 1.0);
}
