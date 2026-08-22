#version 330

// BleachHack entity highlight, pass 1 of 2.
//
// For every pixel, find the nearest filled silhouette texel along X and remember
// how far away it was plus what colour it had. Pass 2 does the same along Y and
// combines the two into a real distance from the silhouette, which is what lets
// the outline be shaded across its width (black inline, gradient, ...).
//
// Distance is stored in alpha, scaled by DIST_SCALE so any in-range distance
// stays below 1.0 - alpha 1.0 means "nothing found within the radius".

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BleachOutline {
    float Fill;
    float Radius;
    float Style;
};

const float DIST_SCALE = 8.0;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float texel = 1.0 / InSize.x;
    int radius = int(max(Radius, 1.0));

    float bestDistance = DIST_SCALE;
    vec3 bestColor = vec3(0.0);

    for (int i = -radius; i <= radius; i++) {
        vec4 s = texture(InSampler, texCoord + vec2(float(i) * texel, 0.0));

        if (s.a > 0.0) {
            float distance = abs(float(i));

            if (distance < bestDistance) {
                bestDistance = distance;
                bestColor = s.rgb;
            }
        }
    }

    if (bestDistance > float(radius)) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        fragColor = vec4(bestColor, bestDistance / DIST_SCALE);
    }
}
