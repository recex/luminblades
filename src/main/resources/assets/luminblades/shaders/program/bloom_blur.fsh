#version 150

// Blur gaussiano separável de 9 taps (direção em BlurDir).
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 BlurDir;
uniform float BlurRadius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
    vec2 step = BlurDir * BlurRadius / OutSize;
    vec3 sum = texture(DiffuseSampler, texCoord).rgb * weights[0];
    for (int i = 1; i < 5; i++) {
        sum += texture(DiffuseSampler, texCoord + step * float(i)).rgb * weights[i];
        sum += texture(DiffuseSampler, texCoord - step * float(i)).rgb * weights[i];
    }
    fragColor = vec4(sum, 1.0);
}
