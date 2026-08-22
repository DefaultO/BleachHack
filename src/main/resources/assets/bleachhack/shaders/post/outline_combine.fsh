#version 330

// BleachHack entity highlight, pass 2: widen vertically, then combine with the
// original silhouette - interior becomes a translucent fill, the widened area
// outside it becomes the solid outline.

uniform sampler2D InSampler;
uniform sampler2D OrigSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
    vec2 OrigSize;
};

layout(std140) uniform BleachOutline {
    float Fill;
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float texel = 1.0 / InSize.y;
    int radius = int(max(Radius, 1.0));

    vec4 dilated = texture(InSampler, texCoord);

    for (int i = -8; i <= 8; i++) {
        if (i < -radius || i > radius) {
            continue;
        }

        vec4 s = texture(InSampler, texCoord + vec2(0.0, float(i) * texel));
        if (s.a > dilated.a) {
            dilated = s;
        }
    }

    vec4 original = texture(OrigSampler, texCoord);

    if (original.a > 0.0) {
        fragColor = vec4(original.rgb, original.a * Fill);
    } else if (dilated.a > 0.0) {
        fragColor = vec4(dilated.rgb, dilated.a);
    } else {
        fragColor = vec4(0.0);
    }
}
