#version 330

// BleachHack entity highlight, pass 1: widen the silhouette horizontally.
// Separable, so cost is 2*(2R+1) taps instead of (2R+1)^2.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BleachOutline {
    float Fill;
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float texel = 1.0 / InSize.x;
    int radius = int(max(Radius, 1.0));

    vec4 best = texture(InSampler, texCoord);

    for (int i = -8; i <= 8; i++) {
        if (i < -radius || i > radius) {
            continue;
        }

        vec4 s = texture(InSampler, texCoord + vec2(float(i) * texel, 0.0));
        if (s.a > best.a) {
            best = s;
        }
    }

    fragColor = best;
}
